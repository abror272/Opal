package com.opal.app.data

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Bloklashning IKKINCHI dvigateli uchun tizim ruxsatlari.
 * MIUI accessibility xizmatini o'chirib qo'yganda ham blok ishlashi uchun.
 */

private const val OPAL_A11Y_COMPONENT =
    "com.opal.app/com.opal.app.blocking.OpalAccessibilityService"

/** "Boshqa ilovalar ustida ko'rsatish" (SYSTEM_ALERT_WINDOW). */
actual fun canDrawOverlays(): Boolean = try {
    Settings.canDrawOverlays(opalContext())
} catch (_: Throwable) {
    false
}

actual fun openOverlaySettings() {
    val ctx = opalContext()
    try {
        ctx.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.fromParts("package", ctx.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Throwable) {
    }
}

/** "Foydalanish tarixi" (PACKAGE_USAGE_STATS) ruxsati. */
actual fun hasUsageAccess(): Boolean {
    val ctx = opalContext()
    return try {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Throwable) {
        false
    }
}

actual fun openUsageAccessSettings() {
    val ctx = opalContext()
    try {
        ctx.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Throwable) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
        }
    }
}

/**
 * WRITE_SECURE_SETTINGS — bir marta `adb shell pm grant` bilan beriladi.
 * Bo'lsa, Opal o'z accessibility xizmatini O'ZI qayta yoqadi (MIUI o'chirsa ham).
 */
actual fun canWriteSecureSettings(): Boolean = try {
    opalContext().checkPermission(
        android.Manifest.permission.WRITE_SECURE_SETTINGS,
        Process.myPid(),
        Process.myUid()
    ) == PackageManager.PERMISSION_GRANTED
} catch (_: Throwable) {
    false
}

/** Accessibility xizmatini majburiy qayta ulash (toggle) — MIUI "enabled but dead" holati uchun. */
actual fun forceRebindAccessibility(): Boolean {
    if (!canWriteSecureSettings()) return false
    val ctx = opalContext()
    return try {
        val cr = ctx.contentResolver
        val cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val set = cur.split(':').filter { it.isNotBlank() }.toMutableSet()
        set.add(OPAL_A11Y_COMPONENT)
        val joined = set.joinToString(":")

        // O'chirib-yoqish binding'ni majburiy yangilaydi.
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
        Settings.Secure.putString(cr, Settings.Secure.ACCESSIBILITY_ENABLED, "0")
        android.os.SystemClock.sleep(220)
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, joined)
        Settings.Secure.putString(cr, Settings.Secure.ACCESSIBILITY_ENABLED, "1")
        true
    } catch (_: Throwable) {
        false
    }
}

/** Xizmat haqiqatan tizimga ulanganmi (heartbeat'dan qat'i nazar). */
fun isAccessibilityBound(): Boolean = AccessibilityBridge.isBound()

internal object AccessibilityBridge {
    @Volatile
    var bound: Boolean = false

    fun isBound(): Boolean = bound
}
