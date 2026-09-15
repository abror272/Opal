package com.opal.app.data

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Mahalliy saqlash — haqiqiy tarix backend'siz ham saqlanadi. */
expect fun loadLocalString(key: String): String?
expect fun saveLocalString(key: String, value: String)

/** HAQIQIY fokus sessiyasi (qurilmada saqlanadi). */
@Serializable
data class StoredSession(
    val id: String,
    val type: String = "CUSTOM",
    val label: String = "Maxsus",
    val emoji: String = "⚡",
    val durationMinutes: Int = 30,
    val startedAtMs: Long = 0L,
    val endedAtMs: Long = 0L,
    val completed: Boolean = false,
    val savedMinutes: Int = 0,
    val focusScore: Int = 0,
    val blockedCount: Int = 0
)

/** HAQIQIY kunlik yig'indi (sessiyalar + ochishlar). */
@Serializable
data class StoredDay(
    val date: String,
    val focusMinutes: Int = 0,
    val savedMinutes: Int = 0,
    val sessions: Int = 0,
    val pickups: Int = 0
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private const val KEY_SESSIONS = "real_sessions"
private const val KEY_DAYS = "real_days"

/* ---------------- Sanalar ---------------- */

fun todayKey(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

fun dateKeyBack(daysAgo: Int): String =
    Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(DatePeriod(days = daysAgo)).toString()

/* ---------------- O'qish / yozish ---------------- */

fun loadStoredSessions(): List<StoredSession> {
    val raw = runCatching { loadLocalString(KEY_SESSIONS) }.getOrNull() ?: return emptyList()
    return runCatching { json.decodeFromString<List<StoredSession>>(raw) }.getOrDefault(emptyList())
}

fun saveStoredSessions(list: List<StoredSession>) {
    runCatching { saveLocalString(KEY_SESSIONS, json.encodeToString(list.take(400))) }
}

fun loadStoredDays(): Map<String, StoredDay> {
    val raw = runCatching { loadLocalString(KEY_DAYS) }.getOrNull() ?: return emptyMap()
    return runCatching {
        json.decodeFromString<List<StoredDay>>(raw).associateBy { it.date }
    }.getOrDefault(emptyMap())
}

fun saveStoredDays(map: Map<String, StoredDay>) {
    runCatching { saveLocalString(KEY_DAYS, json.encodeToString(map.values.sortedBy { it.date })) }
}

/* ---------------- Hisob-kitob ---------------- */

/** Ketma-ket fokus kunlari (bugun hali sessiya bo'lmasa — kechadan boshlanadi). */
fun computeStreak(days: Map<String, StoredDay>): Int {
    var streak = 0
    val today = todayKey()
    var back = if ((days[today]?.sessions ?: 0) > 0) 0 else 1
    while (back < 400) {
        val d = days[dateKeyBack(back)] ?: break
        if (d.sessions <= 0) break
        streak++
        back++
    }
    return streak
}

/** HAQIQIY statistikani qurish: UsageStats ekran vaqti + saqlangan kunlar. */
fun buildRealStats(
    screenByDay: Map<String, Int> = emptyMap(),
    stored: Map<String, StoredDay> = emptyMap(),
    goalMinutes: Int = 240,
    window: Int = 35
): StatsResponseDto {
    val days = (window - 1).downTo(0).map { back ->
        val date = dateKeyBack(back)
        val s = stored[date]
        DailyStatDto(
            id = date,
            date = date,
            screenTimeMinutes = screenByDay[date] ?: 0,
            savedMinutes = s?.savedMinutes ?: 0,
            pickups = s?.pickups ?: 0,
            sessions = s?.sessions ?: 0,
            goalMinutes = goalMinutes
        )
    }
    val last7 = days.takeLast(7)
    val prev7 = days.dropLast(7).takeLast(7)
    val weekSaved = last7.sumOf { it.savedMinutes }
    val weekScreen = last7.sumOf { it.screenTimeMinutes }
    val prevScreen = prev7.sumOf { it.screenTimeMinutes }
    val withData = last7.filter { it.screenTimeMinutes > 0 }
    val avg = if (withData.isEmpty()) 0 else withData.sumOf { it.screenTimeMinutes } / withData.size
    val trend = if (prevScreen > 0) ((weekScreen - prevScreen) * 100) / prevScreen else 0
    return StatsResponseDto(
        days = days,
        today = days.lastOrNull(),
        weekSavedMinutes = weekSaved,
        weekScreenMinutes = weekScreen,
        avgDailyScreenMinutes = avg,
        trendPercent = trend,
        goalMinutes = goalMinutes
    )
}

/** Saqlangan sessiyalarni UI modeliga aylantirish (ISO sanalar — tarix uchun). */
fun StoredSession.toDto(): FocusSessionDto = FocusSessionDto(
    id = id,
    type = type,
    label = label,
    emoji = emoji,
    durationMinutes = durationMinutes,
    startedAt = Instant.fromEpochMilliseconds(startedAtMs).toString(),
    endedAt = if (endedAtMs > 0L) Instant.fromEpochMilliseconds(endedAtMs).toString() else null,
    completed = completed,
    savedMinutes = savedMinutes,
    focusScore = focusScore
)
