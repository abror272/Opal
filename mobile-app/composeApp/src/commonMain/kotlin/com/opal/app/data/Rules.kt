package com.opal.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.toLocalDateTime

/**
 * Bloklash qoidasi.
 *
 * [type]
 *  - "schedule" — vaqt oralig'ida ishlaydi (uyqu, ish, tushlik)
 *  - "limit"    — kunlik ochish sonini cheklaydi
 *
 * [mode] (faqat "schedule" uchun)
 *  - "blockAll" — hamma ilovani bloklaydi (tizim va samarali ilovalardan tashqari)
 *  - "block"    — faqat [apps] ro'yxatidagi ilovalarni bloklaydi
 *  - "allow"    — [apps] ro'yxatidagi ilovalarni OCHADI (boshqa qoidalarni bekor qiladi)
 *
 * [apps] — ta'sirlanadigan paket nomlari. Bo'sh bo'lsa, "limit"/"block" uchun
 * qurilmadagi mashhur chalg'ituvchi ilovalar avtomatik olinadi (SOCIAL_PACKAGES).
 *
 * [opens] — KUNLIK QULFNI OCHISH SONI. Blok paytida foydalanuvchi shu qadar
 * marta "Limitdan foydalanish" tugmasini bosib ilovani ocha oladi.
 * [graceMinutes] — limitdan foydalanganda ilova shu qadar daqiqa ochiq turadi.
 */
@Serializable
data class RuleSpec(
    val id: String,
    val title: String,
    val icon: String = "🔒",
    val type: String = "schedule",
    val mode: String = "block",
    val enabled: Boolean = true,
    val start: String = "22:00",
    val end: String = "08:00",
    val opens: Int = 10,
    val graceMinutes: Int = 5,
    val apps: List<String> = emptyList(),
    val subtitle: String = "",
    val photo: String = ""
)

/** Mashhur chalg'ituvchi ilovalar — qoida uchun standart nishonlar. */
val SOCIAL_PACKAGES = setOf(
    "com.zhiliaoapp.musically",            // TikTok
    "com.ss.android.ugc.trill",            // TikTok (global)
    "com.ss.android.ugc.aweme",            // Douyin
    "com.instagram.android",               // Instagram
    "com.twitter.android",                 // X / Twitter
    "com.x.android",                       // X
    "com.reddit.frontpage",                // Reddit
    "com.snapchat.android",                // Snapchat
    "com.facebook.katana",                 // Facebook
    "com.facebook.lite",
    "com.google.android.youtube",          // YouTube
    "com.netflix.mediaclient",             // Netflix
    "com.pinterest",                       // Pinterest
    "com.linkedin.android",                // LinkedIn
    "com.discord",                         // Discord
    "com.zhihu.android"
)

/** Hech qachon bloklanmaydigan HAYOTIY ilovalar (budilnik, telefon, launcher...). */
val ESSENTIAL_PACKAGES = setOf(
    // Budilnik / soat
    "com.android.deskclock", "com.google.android.deskclock", "com.miui.clock",
    "com.miui.deskclock", "com.coloros.alarmclock", "com.oneplus.deskclock",
    "com.sec.android.app.clockpackage", "com.samsung.android.app.clockpack",
    "com.transsion.clock", "com.oplus.alarmclock",
    // Telefon / SMS / favqulodda
    "com.android.dialer", "com.google.android.dialer", "com.android.server.telecom",
    "com.android.mms", "com.google.android.apps.messaging", "com.samsung.android.messaging",
    "com.android.emergency", "com.android.incallui",
    // Launcher / tizim
    "com.miui.home", "com.android.launcher3", "com.google.android.apps.nexuslauncher",
    "com.android.systemui", "com.miui.securitycenter", "com.android.settings",
    "com.android.permissioncontroller", "com.google.android.permissioncontroller",
    "com.android.camera", "com.miui.calculator", "com.android.calculator2",
    "com.opal.app"
)

/** Ish paytida ruxsat etiladigan samarali ilovalar. */
val PRODUCTIVITY_PACKAGES = setOf(
    "com.google.android.gm",               // Gmail
    "com.google.android.apps.docs",
    "com.google.android.calendar",
    "com.google.android.apps.meetings",
    "com.microsoft.office.outlook",
    "com.microsoft.teams",
    "us.zoom.videomeetings",
    "com.slack",
    "com.notion.id",
    "com.figma.mirror",
    "com.google.android.keep",
    "com.android.calendar",
    "com.android.deskclock",
    "com.android.contacts",
    "com.android.dialer",
    "com.android.mms",
    "com.google.android.dialer",
    "com.android.settings",
    "com.android.camera",
    "com.android.gallery3d",
    "com.miui.gallery",
    "com.google.android.apps.photos"
)

val DEFAULT_RULES = listOf(
    RuleSpec(
        "r-open", "Kunlik 10 ochish", "🔓", "limit", "block", true,
        opens = 10, subtitle = "Chalg'ituvchi ilovalar uchun"
    ),
    RuleSpec(
        "r-sleep", "Uyqu vaqti", "🌙", "schedule", "block", true,
        start = "22:00", end = "08:00", opens = 3, graceMinutes = 5,
        subtitle = "Chalg'ituvchi ilovalar bloklanadi", photo = "sleep"
    ),
    RuleSpec(
        "r-work", "Chuqur ish", "💻", "schedule", "block", true,
        start = "09:00", end = "17:00", opens = 5, graceMinutes = 5,
        subtitle = "Chalg'ituvchilarni bloklash", photo = "deepwork"
    ),
    RuleSpec(
        "r-lunch", "Tushlik tanaffusi", "🍽️", "schedule", "allow", true,
        start = "12:00", end = "13:00", subtitle = "Ijtimoiy tarmoqlar ochiladi", photo = "family"
    )
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

/** Hozirgi kun vaqti daqiqalarda (0..1439). */
fun currentMinutesOfDay(): Int {
    val t = kotlinx.datetime.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return t.hour * 60 + t.minute
}

/** Vaqt oynasi ichidami? Yarim tundan oshadigan oynalar ham to'g'ri ishlaydi. */
fun isWithinWindow(now: Int, start: String, end: String): Boolean {
    val s = parseTimeToMinutes(start)
    val e = parseTimeToMinutes(end)
    if (s == e) return false
    return if (s < e) now in s until e else (now >= s || now < e)
}

/** Qoida hozir faolmi (schedule uchun oyna, limit uchun kunlik hisob). */
fun isRuleActiveNow(rule: RuleSpec, nowMinutes: Int): Boolean =
    rule.enabled && rule.type == "schedule" && isWithinWindow(nowMinutes, rule.start, rule.end)

/** Qoida ta'sirlaydigan paketlar (bo'sh bo'lsa — standart chalg'ituvchilar). */
fun ruleTargets(rule: RuleSpec, installed: Set<String>): Set<String> {
    if (rule.apps.isNotEmpty()) return rule.apps.toSet()
    if (rule.mode == "blockAll") return emptySet()
    // Standart: qurilmada mavjud bo'lgan ijtimoiy/chalg'ituvchi ilovalar
    val detected = installed.filter { it in SOCIAL_PACKAGES }
    return if (detected.isNotEmpty()) detected.toSet() else SOCIAL_PACKAGES
}

/** Bloklash sababi. */
enum class BlockReason(val code: String) {
    MANUAL("manual"), SCHEDULE("schedule"), LIMIT("limit")
}

data class BlockHit(
    val reason: BlockReason,
    val ruleId: String?,
    val title: String,
    val icon: String,
    val detail: String,
    /** Kunlik qulfni ochish (unlock) limiti. 0 = limit yo'q. */
    val unlockLimit: Int = 0,
    /** Bugun allaqachon ishlatilgan ochishlar. */
    val opensToday: Int = 0,
    /** Limitdan foydalanilganda ilova necha daqiqa ochiq turadi. */
    val graceMinutes: Int = 5
) {
    val remainingUnlocks: Int get() = (unlockLimit - opensToday).coerceAtLeast(0)
    val canUnlock: Boolean get() = unlockLimit > 0 && remainingUnlocks > 0
}

/** Shu paket uchun kunlik unlock limitini beruvchi qoida. */
private fun allowanceRule(
    pkg: String,
    rules: List<RuleSpec>,
    installed: Set<String>
): RuleSpec? {
    rules.firstOrNull { it.enabled && it.type == "limit" && pkg in ruleTargets(it, installed) }
        ?.let { return it }
    return rules.firstOrNull { it.enabled && pkg in ruleTargets(it, installed) }
}

/**
 * Ilova hozir bloklanishi kerakmi? Birinchi mos kelgan sabab qaytariladi.
 *
 * @param installed qurilmadagi barcha paketlar (standart nishonlarni aniqlash uchun)
 * @param opensToday shu paket bugun necha marta ochilgan
 */
fun evaluateBlock(
    pkg: String,
    manuallyBlocked: Set<String>,
    rules: List<RuleSpec>,
    installed: Set<String>,
    nowMinutes: Int,
    opensToday: Int
): BlockHit? {
    val active = rules.filter { isRuleActiveNow(it, nowMinutes) }
    val allowance = allowanceRule(pkg, rules, installed)
    val allowLimit = allowance?.opens ?: 0
    val allowGrace = allowance?.graceMinutes ?: 5

    // 0) HAYOTIY ilovalar (budilnik, telefon, launcher) — hech qachon bloklanmaydi
    if (pkg in ESSENTIAL_PACKAGES) return null

    // 1) "allow" qoidalari ustun — ilova ochiladi
    for (r in active) {
        if (r.mode == "allow" && pkg in ruleTargets(r, installed)) return null
    }

    // 2) "blockAll" — hamma narsa (hayotiy va samarali ilovalardan tashqari)
    for (r in active) {
        if (r.mode == "blockAll" && pkg !in PRODUCTIVITY_PACKAGES && pkg !in ESSENTIAL_PACKAGES) {
            return BlockHit(
                BlockReason.SCHEDULE, r.id, r.title, r.icon,
                "${r.start}–${r.end} oralig'ida hammasi bloklangan",
                unlockLimit = r.opens, opensToday = opensToday, graceMinutes = r.graceMinutes
            )
        }
    }

    // 3) Qo'lda bloklash
    if (pkg in manuallyBlocked) {
        return BlockHit(
            BlockReason.MANUAL, null, "Bloklangan ilova", "🔒", "Siz bu ilovani bloklagansiz",
            unlockLimit = allowLimit, opensToday = opensToday, graceMinutes = allowGrace
        )
    }

    // 4) "block" qoidalari
    for (r in active) {
        if (r.mode == "block" && pkg in ruleTargets(r, installed)) {
            return BlockHit(
                BlockReason.SCHEDULE, r.id, r.title, r.icon,
                "${r.start}–${r.end} oralig'ida bloklangan",
                unlockLimit = r.opens, opensToday = opensToday, graceMinutes = r.graceMinutes
            )
        }
    }

    // 5) Kunlik ochish limiti
    for (r in rules) {
        if (!r.enabled || r.type != "limit") continue
        if (pkg !in ruleTargets(r, installed)) continue
        if (opensToday >= r.opens) {
            return BlockHit(
                BlockReason.LIMIT, r.id, r.title, r.icon,
                "Kunlik ${r.opens} ochish limiti tugadi",
                unlockLimit = r.opens, opensToday = opensToday, graceMinutes = r.graceMinutes
            )
        }
    }

    return null
}

/** Qoida kartasidagi jonli holat matni. */
fun ruleStatusLabel(rule: RuleSpec, nowMinutes: Int): String? {
    if (!rule.enabled) return null
    if (rule.type == "limit") return "Kuniga ${rule.opens} ochish · ${rule.graceMinutes} daq"
    val s = parseTimeToMinutes(rule.start)
    val e = parseTimeToMinutes(rule.end)
    if (isWithinWindow(nowMinutes, rule.start, rule.end)) return "Hozir faol"
    val until = if (nowMinutes < s) s - nowMinutes else 1440 - nowMinutes + s
    val h = until / 60
    val m = until % 60
    return if (h > 0) "${h}s ${m}d dan keyin" else "${m} daqiqadan keyin"
}
