package com.opal.app.data

import kotlinx.serialization.Serializable

/** Web backend'dagi BlockApp modelining aynan nusxasi (id — string, Prisma cuid). */
@Serializable
data class BlockAppDto(
    val id: String,
    val name: String,
    val emoji: String = "📱",
    val gradient: String = "",
    val category: String = "Boshqa",
    val blocked: Boolean = false,
    val dailyLimitMinutes: Int = 60,
    val todayMinutes: Int = 0,
    val order: Int = 0
)

@Serializable
data class FocusSessionDto(
    val id: String,
    val type: String = "CUSTOM",
    val label: String = "Maxsus",
    val emoji: String = "⚡",
    val durationMinutes: Int = 30,
    val startedAt: String = "",
    val endedAt: String? = null,
    val completed: Boolean = false,
    val savedMinutes: Int = 0,
    val focusScore: Int = 0,
    val blockedApps: String? = null
)

@Serializable
data class DailyStatDto(
    val id: String = "",
    val date: String,
    val screenTimeMinutes: Int = 0,
    val savedMinutes: Int = 0,
    val pickups: Int = 0,
    val goalMinutes: Int = 240
)

/** GET /api/stats javobi. */
@Serializable
data class StatsResponseDto(
    val days: List<DailyStatDto> = emptyList(),
    val today: DailyStatDto? = null,
    val weekSavedMinutes: Int = 0,
    val weekScreenMinutes: Int = 0,
    val avgDailyScreenMinutes: Int = 0,
    val trendPercent: Int = 0,
    val goalMinutes: Int = 240
)

@Serializable
data class UserProfileDto(
    val id: String = "",
    val name: String = "Foydalanuvchi",
    val handle: String = "@user",
    val streakDays: Int = 0,
    val totalSavedMinutes: Int = 0,
    val totalSessions: Int = 0,
    val protectionEnabled: Boolean = true,
    val strictMode: Boolean = false,
    val goalMinutes: Int = 240,
    val plan: String = "FREE"
)

/* ---------- Request tanalari ---------- */

@Serializable
data class AppPatchRequest(
    val id: String,
    val blocked: Boolean? = null,
    val dailyLimitMinutes: Int? = null
)

@Serializable
data class SessionStartRequest(
    val type: String,
    val durationMinutes: Int,
    val emoji: String
)

@Serializable
data class SessionPatchRequest(
    val id: String,
    val early: Boolean,
    val focusScore: Int? = null
)

@Serializable
data class ProfilePatchRequest(
    val protectionEnabled: Boolean? = null,
    val strictMode: Boolean? = null,
    val goalMinutes: Int? = null,
    val plan: String? = null,
    val name: String? = null
)

/* ---------- UI darajasidagi preset ---------- */

data class SessionPreset(
    val type: String,
    val label: String,
    val emoji: String,
    val minutes: Int,
    val desc: String
)

val SESSION_PRESETS = listOf(
    SessionPreset("DEEP_FOCUS", "Deep Focus", "🧠", 45, "Eng chalg'ituvchi ilovalar bloklanadi"),
    SessionPreset("WORK", "Ish rejimi", "💼", 90, "Faqat ish ilovalari ochiq qoladi"),
    SessionPreset("STUDY", "O'qish", "📚", 60, "Kutubxona rejimi, tinchlik"),
    SessionPreset("SLEEP", "Uyqu rejimi", "🌙", 480, "Tungi bildirishnomalar o'chadi"),
    SessionPreset("CUSTOM", "Maxsus", "⚡", 30, "Davomiylikni o'zingiz tanlang")
)

val SESSION_DURATIONS = listOf(15, 25, 30, 45, 60, 90, 120)

/* ---------- Formatlash helperlari ---------- */

fun formatMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    if (h <= 0) return "${m}d"
    if (m == 0) return "${h}s"
    return "${h}s ${m}d"
}

fun formatClock(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
