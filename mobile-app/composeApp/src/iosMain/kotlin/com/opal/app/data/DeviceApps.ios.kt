package com.opal.app.data

import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults

actual fun loadInstalledApps(): List<InstalledApp> = emptyList()

actual fun loadBlockedPackages(): Set<String> {
    val arr = NSUserDefaults.standardUserDefaults.arrayForKey("opal_blocked") ?: return emptySet()
    return arr.filterIsInstance<String>().toSet()
}

actual fun saveBlockedPackages(packages: Set<String>) {
    NSUserDefaults.standardUserDefaults.setObject(packages.toList(), "opal_blocked")
}

actual fun isStrictBlocking(): Boolean = NSUserDefaults.standardUserDefaults.boolForKey("opal_strict")

actual fun setStrictBlocking(active: Boolean) {
    NSUserDefaults.standardUserDefaults.setBool(active, "opal_strict")
}

actual fun isBlockingServiceEnabled(): Boolean = false

actual fun openBlockingSettings() {}

actual fun openAutostartSettings(): Boolean = false

actual fun isIgnoringBatteryOptimizations(): Boolean = true

actual fun requestIgnoreBatteryOptimizations() {}

actual fun watchdogRunning(): Boolean = false

actual fun startWatchdog() {}

actual fun canDrawOverlays(): Boolean = false

actual fun openOverlaySettings() {}

actual fun hasUsageAccess(): Boolean = false

actual fun openUsageAccessSettings() {}

actual fun canWriteSecureSettings(): Boolean = false

actual fun forceRebindAccessibility(): Boolean = false

actual fun realScreenTimeToday(): Int = 0

actual fun realScreenTimeByDay(days: Int): Map<String, Int> = emptyMap()

actual fun realPickupsToday(): Int = 0

actual fun grantGrace(packageName: String, millis: Long) {
    val until = (NSDate().timeIntervalSince1970 * 1000).toLong() + millis
    NSUserDefaults.standardUserDefaults.setInteger(until.toLong(), "grace_$packageName")
}

actual fun inGrace(packageName: String): Boolean {
    val until = NSUserDefaults.standardUserDefaults.integerForKey("grace_$packageName")
    val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
    return until > now
}

actual fun recordAppOpen(packageName: String) {}

actual fun recordUnlock(packageName: String) {}

actual fun appOpensToday(packageName: String): Int = 0

actual fun installedSocialPackages(): Set<String> = emptySet()
