package com.opal.app.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.opal.app.data.appOpensToday
import com.opal.app.data.evaluateBlock
import com.opal.app.data.inGrace
import com.opal.app.data.installedSocialPackages
import com.opal.app.data.isStrictBlocking
import com.opal.app.data.loadBlockedPackages
import com.opal.app.data.loadRules
import com.opal.app.data.prefs
import com.opal.app.data.recordAppOpen

/**
 * QAT'IY BLOKLASH: foydalanuvchi bloklangan ilovani ochsa — darhol
 * to'liq ekranli "Opal tomonidan bloklandi" oynasini (TYPE_ACCESSIBILITY_OVERLAY)
 * ustiga chizadi. Fon-aktivlik cheklovlari bunga ta'sir qilmaydi.
 *
 * Bloklash sabablari:
 *  1. Qo'lda bloklash ("Ilovalarim" bo'limida tanlangan)
 *  2. Qoidalar: "blockAll" (uyqu), "block" (ish), kunlik ochish limiti
 *  3. "allow" qoidalari boshqa sabablarni bekor qiladi (tushlik tanaffusi)
 */
class OpalAccessibilityService : AccessibilityService() {

    private val overlay by lazy { BlockOverlay(this) }
    private var lastPackage: String? = null
    private var lastAt = 0L
    private var foregroundPkg: String? = null
    private var lastBeat = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            try {
                foregroundPkg?.let { evaluate(it, recordOpens = false) }
            } catch (_: Throwable) {
            }
            handler.postDelayed(this, 15_000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        beat(force = true)
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 15_000L)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clearHeartbeat()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clearHeartbeat()
        handler.removeCallbacks(ticker)
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        beat()

        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            e.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == "com.android.systemui" || pkg == "com.miui.securitycenter") return

        foregroundPkg = pkg

        // Boshqa ilova ochildi — overlay'ni yopamiz.
        val shown = overlay.blockedPackage
        if (shown != null && shown != pkg) overlay.hide()

        evaluate(pkg, recordOpens = true)
    }

    private fun nowMinutes(): Int {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
    }

    /** Qoida/qo'lda bloklash bo'yicha qaror qabul qiladi va overlay ko'rsatadi. */
    private fun evaluate(pkg: String, recordOpens: Boolean) {
        val hit = evaluateBlock(
            pkg = pkg,
            manuallyBlocked = loadBlockedPackages(),
            rules = loadRules(),
            installed = installedSocialPackages(),
            nowMinutes = nowMinutes(),
            opensToday = appOpensToday(pkg)
        )

        if (hit == null) {
            if (recordOpens) recordAppOpen(pkg)
            return
        }

        if (inGrace(pkg)) return
        if (overlay.isShowing && overlay.blockedPackage == pkg) return

        val now = SystemClock.elapsedRealtime()
        if (!overlay.isShowing && pkg == lastPackage && now - lastAt < 800L) return
        lastPackage = pkg
        lastAt = now

        val pm = packageManager
        val label = try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
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
            reasonTitle = "${hit.icon} ${hit.title}",
            reasonDetail = hit.detail,
            strict = isStrictBlocking(),
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
