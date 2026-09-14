package com.opal.app.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.opal.app.data.inGrace
import com.opal.app.data.isStrictBlocking
import com.opal.app.data.loadBlockedPackages
import com.opal.app.data.prefs

/**
 * QAT'IY BLOKLASH: foydalanuvchi bloklangan ilovani ochsa — darhol
 * to'liq ekranli "Opal tomonidan bloklandi" oynasini (TYPE_ACCESSIBILITY_OVERLAY)
 * ustiga chizadi. Fon-aktivlik cheklovlari bunga ta'sir qilmaydi.
 */
class OpalAccessibilityService : AccessibilityService() {

    private val overlay by lazy { BlockOverlay(this) }
    private var lastPackage: String? = null
    private var lastAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        beat(force = true)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clearHeartbeat()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clearHeartbeat()
        overlay.hide()
        super.onDestroy()
    }

    private fun clearHeartbeat() {
        try {
            prefs().edit().putLong("svc_hb", 0L).apply()
        } catch (_: Throwable) {
        }
    }

    /** Xizmat tirikligini bildiruvchi belgi. */
    private fun beat(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastBeat < 5_000L) return
        lastBeat = now
        try {
            prefs().edit().putLong("svc_hb", now).apply()
        } catch (_: Throwable) {
        }
    }

    private var lastBeat = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        beat()

        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            e.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == "com.android.systemui" || pkg == "com.miui.securitycenter") return

        // Boshqa ilova ochildi — overlay'ni yopamiz.
        val shown = overlay.blockedPackage
        if (shown != null && shown != pkg) {
            overlay.hide()
        }

        if (!isStrictBlocking()) return
        if (!loadBlockedPackages().contains(pkg)) return
        if (inGrace(pkg)) return

        // Overlay allaqachon shu ilova uchun ko'rsatilgan bo'lsa — qayta chizmaymiz.
        if (overlay.isShowing && overlay.blockedPackage == pkg) return

        val now = SystemClock.elapsedRealtime()
        if (!overlay.isShowing && pkg == lastPackage && now - lastAt < 800L) return
        lastPackage = pkg
        lastAt = now

        val pm = packageManager
        val label = try {
            val ai = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (_: Throwable) {
            pkg.substringAfterLast('.')
        }
        val icon = try {
            pm.getApplicationIcon(pkg)
        } catch (_: Throwable) {
            null
        }

        overlay.show(
            packageName = pkg,
            label = label,
            icon = icon,
            onDismiss = {
                overlay.hide()
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
            onAllow = {
                overlay.hide()
                com.opal.app.data.grantGrace(pkg, 5 * 60 * 1000L)
                val launch = pm.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        startActivity(launch)
                    } catch (_: Throwable) {
                    }
                }
            }
        )
    }

    override fun onInterrupt() {}
}
