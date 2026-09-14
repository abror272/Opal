package com.opal.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bloklash qoidasi.
 * type = "schedule" — vaqt oralig'ida bloklaydi (uyqu, ish, tushlik).
 * type = "limit"    — kunlik ochish sonini cheklaydi.
 */
@Serializable
data class RuleSpec(
    val id: String,
    val title: String,
    val icon: String = "🔒",
    val type: String = "schedule",
    val enabled: Boolean = true,
    val start: String = "22:00",
    val end: String = "08:00",
    val opens: Int = 10,
    val subtitle: String = "",
    val photo: String = ""
)

val DEFAULT_RULES = listOf(
    RuleSpec("r-open", "Kunlik 10 ochish", "🔓", "limit", true, opens = 10, subtitle = "TikTok, Instagram +3"),
    RuleSpec("r-sleep", "Uyqu vaqti", "🌙", "schedule", true, "22:00", "08:00", subtitle = "Hammasini bloklash", photo = "sleep"),
    RuleSpec("r-work", "Chuqur ish", "💻", "schedule", true, "09:00", "17:00", subtitle = "Faqat ish ilovalari", photo = "deepwork"),
    RuleSpec("r-lunch", "Tushlik tanaffusi", "🍽️", "schedule", true, "12:00", "13:00", subtitle = "Ijtimoiy tarmoqlar ochiladi", photo = "family")
)

expect fun loadRulesRaw(): String?
expect fun saveRulesRaw(value: String)

private val rulesJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun loadRules(): List<RuleSpec> {
    val raw = loadRulesRaw() ?: return DEFAULT_RULES
    return runCatching { rulesJson.decodeFromString<List<RuleSpec>>(raw) }.getOrElse { DEFAULT_RULES }
}

fun persistRules(rules: List<RuleSpec>) {
    runCatching { saveRulesRaw(rulesJson.encodeToString(rules)) }
}

/** "22:00" → 1320 daqiqa */
fun parseTimeToMinutes(t: String): Int {
    val parts = t.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return (h.coerceIn(0, 23)) * 60 + m.coerceIn(0, 59)
}

fun minutesToTime(min: Int): String {
    val v = ((min % 1440) + 1440) % 1440
    return "${(v / 60).toString().padStart(2, '0')}:${(v % 60).toString().padStart(2, '0')}"
}
