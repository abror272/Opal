package com.opal.app.data

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun weekdayLabelUz(iso: String): String {
    return try {
        val names = mapOf(
            kotlinx.datetime.DayOfWeek.MONDAY to "Dush",
            kotlinx.datetime.DayOfWeek.TUESDAY to "Sesh",
            kotlinx.datetime.DayOfWeek.WEDNESDAY to "Chor",
            kotlinx.datetime.DayOfWeek.THURSDAY to "Pay",
            kotlinx.datetime.DayOfWeek.FRIDAY to "Jum",
            kotlinx.datetime.DayOfWeek.SATURDAY to "Shan",
            kotlinx.datetime.DayOfWeek.SUNDAY to "Yak"
        )
        names[LocalDate.parse(iso).dayOfWeek] ?: "—"
    } catch (_: Throwable) {
        "—"
    }
}

/** Offline demo ma'lumotlari — API ishlamasa ilova baribir chiroyli ko'rinadi. */
object DemoData {

    val apps = listOf(
        BlockAppDto("demo-1", "Instagram", "📷", "Ijtimoiy", "Ijtimoiy tarmoq", true, 60, 312, 0),
        BlockAppDto("demo-2", "TikTok", "🎬", "Video", "Ijtimoiy tarmoq", true, 45, 187, 1),
        BlockAppDto("demo-3", "YouTube", "▶️", "Video", "Video", false, 90, 145, 2),
        BlockAppDto("demo-4", "Telegram", "✈️", "Messaging", "Messenger", false, 120, 96, 3),
        BlockAppDto("demo-5", "X", "🐦", "Yangiliklar", "Ijtimoiy tarmoq", true, 30, 74, 4),
        BlockAppDto("demo-6", "Safari", "🌐", "Brauzer", "Brauzer", false, 240, 52, 5),
        BlockAppDto("demo-7", "Netflix", "🎥", "Video", "Video", false, 60, 43, 6),
        BlockAppDto("demo-8", "Steam", "🎮", "O'yinlar", "O'yinlar", true, 60, 118, 7),
        BlockAppDto("demo-9", "Spotify", "🎧", "Musiqa", "Musiqa", false, 300, 38, 8),
        BlockAppDto("demo-10", "Reddit", "👽", "Ijtimoiy", "Forum", false, 45, 29, 9)
    )

    val profile = UserProfileDto(
        id = "demo-profile",
        name = "Aziz",
        handle = "@aziz.dev",
        streakDays = 12,
        totalSavedMinutes = 1840,
        totalSessions = 47,
        protectionEnabled = true,
        strictMode = false,
        goalMinutes = 240,
        plan = "FREE"
    )

    /** 35 kunlik izchillik xaritasi uchun to'liq oyna. */
    private val SAVE_PATTERN = listOf(
        95, 120, 0, 140, 88, 150, 60, 0, 110, 175, 45, 130, 0, 90, 160,
        70, 0, 105, 132, 48, 165, 82, 0, 118, 96, 142, 55, 0, 128, 172,
        64, 0, 100, 138, 76
    )

    fun stats(): StatsResponseDto {
        val t = today()
        val days = (0..34).map { i ->
            val d = t.minus(DatePeriod(days = 34 - i))
            DailyStatDto(
                id = "demo-stat-$i",
                date = d.toString(),
                screenTimeMinutes = 180 + ((i * 53) % 190),
                savedMinutes = SAVE_PATTERN[i % SAVE_PATTERN.size],
                pickups = 30 + ((i * 17) % 60),
                goalMinutes = 240
            )
        }
        val last7 = days.takeLast(7)
        return StatsResponseDto(
            days = days,
            today = days.last(),
            weekSavedMinutes = last7.sumOf { it.savedMinutes },
            weekScreenMinutes = last7.sumOf { it.screenTimeMinutes },
            avgDailyScreenMinutes = last7.sumOf { it.screenTimeMinutes } / 7,
            trendPercent = -8,
            goalMinutes = 240
        )
    }

    val sessions = listOf(
        FocusSessionDto("demo-s1", "DEEP_FOCUS", "Deep Focus", "🧠", 45, "", null, true, 45, 88),
        FocusSessionDto("demo-s2", "STUDY", "O'qish", "📚", 60, "", null, true, 60, 82),
        FocusSessionDto("demo-s3", "CUSTOM", "Maxsus", "⚡", 30, "", null, false, 18, 40),
        FocusSessionDto("demo-s4", "WORK", "Ish rejimi", "💼", 90, "", null, true, 90, 91),
        FocusSessionDto("demo-s5", "SLEEP", "Uyqu rejimi", "🌙", 480, "", null, true, 480, 95)
    )
}
