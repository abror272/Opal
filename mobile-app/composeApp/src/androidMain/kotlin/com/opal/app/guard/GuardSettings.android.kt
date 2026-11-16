package com.opal.app.guard

import android.content.Intent
import com.opal.app.data.opalContext
import com.opal.app.data.prefs
import java.time.LocalDate

private const val KEY_ENABLED = "guard18_on"
private const val KEY_SENSITIVITY = "guard18_sens"
private const val KEY_HITS_DAY = "guard18_hits_day"
private const val KEY_HITS_TODAY = "guard18_hits_today"
private const val KEY_HITS_TOTAL = "guard18_hits_total"
private const val KEY_LAST_HIT = "guard18_last_hit"
private const val KEY_FP_DAY = "guard18_fp_day"
private const val KEY_FP_TODAY = "guard18_fp_today"
private const val KEY_CD_PREFIX = "guard18_cd_"

internal const val ACTION_GUARD_SELFTEST = "com.opal.app.action.GUARD18_SELFTEST"

private fun today(): Long = LocalDate.now().toEpochDay()

actual fun isAdultGuardEnabled(): Boolean = prefs().getBoolean(KEY_ENABLED, true)

actual fun setAdultGuardEnabled(on: Boolean) {
    prefs().edit().putBoolean(KEY_ENABLED, on).apply()
}

actual fun adultGuardSensitivityCode(): String =
    prefs().getString(KEY_SENSITIVITY, GuardSensitivity.MEDIUM.code)
        ?: GuardSensitivity.MEDIUM.code

actual fun setAdultGuardSensitivityCode(code: String) {
    prefs().edit().putString(KEY_SENSITIVITY, GuardSensitivity.from(code).code).apply()
}

actual fun adultGuardHitsToday(): Int {
    val p = prefs()
    return if (p.getLong(KEY_HITS_DAY, -1L) == today()) p.getInt(KEY_HITS_TODAY, 0) else 0
}

actual fun adultGuardHitsTotal(): Int = prefs().getInt(KEY_HITS_TOTAL, 0)

actual fun adultGuardLastHitAt(): Long = prefs().getLong(KEY_LAST_HIT, 0L)

actual fun recordAdultGuardHit() {
    val p = prefs()
    val day = today()
    val sameDay = p.getLong(KEY_HITS_DAY, -1L) == day
    val todayCount = if (sameDay) p.getInt(KEY_HITS_TODAY, 0) else 0
    p.edit()
        .putLong(KEY_HITS_DAY, day)
        .putInt(KEY_HITS_TODAY, todayCount + 1)
        .putInt(KEY_HITS_TOTAL, p.getInt(KEY_HITS_TOTAL, 0) + 1)
        .putLong(KEY_LAST_HIT, System.currentTimeMillis())
        .apply()
}

actual fun recordAdultGuardFalsePositive() {
    val p = prefs()
    val day = today()
    val sameDay = p.getLong(KEY_FP_DAY, -1L) == day
    val count = if (sameDay) p.getInt(KEY_FP_TODAY, 0) else 0
    p.edit()
        .putLong(KEY_FP_DAY, day)
        .putInt(KEY_FP_TODAY, count + 1)
        .apply()
}

actual fun adultGuardFalsePositivesToday(): Int {
    val p = prefs()
    return if (p.getLong(KEY_FP_DAY, -1L) == today()) p.getInt(KEY_FP_TODAY, 0) else 0
}

actual fun adultGuardCooldownUntil(packageName: String): Long {
    if (packageName.isBlank()) return 0L
    val until = prefs().getLong(KEY_CD_PREFIX + packageName, 0L)
    return if (until > System.currentTimeMillis()) until else 0L
}

actual fun setAdultGuardCooldown(packageName: String, millis: Long) {
    if (packageName.isBlank()) return
    prefs().edit()
        .putLong(KEY_CD_PREFIX + packageName, System.currentTimeMillis() + millis)
        .apply()
}

actual fun requestAdultGuardSelfTest() {
    try {
        val ctx = opalContext()
        ctx.sendBroadcast(
            Intent(ACTION_GUARD_SELFTEST).setPackage(ctx.packageName)
        )
    } catch (_: Throwable) {
    }
}

private const val KEY_VISUAL = "guard18_visual"

actual fun isAdultGuardVisualEnabled(): Boolean = prefs().getBoolean(KEY_VISUAL, true)

actual fun setAdultGuardVisualEnabled(on: Boolean) {
    prefs().edit().putBoolean(KEY_VISUAL, on).apply()
}
