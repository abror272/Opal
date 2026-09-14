package com.opal.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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

    // 1) Xizmat "heartbeat"i — eng ishonchli (MIUI cheklovlaridan mustaqil).
    val hb = prefs().getLong("svc_hb", 0L)
    if (hb > 0L && System.currentTimeMillis() - hb < 120_000L) return true

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

actual fun grantGrace(packageName: String, millis: Long) {
    val until = System.currentTimeMillis() + millis
    prefs().edit().putLong("grace_$packageName", until).apply()
}

actual fun inGrace(packageName: String): Boolean {
    val until = prefs().getLong("grace_$packageName", 0L)
    return until > System.currentTimeMillis()
}
