package com.opal.app.data

/** Opal Score + Sleep/Focus/Rest ko'rsatkichlari (0..100). */
data class OpalScores(
    val overall: Int,
    val sleep: Int,
    val focus: Int,
    val rest: Int,
    val improving: Boolean
)

/**
 * Mavjud statistikadan Opal ko'rsatkichlarini hosil qiladi.
 * (Backend'da alohida maydonlar yo'q — shuning uchun mantiqiy formula.)
 */
fun computeScores(
    profile: UserProfileDto,
    stats: StatsResponseDto,
    sessions: List<FocusSessionDto>
): OpalScores {
    val today = stats.today
    val goal = stats.goalMinutes.coerceAtLeast(1)

    // Focus — bugun tejalgan vaqt maqsadga nisbatan
    val focusFrac = ((today?.savedMinutes ?: 0).toFloat() / goal).coerceIn(0f, 1f)
    // Rest — ekran vaqti maqsadga nisbatan kam bo'lsa yaxshi
    val screen = today?.screenTimeMinutes ?: 0
    val restFrac = (1f - (screen.toFloat() / (goal * 2f))).coerceIn(0f, 1f)
    // Sleep — uyqu sessiyasi bo'lsa yuqori
    val sleepFrac = if (sessions.any { it.type == "SLEEP" }) 0.92f else 0.62f

    val focus = (focusFrac * 100f).toInt().coerceIn(0, 100)
    val rest = (restFrac * 100f).toInt().coerceIn(0, 100)
    val sleep = (sleepFrac * 100f).toInt().coerceIn(0, 100)
    val overall = ((sleep + focus + rest) / 3f).toInt().coerceIn(0, 100)

    return OpalScores(
        overall = overall,
        sleep = sleep,
        focus = focus,
        rest = rest,
        improving = stats.trendPercent <= 0
    )
}
