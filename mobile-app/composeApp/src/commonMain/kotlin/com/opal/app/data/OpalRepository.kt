package com.opal.app.data

import com.opal.app.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * OPAL repository — MAHALLIY BIRINCHI (local-first).
 *
 * Hamma raqam qurilmadan olinadi:
 *  - ekran vaqti / pickup'lar  -> Android UsageStats (`realScreenTimeByDay`, `realPickupsToday`)
 *  - fokus sessiyalari         -> qurilmada saqlangan haqiqiy sessiyalar (`StoredSession`)
 *  - streak / jami statistika  -> shu saqlangan ma'lumotlardan hisoblanadi
 *
 * Demo (soxta) ma'lumot YO'Q.
 */
class OpalRepository(private val client: HttpClient = createHttpClient()) {

    /* ---------- Mahalliy haqiqiy ombor ---------- */

    private val storedDays: MutableMap<String, StoredDay> = loadStoredDays().toMutableMap()
    private val storedSessions: MutableList<StoredSession> = loadStoredSessions().toMutableList()
    private val pending: MutableMap<String, StoredSession> = mutableMapOf()

    private var screenCache: Map<String, Int> = emptyMap()
    private var screenCacheAt = 0L

    var goalMinutes: Int = 240
        private set

    /* ---------- StateFlow'lar ---------- */

    val apps = MutableStateFlow<List<BlockAppDto>>(DemoData.apps)
    val profile = MutableStateFlow(realProfile())
    val stats = MutableStateFlow(realStats())
    val sessions = MutableStateFlow(storedSessions.sortedByDescending { it.startedAtMs }.map { it.toDto() })
    val online = MutableStateFlow(false)

    /** Bugun kamida 1 ta haqiqiy sessiya bo'lganmi (gems uchun). */
    val usedToday = MutableStateFlow(false)

    /* ---------- Qurilmadagi haqiqiy ilovalar + qat'iy bloklash ---------- */

    val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val strictBlocking = MutableStateFlow(false)
    val blockingServiceOn = MutableStateFlow(false)
    val watchdogOn = MutableStateFlow(false)
    val overlayPerm = MutableStateFlow(false)
    val usagePerm = MutableStateFlow(false)
    val secureSettings = MutableStateFlow(false)
    val rules = MutableStateFlow(DEFAULT_RULES)
    val appsLoading = MutableStateFlow(false)
    val deviceLoaded = MutableStateFlow(false)

    /* ==================== HAQIQIY STATISTIKA ==================== */

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    /** UsageStats'dan ekran vaqti (60 soniya kesh). */
    private fun screenTimes(force: Boolean = false): Map<String, Int> {
        val now = nowMs()
        if (!force && screenCache.isNotEmpty() && now - screenCacheAt < 60_000L) return screenCache
        screenCache = runCatching { realScreenTimeByDay(35) }.getOrDefault(emptyMap())
        screenCacheAt = now
        return screenCache
    }

    private fun realStats(): StatsResponseDto = buildRealStats(
        screenByDay = screenTimes(),
        stored = storedDays,
        goalMinutes = goalMinutes
    )

    private fun realProfile(): UserProfileDto {
        val base = DemoData.profile
        return base.copy(
            streakDays = computeStreak(storedDays),
            totalSavedMinutes = storedSessions.sumOf { it.savedMinutes },
            totalSessions = storedSessions.size,
            strictMode = runCatching { isStrictBlocking() }.getOrDefault(false),
            goalMinutes = goalMinutes
        )
    }

    /** Haqiqiy statistikani qayta hisoblash (UsageStats + saqlangan kunlar). */
    fun refreshRealStats(force: Boolean = false) {
        val today = todayKey()
        val realPickups = runCatching { realPickupsToday() }.getOrDefault(0)
        if (realPickups > 0 && (storedDays[today]?.pickups ?: 0) != realPickups) {
            storedDays[today] = (storedDays[today] ?: StoredDay(today)).copy(pickups = realPickups)
            saveStoredDays(storedDays)
        }
        screenTimes(force)
        stats.value = realStats()
        profile.value = realProfile()
        sessions.value = storedSessions.sortedByDescending { it.startedAtMs }.map { it.toDto() }
        usedToday.value = (storedDays[today]?.sessions ?: 0) > 0
    }

    /** Bugun haqiqiy ma'lumot bormi (UI bo'sh holat uchun). */
    fun hasAnyRealData(): Boolean = storedSessions.isNotEmpty() || storedDays.values.any { it.savedMinutes > 0 }

    /* ==================== SESSIYALAR (haqiqiy) ==================== */

    /** Yangi sessiya boshlash — mahalliy ID qaytaradi. */
    fun startSession(preset: SessionPreset): FocusSessionDto {
        val now = nowMs()
        val id = "s-$now"
        val s = StoredSession(
            id = id,
            type = preset.type,
            label = preset.label,
            emoji = preset.emoji,
            durationMinutes = preset.minutes,
            startedAtMs = now,
            blockedCount = blockedPackages.value.size
        )
        pending[id] = s
        return s.toDto()
    }

    /**
     * Sessiyani yakunlash. [savedMinutes] — SessionController hisoblagan haqiqiy qiymat.
     * Natija darhol qurilmaga yoziladi va statistika qayta hisoblanadi.
     */
    fun completeSession(id: String, early: Boolean, savedMinutes: Int, focusScore: Int = 0) {
        val p = pending.remove(id) ?: return
        val endMs = nowMs()
        val elapsedMin = ((endMs - p.startedAtMs) / 60_000L).toInt().coerceAtLeast(0)
        val done = p.copy(
            endedAtMs = endMs,
            completed = !early,
            savedMinutes = savedMinutes,
            focusScore = focusScore,
            blockedCount = blockedPackages.value.size
        )
        storedSessions.add(done)

        val date = Instant.fromEpochMilliseconds(p.startedAtMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val day = storedDays[date] ?: StoredDay(date)
        storedDays[date] = day.copy(
            focusMinutes = day.focusMinutes + elapsedMin,
            savedMinutes = day.savedMinutes + savedMinutes,
            sessions = day.sessions + 1
        )

        saveStoredSessions(storedSessions)
        saveStoredDays(storedDays)
        refreshRealStats(force = true)
    }

    /** Foydalanuvchi bahosini saqlash (Focus Score). */
    fun rescoreSession(id: String, score: Int) {
        val idx = storedSessions.indexOfFirst { it.id == id }
        if (idx < 0) return
        storedSessions[idx] = storedSessions[idx].copy(focusScore = score)
        saveStoredSessions(storedSessions)
        sessions.value = storedSessions.sortedByDescending { it.startedAtMs }.map { it.toDto() }
    }

    /** Sessiyani ID bo'yicha topish. */
    fun sessionById(id: String): StoredSession? = storedSessions.firstOrNull { it.id == id }

    fun recentSessions(): List<FocusSessionDto> =
        storedSessions.sortedByDescending { it.startedAtMs }.take(5).map { it.toDto() }

    /* ==================== QURILMA / BLOKLASH ==================== */

    suspend fun loadDeviceData() {
        if (appsLoading.value) return
        appsLoading.value = true
        rules.value = loadRules()
        blockedPackages.value = loadBlockedPackages()
        strictBlocking.value = isStrictBlocking()
        refreshBlockingService()
        val loaded = withContext(Dispatchers.Default) {
            runCatching { loadInstalledApps() }.getOrDefault(emptyList())
        }
        installedApps.value = loaded
        deviceLoaded.value = true
        appsLoading.value = false
        refreshRealStats()
    }

    fun toggleBlockedPackage(pkg: String) {
        val next = blockedPackages.value.toMutableSet()
        if (!next.add(pkg)) next.remove(pkg)
        blockedPackages.value = next
        runCatching { saveBlockedPackages(next) }
    }

    fun setPackageBlocked(pkg: String, blocked: Boolean) {
        val next = blockedPackages.value.toMutableSet()
        if (blocked) next.add(pkg) else next.remove(pkg)
        blockedPackages.value = next
        runCatching { saveBlockedPackages(next) }
    }

    fun setStrictBlocking(on: Boolean) {
        strictBlocking.value = on
        runCatching { com.opal.app.data.setStrictBlocking(on) }
        profile.value = profile.value.copy(strictMode = on)
        if (on) runCatching { startWatchdog() }
    }

    /** "Bloklash" yoqish/o'chirish (Timer ekranidagi toggle). */
    fun setProtection(enabled: Boolean) {
        profile.value = profile.value.copy(protectionEnabled = enabled)
        if (enabled) ensureBlockingEngine()
    }

    /** "Qat'iy rejim" toggle. */
    fun setStrict(enabled: Boolean) {
        setStrictBlocking(enabled)
    }

    fun refreshBlockingService() {
        blockingServiceOn.value = runCatching { isBlockingServiceEnabled() }.getOrDefault(false)
        watchdogOn.value = runCatching { watchdogRunning() }.getOrDefault(false)
        overlayPerm.value = runCatching { canDrawOverlays() }.getOrDefault(false)
        usagePerm.value = runCatching { hasUsageAccess() }.getOrDefault(false)
        secureSettings.value = runCatching { canWriteSecureSettings() }.getOrDefault(false)
    }

    /** Bloklash tizimini ishga tayyorlash (ruxsatlar bo'lsa watchdog'ni yoqadi). */
    fun ensureBlockingEngine() {
        refreshBlockingService()
        if (overlayPerm.value && usagePerm.value) runCatching { startWatchdog() }
    }

    fun updateRule(rule: RuleSpec) {
        val next = rules.value.map { if (it.id == rule.id) rule else it }
        rules.value = next
        persistRules(next)
    }

    fun resetRules() {
        rules.value = DEFAULT_RULES
        persistRules(DEFAULT_RULES)
    }

    /* ==================== Backend (ixtiyoriy) ==================== */

    suspend fun refreshAll() {
        refreshRealStats()
        loadDeviceData()
        try {
            val remote = client.get("/api/profile").body<UserProfileDto>()
            // Backend mavjud bo'lsa faqat nom/handle'ni olamiz — raqamlar baribir mahalliy.
            profile.value = profile.value.copy(name = remote.name, handle = remote.handle)
            online.value = true
        } catch (_: Throwable) {
            online.value = false
        }
    }

    fun setGoalMinutes(goal: Int) {
        goalMinutes = goal.coerceIn(30, 900)
        stats.value = realStats()
        profile.value = profile.value.copy(goalMinutes = goalMinutes)
    }
}
