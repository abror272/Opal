package com.opal.app.guard

/**
 * 18+ himoyasi sozlamalari va statistikasi (platformadan mustaqil API).
 *
 * Android'da SharedPreferences'da saqlanadi; iOS'da hozircha ekran matnini
 * o'qish imkoni yo'q (Apple ruxsat bermaydi), shuning uchun no-op.
 */

/** Himoya yoqilganmi? (default: YOQILGAN — "yuqori o'rve" talabi) */
expect fun isAdultGuardEnabled(): Boolean
expect fun setAdultGuardEnabled(on: Boolean)

/** "low" | "medium" | "high" */
expect fun adultGuardSensitivityCode(): String
expect fun setAdultGuardSensitivityCode(code: String)

/** Bugungi 18+ bloklar soni (kun almashsa 0 dan boshlanadi). */
expect fun adultGuardHitsToday(): Int

/** Umumiy bloklar soni. */
expect fun adultGuardHitsTotal(): Int

/** Oxirgi blok vaqti (epoch ms, 0 = hech qachon). */
expect fun adultGuardLastHitAt(): Long

/** Blok qayd etish. */
expect fun recordAdultGuardHit()

/** "Bu xato" bosilganda qayd etish. */
expect fun recordAdultGuardFalsePositive()

/** Bugun nechta "xato" deb belgilangan. */
expect fun adultGuardFalsePositivesToday(): Int

/** Shu ilova uchun vaqtinchalik istisno (epoch ms gacha). */
expect fun adultGuardCooldownUntil(packageName: String): Long
expect fun setAdultGuardCooldown(packageName: String, millis: Long)

/** UI'dan haqiqiy overlay'ni sinash (accessibility xizmati orqali). */
expect fun requestAdultGuardSelfTest()

/** Yordamchi: kod -> enum. */
fun adultGuardSensitivity(): GuardSensitivity =
    GuardSensitivity.from(adultGuardSensitivityCode())

fun setAdultGuardSensitivity(value: GuardSensitivity) =
    setAdultGuardSensitivityCode(value.code)

/**
 * Vizual tekshirish (ekran rasmi / rasm-video) yoqilganmi?
 * Android 11+ da ishlaydi; iOS'da yo'q.
 */
expect fun isAdultGuardVisualEnabled(): Boolean
expect fun setAdultGuardVisualEnabled(on: Boolean)
