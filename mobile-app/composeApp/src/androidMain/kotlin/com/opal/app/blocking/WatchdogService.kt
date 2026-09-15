package com.opal.app.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.opal.app.R
import com.opal.app.data.appOpensToday
import com.opal.app.data.canDrawOverlays
import com.opal.app.data.currentForegroundPackage
import com.opal.app.data.evaluateBlock
import com.opal.app.data.forceRebindAccessibility
import com.opal.app.data.grantGrace
import com.opal.app.data.hasUsageAccess
import com.opal.app.data.inGrace
import com.opal.app.data.installedSocialPackages
import com.opal.app.data.isStrictBlocking
import com.opal.app.data.loadBlockedPackages
import com.opal.app.data.loadRules
import com.opal.app.data.prefs
import com.opal.app.data.recordAppOpen
import java.util.Calendar

/**
 * IKKINCHI BLOKLASH DVIGATELI (MIUI'ga qarshi).
 *
 * MIUI accessibility xizmatini "yoqilgan" deb turib, aslida ishlamay qo'yadi
 * (Bound services:{}). Shuning uchun bu foreground xizmat:
 *   1. UsageStats orqali ekrandagi ilovani kuzatadi,
 *   2. bloklanishi kerak bo'lsa TYPE_APPLICATION_OVERLAY chizadi,
 *   3. accessibility xizmatini davriy ravishda qayta ulaydi (WRITE_SECURE_SETTINGS bo'lsa).
 *
 * Accessibility ishlab turganda bu xizmat faqat kuzatuvchi (redundant overlay chizmaydi).
 */
class WatchdogService : Service() {

    private lateinit var overlay: BlockOverlay
    private val handler = Handler(Looper.getMainLooper())
    private var lastPkg: String? = null
    private var tickCount = 0L

    companion object {
        const val CHANNEL = "opal_watchdog"
        const val NOTIF_ID = 0x0A13

        fun start(ctx: Context) {
            try {
                val i = Intent(ctx, WatchdogService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(i)
                } else {
                    ctx.startService(i)
                }
            } catch (_: Throwable) {
            }
        }

        fun stop(ctx: Context) {
            try {
                ctx.stopService(Intent(ctx, WatchdogService::class.java))
            } catch (_: Throwable) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlay = BlockOverlay(this, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        startForegroundCompat()
        prefs().edit().putBoolean("watchdog_on", true).apply()
        handler.postDelayed(tick, 600L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        overlay.hide()
        try {
            prefs().edit().putBoolean("watchdog_on", false).putBoolean("wd_ok", false).apply()
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }

    private val tick = object : Runnable {
        override fun run() {
            try {
                step()
            } catch (_: Throwable) {
            }
            handler.postDelayed(this, 1000L)
        }
    }

    private fun step() {
        tickCount++

        val overlayOk = canDrawOverlays() && hasUsageAccess()
        val now = System.currentTimeMillis()
        prefs().edit().putLong("wd_hb", now).putBoolean("wd_ok", overlayOk).apply()

        // Har ~20 sekundda accessibility xizmatini "tiriltirish"ga urinamiz.
        if (tickCount % 20L == 0L) tryRepair()

        if (!overlayOk) return

        val a11yAlive = runCatching {
            val hb = prefs().getLong("svc_hb", 0L)
            hb > 0L && now - hb < 20_000L
        }.getOrDefault(false)
        // Accessibility ishlayapti — u o'zi bloklaydi, dublikat chizmaymiz.
        if (a11yAlive) {
            if (overlay.isShowing) overlay.hide()
            return
        }

        val pkg = currentForegroundPackage() ?: return
        if (pkg == packageName || pkg == "com.miui.home" || pkg == "com.android.systemui") {
            if (overlay.isShowing) overlay.hide()
            lastPkg = pkg
            return
        }

        val firstSeen = pkg != lastPkg
        lastPkg = pkg

        val hit = evaluateBlock(
            pkg = pkg,
            manuallyBlocked = loadBlockedPackages(),
            rules = loadRules(),
            installed = installedSocialPackages(),
            nowMinutes = nowMinutes(),
            opensToday = appOpensToday(pkg)
        )

        if (hit == null) {
            if (firstSeen) recordAppOpen(pkg)
            if (overlay.isShowing) overlay.hide()
            return
        }
        if (inGrace(pkg)) return
        if (overlay.isShowing && overlay.blockedPackage == pkg) return

        showBlock(pkg, hit.icon + " " + hit.title, hit.detail)
    }

    private fun showBlock(pkg: String, reasonTitle: String, reasonDetail: String) {
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
            reasonTitle = reasonTitle,
            reasonDetail = reasonDetail,
            strict = isStrictBlocking(),
            onDismiss = {
                overlay.hide()
                goHome()
            },
            onAllow = {
                overlay.hide()
                grantGrace(pkg, 5 * 60 * 1000L)
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

    private fun goHome() {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
        }
    }

    private fun tryRepair() {
        val alive = runCatching {
            val hb = prefs().getLong("svc_hb", 0L)
            hb > 0L && System.currentTimeMillis() - hb < 20_000L
        }.getOrDefault(false)
        if (!alive) forceRebindAccessibility()
    }

    private fun nowMinutes(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Opal himoyasi", NotificationManager.IMPORTANCE_MIN)
                        .apply { description = "Bloklash xizmati faol" }
                )
            }
        }
        val pi = PendingIntent.getActivity(
            this,
            1,
            Intent(this, com.opal.app.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("Opal himoyasi faol")
            .setContentText("Bloklangan ilovalar kuzatilmoqda")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pi)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (_: Throwable) {
            try {
                startForeground(NOTIF_ID, n)
            } catch (_: Throwable) {
            }
        }
    }
}
