package com.opal.app.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.math.max

/** Opal Score + Sleep/Focus/Rest + kunlik o'zgarish (web `computeScores` bilan bir xil). */
data class OpalScores(
    val score: Int,
    val focus: Int,
    val rest: Int,
    val sleep: Int,
    val delta: Int
)

fun computeScores(
    profile: UserProfileDto,
    stats: StatsResponseDto,
    sessions: List<FocusSessionDto>
): OpalScores {
    val today = stats.today
    val screen = today?.screenTimeMinutes ?: 180
    val saved = today?.savedMinutes ?: 0
    val goal = stats.goalMinutes.takeIf { it > 0 } ?: 240

    val todayIso = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
    val todays = sessions.filter { (it.endedAt ?: it.startedAt).take(10) == todayIso }
    val completed = todays.count { it.completed }

    val over = max(screen - goal, 0)
    val score = (88f - over * 0.1f + saved * 0.18f).coerceIn(42f, 99f).toInt()
    val focus = (52f + completed * 11f + saved * 0.09f).coerceIn(38f, 98f).toInt()
    val rest = (95f - screen * 0.1f).coerceIn(44f, 97f).toInt()
    val sleep = lastSleepMinutes(sessions)?.let { sleepScoreFromMinutes(it) }
        ?: (80f - screen * 0.04f).coerceIn(55f, 82f).toInt()

    val trend = stats.trendPercent
    val delta = when {
        trend == 0 -> 0
        trend > 0 -> -minOf(trend / 8, 6)
        else -> minOf(-trend / 8, 6)
    }

    return OpalScores(score = score, focus = focus, rest = rest, sleep = sleep, delta = delta)
}

/** Oxirgi 32 soat ichida tugagan SLEEP sessiyasi (daqiqa). */
private fun lastSleepMinutes(sessions: List<FocusSessionDto>): Int? {
    val now = Clock.System.now()
    val cands = sessions
        .filter { it.type == "SLEEP" && it.endedAt != null }
        .sortedByDescending { it.endedAt }
    for (s in cands) {
        val end = runCatching { Instant.parse(s.endedAt!!) }.getOrNull() ?: continue
        if ((now - end).inWholeHours <= 32) {
            val start = runCatching { Instant.parse(s.startedAt) }.getOrNull() ?: continue
            return ((end - start).inWholeMinutes).coerceIn(0, 720).toInt()
        }
    }
    return null
}

private fun sleepScoreFromMinutes(minutes: Int): Int =
    (40f + minutes * 0.108f).coerceIn(40f, 97f).toInt()
