package com.opal.app.guard

/**
 * iOS: hozircha 18+ ekran himoyasi mavjud emas.
 * iOS'da boshqa ilova ekranini o'qish uchun API yo'q (Screen Time / Family
 * Controls faqat Apple'ning o'z tizimi orqali ishlaydi). Shu sababli barcha
 * chaqiruvlar xavfsiz no-op — UI'da "faqat Android" deb ko'rsatiladi.
 */

actual fun isAdultGuardEnabled(): Boolean = false
actual fun setAdultGuardEnabled(on: Boolean) {}
actual fun adultGuardSensitivityCode(): String = GuardSensitivity.MEDIUM.code
actual fun setAdultGuardSensitivityCode(code: String) {}
actual fun adultGuardHitsToday(): Int = 0
actual fun adultGuardHitsTotal(): Int = 0
actual fun adultGuardLastHitAt(): Long = 0L
actual fun recordAdultGuardHit() {}
actual fun recordAdultGuardFalsePositive() {}
actual fun adultGuardFalsePositivesToday(): Int = 0
actual fun adultGuardCooldownUntil(packageName: String): Long = 0L
actual fun setAdultGuardCooldown(packageName: String, millis: Long) {}
actual fun requestAdultGuardSelfTest() {}
