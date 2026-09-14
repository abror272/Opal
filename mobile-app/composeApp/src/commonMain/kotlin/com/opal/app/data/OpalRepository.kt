package com.opal.app.data

import com.opal.app.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Web backend bilan ishlaydigan repository.
 * API band bo'lmasa — offline demo ma'lumotlari bilan davom etadi (graceful fallback).
 */
class OpalRepository(private val client: HttpClient = createHttpClient()) {

    val apps = MutableStateFlow<List<BlockAppDto>>(DemoData.apps)
    val profile = MutableStateFlow(DemoData.profile)
    val stats = MutableStateFlow(DemoData.stats())
    val sessions = MutableStateFlow<List<FocusSessionDto>>(DemoData.sessions)
    val online = MutableStateFlow(false)

    /* ---------- Qurilmadagi haqiqiy ilovalar + qat'iy bloklash ---------- */

    val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val strictBlocking = MutableStateFlow(false)
    val blockingServiceOn = MutableStateFlow(false)
    val rules = MutableStateFlow(DEFAULT_RULES)
    val appsLoading = MutableStateFlow(false)
    val deviceLoaded = MutableStateFlow(false)

    /** O'rnatilgan ilovalar, bloklangan paketlar va qoidalarni yuklash. */
    suspend fun loadDeviceData() {
        if (appsLoading.value) return
        appsLoading.value = true
        rules.value = loadRules()
        blockedPackages.value = loadBlockedPackages()
        strictBlocking.value = isStrictBlocking()
        blockingServiceOn.value = isBlockingServiceEnabled()
        val loaded = withContext(Dispatchers.Default) { runCatching { loadInstalledApps() }.getOrDefault(emptyList()) }
        installedApps.value = loaded
        deviceLoaded.value = true
        appsLoading.value = false
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
    }

    fun refreshBlockingService() {
        blockingServiceOn.value = runCatching { isBlockingServiceEnabled() }.getOrDefault(false)
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

    /** Barcha ma'lumotlarni backend'dan yangilash. */
    suspend fun refreshAll() {
        try {
            coroutineScope {
                val appsDef = async { client.get("/api/apps").body<List<BlockAppDto>>() }
                val profileDef = async { client.get("/api/profile").body<UserProfileDto>() }
                val statsDef = async { client.get("/api/stats").body<StatsResponseDto>() }
                val sessionsDef = async { client.get("/api/sessions").body<List<FocusSessionDto>>() }

                val newApps = appsDef.await()
                val newProfile = profileDef.await()
                val newStats = statsDef.await()
                val newSessions = sessionsDef.await()

                apps.value = newApps
                profile.value = newProfile
                stats.value = newStats
                sessions.value = newSessions
                online.value = true
            }
        } catch (_: Throwable) {
            online.value = false
        }
    }

    /** Bloklashni optimistic o'zgartirish (switch darhol siljiy, keyin serverga yoziladi). */
    suspend fun setBlocked(app: BlockAppDto, blocked: Boolean) {
        apps.update { list -> list.map { if (it.id == app.id) it.copy(blocked = blocked) else it } }
        try {
            client.patch("/api/apps") {
                setBody(AppPatchRequest(id = app.id, blocked = blocked))
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun setProtection(enabled: Boolean) {
        profile.update { it.copy(protectionEnabled = enabled) }
        patchProfile(ProfilePatchRequest(protectionEnabled = enabled))
    }

    suspend fun setStrict(enabled: Boolean) {
        profile.update { it.copy(strictMode = enabled) }
        patchProfile(ProfilePatchRequest(strictMode = enabled))
    }

    suspend fun setGoalMinutes(goal: Int) {
        profile.update { it.copy(goalMinutes = goal) }
        patchProfile(ProfilePatchRequest(goalMinutes = goal))
    }

    suspend fun setPlan(plan: String) {
        profile.update { it.copy(plan = plan) }
        patchProfile(ProfilePatchRequest(plan = plan))
    }

    private suspend fun patchProfile(body: ProfilePatchRequest) {
        try {
            val updated = client.patch("/api/profile") { setBody(body) }.body<UserProfileDto>()
            profile.value = updated
        } catch (_: Throwable) {
        }
    }

    /** Yangi sessiya yaratish (backend'ga yozamiz, ID olamiz). */
    suspend fun startSession(preset: SessionPreset): FocusSessionDto? = try {
        client.post("/api/sessions") {
            setBody(
                SessionStartRequest(
                    type = preset.type,
                    durationMinutes = preset.minutes,
                    emoji = preset.emoji
                )
            )
        }.body<FocusSessionDto>()
    } catch (_: Throwable) {
        null
    }

    /**
     * Sessiyani tugatish. early=true — erta chiqish (streak -1, saved = o'tgan vaqt-2).
     * Server profil + statistikani yangilaydi; biz local holatni ham mirror qilamiz.
     */
    suspend fun completeSession(id: String, early: Boolean, focusScore: Int = 75): FocusSessionDto? = try {
        val session = client.patch("/api/sessions") {
            setBody(SessionPatchRequest(id = id, early = early, focusScore = focusScore))
        }.body<FocusSessionDto>()
        mirrorSessionResult(early, session.savedMinutes)
        session
    } catch (_: Throwable) {
        // Offline: local hisobda mirror qilamiz
        mirrorSessionResult(early, localSavedEstimate(early))
        null
    }

    /** Backend'ga yozib bo'lmagan sessiya natijasini local holatga qo'llash. */
    fun mirrorSessionResult(early: Boolean, savedMinutes: Int) {
        profile.update {
            it.copy(
                totalSessions = it.totalSessions + 1,
                totalSavedMinutes = it.totalSavedMinutes + savedMinutes,
                streakDays = if (early) maxOf(it.streakDays - 1, 0) else it.streakDays + 1
            )
        }
        stats.update { st ->
            val today = st.today ?: st.days.lastOrNull()?.copy(
                date = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            )
            st.copy(
                today = today?.copy(savedMinutes = today.savedMinutes + savedMinutes),
                weekSavedMinutes = st.weekSavedMinutes + savedMinutes
            )
        }
    }

    private fun localSavedEstimate(early: Boolean): Int = if (early) 0 else 25

    /** Qisqa sessiya tarixi (5 ta). */
    fun recentSessions(): List<FocusSessionDto> = sessions.value.take(5)

    /** Foydalanuvchi bahosini sessiyaga yozish (Focus Score). */
    fun rescoreSession(label: String, score: Int) {
        sessions.update { list ->
            val idx = list.indexOfFirst { it.label == label }.takeIf { it >= 0 } ?: 0
            list.mapIndexed { i, s -> if (i == idx) s.copy(focusScore = score) else s }
        }
    }
}
