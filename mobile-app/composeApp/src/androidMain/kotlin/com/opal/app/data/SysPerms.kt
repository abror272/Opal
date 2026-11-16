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
 * WRITE_SECURE_SETTINGS ATAYLAB ISHLATILMAYDI — bu ruxsat xavfsizlik skanerlarida
 * yuqori risk deb belgilanadi. Shuning uchun o'z-o'zini tiklash o'chirilgan:
 * xizmat o'chsa WatchdogService ikkinchi dvigatel sifatida ishlaydi.
 */
actual fun canWriteSecureSettings(): Boolean = false

actual fun forceRebindAccessibility(): Boolean = false

/** Xizmat haqiqatan tizimga ulanganmi (heartbeat'dan qat'i nazar). */
fun isAccessibilityBound(): Boolean = AccessibilityBridge.isBound()

internal object AccessibilityBridge {
    @Volatile
    var bound: Boolean = false

    fun isBound(): Boolean = bound
}
