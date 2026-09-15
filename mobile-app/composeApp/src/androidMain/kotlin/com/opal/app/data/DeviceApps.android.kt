package com.opal.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.PowerManager
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.opal.app.OpalApplication

internal fun opalContext(): Context = OpalApplication.instance

private const val PREFS = "opal_block"
internal fun prefs() = opalContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

actual fun loadInstalledApps(): List<InstalledApp> {
    val context = opalContext()
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    } catch (_: Throwable) {
        emptyList()
    }

    val self = context.packageName
    val seen = HashSet<String>()
    val out = ArrayList<InstalledApp>(resolved.size)
    for (ri in resolved) {
        val ai = ri.activityInfo?.applicationInfo ?: continue
        val pkg = ai.packageName ?: continue
        if (pkg == self || !seen.add(pkg)) continue
        val label = try {
            pm.getApplicationLabel(ai).toString()
        } catch (_: Throwable) {
            pkg
        }
        val icon = try {
            pm.getApplicationIcon(ai).toImageBitmap(120)
        } catch (_: Throwable) {
            null
        }
        out += InstalledApp(pkg, label, icon)
    }
    return out.sortedBy { it.label.lowercase() }
}

private fun Drawable.toImageBitmap(target: Int): ImageBitmap? {
    try {
        if (this is BitmapDrawable && bitmap != null) {
            return Bitmap.createScaledBitmap(bitmap, target, target, true).asImageBitmap()
        }
        val w = intrinsicWidth.takeIf { it > 0 } ?: target
        val h = intrinsicHeight.takeIf { it > 0 } ?: target
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, w, h)
        draw(canvas)
        return if (w == target && h == target) bmp.asImageBitmap()
        else Bitmap.createScaledBitmap(bmp, target, target, true).asImageBitmap()
    } catch (_: Throwable) {
        return null
    }
}

actual fun loadBlockedPackages(): Set<String> =
    prefs().getStringSet("blocked", emptySet())?.toSet() ?: emptySet()

actual fun saveBlockedPackages(packages: Set<String>) {
    prefs().edit().putStringSet("blocked", HashSet(packages)).apply()
}

actual fun isStrictBlocking(): Boolean = prefs().getBoolean("strict", false)

actual fun setStrictBlocking(active: Boolean) {
    prefs().edit().putBoolean("strict", active).apply()
}

actual fun isBlockingServiceEnabled(): Boolean {
    val context = opalContext()
    val now = System.currentTimeMillis()

    // 1) Accessibility xizmati "heartbeat"i — eng ishonchli (MIUI cheklovlaridan mustaqil).
    val hb = prefs().getLong("svc_hb", 0L)
    if (hb > 0L && now - hb < 30_000L) return true

    // 2) Watchdog (overlay + usage access) ishlayaptimi — MIUI accessibility'ni o'ldirsa ham bloklaydi.
    val wd = prefs().getLong("wd_hb", 0L)
    if (wd > 0L && now - wd < 30_000L && prefs().getBoolean("wd_ok", false)) return true

    // 2) Secure settings (ba'zi qurilmalarda ishlaydi)
    try {
        val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        android.util.Log.d("OpalBlock", "secure=$flat")
        if (!flat.isNullOrBlank()) {
            val expected = context.packageName + "/com.opal.app.blocking.OpalAccessibilityService"
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(flat)
            while (splitter.hasNext()) {
                val item = splitter.next()
                if (item.equals(expected, ignoreCase = true)) return true
                if (item.equals(context.packageName + "/.blocking.OpalAccessibilityService", ignoreCase = true)) return true
            }
        }
    } catch (_: Throwable) {
    }

    // 3) AccessibilityManager
    try {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabled = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabled != null && enabled.any { it.resolveInfo?.serviceInfo?.packageName == context.packageName }) {
            return true
        }
    } catch (_: Throwable) {
    }

    return false
}

actual fun openBlockingSettings() {
    val context = opalContext()
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Throwable) {
    }
}

/** MIUI / EMUI / ColorOS / OnePlus / Samsung "autostart" ekranlari. */
private val AUTOSTART_COMPONENTS = listOf(
    ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
    ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
    ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
    ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
    ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
    ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
    ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
    ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
)

actual fun openAutostartSettings(): Boolean {
    val context = opalContext()
    val pm = context.packageManager
    for (component in AUTOSTART_COMPONENTS) {
        try {
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(intent)
                return true
            }
        } catch (_: Throwable) {
        }
    }
    // Zaxira: ilova sozlamalari sahifasi.
    return try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Throwable) {
        false
    }
}

actual fun isIgnoringBatteryOptimizations(): Boolean {
    return try {
        val pm = opalContext().getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(opalContext().packageName) ?: true
    } catch (_: Throwable) {
        true
    }
}

actual fun watchdogRunning(): Boolean = prefs().getBoolean("watchdog_on", false)

actual fun startWatchdog() {
    com.opal.app.blocking.WatchdogService.start(opalContext())
}

actual fun requestIgnoreBatteryOptimizations() {
    val context = opalContext()
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
        }
    }
}

actual fun grantGrace(packageName: String, millis: Long) {
    val until = System.currentTimeMillis() + millis
    prefs().edit().putLong("grace_$packageName", until).apply()
}

actual fun inGrace(packageName: String): Boolean {
    val until = prefs().getLong("grace_$packageName", 0L)
    return until > System.currentTimeMillis()
}

/* ---------- Kunlik ochish hisobi ---------- */

private fun todayKey(): String {
    val c = java.util.Calendar.getInstance()
    return "%04d%02d%02d".format(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
}

actual fun appOpensToday(packageName: String): Int {
    val p = prefs()
    if (p.getString("opens_day", null) != todayKey()) return 0
    return p.getInt("opens_$packageName", 0)
}

actual fun recordAppOpen(packageName: String) {
    val p = prefs()
    val today = todayKey()
    val now = System.currentTimeMillis()
    // Bir xil paketni 20 sekund ichida qayta sanamaymiz (WINDOW_STATE_CHANGED ko'p marta keladi).
    if (p.getString("last_open_pkg", null) == packageName && now - p.getLong("last_open_at", 0L) < 20_000L) return
    val edit = p.edit()
    if (p.getString("opens_day", null) != today) {
        p.all.keys.filter { it.startsWith("opens_") && it != "opens_day" }.forEach { edit.remove(it) }
        edit.putString("opens_day", today)
    }
    val prev = if (p.getString("opens_day", null) == today) p.getInt("opens_$packageName", 0) else 0
    edit.putInt("opens_$packageName", prev + 1)
    edit.putString("last_open_pkg", packageName)
    edit.putLong("last_open_at", now)
    edit.apply()
}

private var socialCache: Set<String>? = null

actual fun installedSocialPackages(): Set<String> {
    socialCache?.let { return it }
    val pm = opalContext().packageManager
    val out = HashSet<String>()
    for (p in SOCIAL_PACKAGES) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(p, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(p, 0)
            }
            out += p
        } catch (_: Throwable) {
        }
    }
    socialCache = out
    return out
}
