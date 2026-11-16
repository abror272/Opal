package com.opal.app.blocking

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
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
import com.opal.app.guard.ACTION_GUARD_SELFTEST
import com.opal.app.guard.AdultContentGuard
import com.opal.app.guard.GuardInput
import com.opal.app.guard.GuardVerdict
import com.opal.app.guard.adultGuardCooldownUntil
import com.opal.app.guard.adultGuardSensitivity
import com.opal.app.guard.isAdultGuardEnabled
import com.opal.app.guard.recordAdultGuardFalsePositive
import com.opal.app.guard.recordAdultGuardHit
import com.opal.app.guard.setAdultGuardCooldown

/**
 * QAT'IY BLOKLASH + 18+ HIMOYASI.
 *
 * 1) Ilova bloklash: foydalanuvchi bloklangan ilovani ochsa — darhol
 *    to'liq ekranli "Lumirra tomonidan bloklandi" oynasini (TYPE_ACCESSIBILITY_OVERLAY)
 *    ustiga chizadi.
 *
 * 2) 18+ himoyasi: ekrandagi matn/havola o'zgarganda (TYPE_WINDOW_CONTENT_CHANGED)
 *    daraxtdan matn yig'ib [AdultContentGuard] orqali tekshiradi. 18+ aniqlansa
 *    ekran [GuardOverlay] bilan yopiladi. Skanerlash qat'iy byudjetli (debounce,
 *    throttle, node limiti) — UI qotmaydi.
 *
 * Bloklash sabablari:
 *  1. Qo'lda bloklash ("Ilovalarim" bo'limida tanlangan)
 *  2. Qoidalar: "blockAll" (uyqu), "block" (ish), kunlik ochish limiti
 *  3. "allow" qoidalari boshqa sabablarni bekor qiladi (tushlik tanaffusi)
 */
class OpalAccessibilityService : AccessibilityService() {

    private val overlay by lazy { BlockOverlay(this) }
    private val guardOverlay by lazy { GuardOverlay(this) }
    private val notifier by lazy { RuleNotifier(this) }
    private var lastPackage: String? = null
    private var lastAt = 0L
    private var foregroundPkg: String? = null
    private var lastBeat = 0L

    // 18+ skanerlash holati
    private var lastGuardScanAt = 0L
    private var lastGuardTriggerAt = 0L
    private var pendingScanPkg: String? = null
    private var receiverRegistered = false

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val KEEP_ALIVE_ID = 0x0A12
        private const val KEEP_ALIVE_CHANNEL = "opal_service"

        /** Content-change eventlar juda ko'p — shu sababli kechikish + throttle. */
        private const val GUARD_DEBOUNCE_MS = 320L
        private const val GUARD_MIN_INTERVAL_MS = 650L
        private const val GUARD_TRIGGER_COOLDOWN_MS = 2_500L
        private const val GUARD_FALSE_POSITIVE_MS = 15 * 60_000L

        /** Hech qachon 18+ uchun skanerlanmaydigan paketlar. */
        private val SCAN_SKIP = setOf(
            "com.android.systemui",
            "com.miui.securitycenter",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller"
        )
    }

    private val ticker = object : Runnable {
        override fun run() {
            try {
                val rules = loadRules()
                notifier.sync(rules, nowMinutes())
                foregroundPkg?.let { evaluate(it, recordOpens = false) }
            } catch (_: Throwable) {
            }
            handler.postDelayed(this, 15_000L)
        }
    }

    private val guardScan = Runnable { runGuardScan() }

    private val selfTestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_GUARD_SELFTEST) return
            try {
                showGuardSelfTest()
            } catch (_: Throwable) {
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        beat(force = true)
        startKeepAlive()
        registerSelfTestReceiver()
        handler.removeCallbacks(ticker)
        // Birinchi snapshot: hozir faol qoidalar uchun bildirishnoma chiqarmaymiz.
        runCatching { notifier.sync(loadRules(), nowMinutes()) }
        handler.postDelayed(ticker, 15_000L)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clearHeartbeat()
        stopKeepAlive()
        unregisterSelfTestReceiver()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clearHeartbeat()
        stopKeepAlive()
        unregisterSelfTestReceiver()
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(guardScan)
        overlay.hide()
        guardOverlay.hide()
        super.onDestroy()
    }

    private fun registerSelfTestReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter(ACTION_GUARD_SELFTEST)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(selfTestReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(selfTestReceiver, filter)
            }
            receiverRegistered = true
        } catch (_: Throwable) {
        }
    }

    private fun unregisterSelfTestReceiver() {
        if (!receiverRegistered) return
        receiverRegistered = false
        try {
            unregisterReceiver(selfTestReceiver)
        } catch (_: Throwable) {
        }
    }

    /**
     * MIUI/HyperOS agressiv fon tozalashiga qarshi: xizmatni foreground
     * xizmatga aylantiramiz — jarayon tizim tomonidan o'ldirilmaydi.
     */
    private fun startKeepAlive() {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (nm.getNotificationChannel(KEEP_ALIVE_CHANNEL) == null) {
                    nm.createNotificationChannel(
                        android.app.NotificationChannel(
                            KEEP_ALIVE_CHANNEL,
                            "Lumirra himoyasi",
                            android.app.NotificationManager.IMPORTANCE_MIN
                        ).apply { description = "Bloklash va 18+ himoyasi faol" }
                    )
                }
            }
            val nm2 = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val intent = android.content.Intent(this, com.opal.app.MainActivity::class.java)
            val pi = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                        android.app.PendingIntent.FLAG_IMMUTABLE else 0)
            )
            val n = androidx.core.app.NotificationCompat.Builder(this, KEEP_ALIVE_CHANNEL)
                .setSmallIcon(com.opal.app.R.drawable.ic_notify)
                .setContentTitle("Lumirra himoyasi faol")
                .setContentText("Ilovalar va 18+ kontent kuzatilmoqda")
                .setOngoing(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pi)
                .build()
            startForeground(KEEP_ALIVE_ID, n)
        } catch (_: Throwable) {
        }
    }

    private fun stopKeepAlive() {
        try {
            stopForeground(true)
        } catch (_: Throwable) {
        }
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

        // ---- 18+ himoyasi: kontent o'zgardi ----
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val eventPkg = e.packageName?.toString() ?: return
            if (!isIgnoredPackage(eventPkg)) {
                if (foregroundPkg == null) foregroundPkg = eventPkg
                if (eventPkg == foregroundPkg) scheduleGuardScan(eventPkg)
            }
            return
        }

        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            e.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == "com.android.systemui" || pkg == "com.miui.securitycenter") return

        foregroundPkg = pkg

        // Boshqa ilova ochildi — overlay'larni yopamiz.
        val shown = overlay.blockedPackage
        if (shown != null && shown != pkg) overlay.hide()

        val guardShown = guardOverlay.blockedPackage
        if (guardShown != null && guardShown != pkg) guardOverlay.hide()

        evaluate(pkg, recordOpens = true)
        scheduleGuardScan(pkg)
    }

    /* ==================== 18+ skanerlash ==================== */

    private fun isIgnoredPackage(pkg: String): Boolean =
        pkg == packageName || pkg in SCAN_SKIP

    private fun scheduleGuardScan(pkg: String) {
        if (pkg.isBlank() || isIgnoredPackage(pkg)) return
        if (guardOverlay.isShowing) return
        if (!runCatching { isAdultGuardEnabled() }.getOrDefault(false)) return
        if (adultGuardCooldownUntil(pkg) > System.currentTimeMillis()) return
        pendingScanPkg = pkg
        handler.removeCallbacks(guardScan)
        handler.postDelayed(guardScan, GUARD_DEBOUNCE_MS)
    }

    private fun runGuardScan() {
        val pkg = pendingScanPkg ?: return
        pendingScanPkg = null

        if (pkg != foregroundPkg) return
        if (isIgnoredPackage(pkg)) return
        if (guardOverlay.isShowing || overlay.isShowing) return
        if (!isScreenOn()) return
        if (!runCatching { isAdultGuardEnabled() }.getOrDefault(false)) return
        if (adultGuardCooldownUntil(pkg) > System.currentTimeMillis()) return

        // Global throttle — og'ir ilovalarda ham UI qotmasligi uchun.
        val now = SystemClock.elapsedRealtime()
        if (now - lastGuardScanAt < GUARD_MIN_INTERVAL_MS) {
            scheduleGuardScan(pkg)
            return
        }
        lastGuardScanAt = now

        val root = try {
            rootInActiveWindow
        } catch (_: Throwable) {
            null
        }
        val text = try {
            ScreenTextScanner.scan(root)
        } catch (_: Throwable) {
            ""
        } finally {
            try {
                root?.recycle()
            } catch (_: Throwable) {
            }
        }
        if (text.length < 3) return

        val sensitivity = runCatching { adultGuardSensitivity() }.getOrDefault(
            com.opal.app.guard.GuardSensitivity.MEDIUM
        )
        val verdict = try {
            AdultContentGuard.evaluate(
                GuardInput(
                    screenText = text,
                    packageName = pkg,
                    appLabel = appLabel(pkg)
                )
            )
        } catch (_: Throwable) {
            GuardVerdict()
        }
        if (!verdict.isAdult(sensitivity)) return

        triggerGuard(pkg, verdict)
    }

    private fun triggerGuard(pkg: String, verdict: GuardVerdict) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastGuardTriggerAt < GUARD_TRIGGER_COOLDOWN_MS) return
        if (guardOverlay.isShowing && guardOverlay.blockedPackage == pkg) return
        lastGuardTriggerAt = now

        // Bloklash overlay'i ustma-ust tushmasin.
        overlay.hide()
        handler.removeCallbacks(guardScan)

        runCatching { recordAdultGuardHit() }

        guardOverlay.show(
            packageName = pkg,
            verdict = verdict,
            onExit = {
                guardOverlay.hide()
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
            onFalsePositive = {
                guardOverlay.hide()
                runCatching {
                    recordAdultGuardFalsePositive()
                    setAdultGuardCooldown(pkg, GUARD_FALSE_POSITIVE_MS)
                }
                launchPackage(pkg)
            }
        )
    }

    private fun showGuardSelfTest() {
        if (guardOverlay.isShowing) guardOverlay.hide()
        guardOverlay.show(
            packageName = packageName,
            verdict = GuardVerdict(
                score = 200,
                kind = com.opal.app.guard.GuardKind.SITE,
                signals = listOf("self-test")
            ),
            selfTest = true,
            onExit = { guardOverlay.hide() },
            onFalsePositive = { guardOverlay.hide() }
        )
    }

    private fun isScreenOn(): Boolean = try {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        pm.isInteractive
    } catch (_: Throwable) {
        true
    }

    private fun appLabel(pkg: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Throwable) {
        ""
    }

    private fun launchPackage(pkg: String) {
        try {
            val launch = packageManager.getLaunchIntentForPackage(pkg) ?: return
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        } catch (_: Throwable) {
        }
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
        if (guardOverlay.isShowing) return
        if (overlay.isShowing && overlay.blockedPackage == pkg) return

        val now = SystemClock.elapsedRealtime()
        if (!overlay.isShowing && pkg == lastPackage && now - lastAt < 800L) return
        lastPackage = pkg
        lastAt = now

        val pm = packageManager
        val label = appLabel(pkg).ifEmpty { pkg.substringAfterLast('.') }
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
            unlockLabel = if (hit.canUnlock) {
                "🔓 Limitdan foydalanish · ${hit.remainingUnlocks} ta (${hit.graceMinutes} daq)"
            } else {
                null
            },
            onDismiss = {
                overlay.hide()
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
            onAllow = {
                overlay.hide()
                com.opal.app.data.grantGrace(pkg, 5 * 60 * 1000L)
                launchPackage(pkg)
            },
            onUnlock = {
                overlay.hide()
                com.opal.app.data.recordUnlock(pkg)
                com.opal.app.data.grantGrace(pkg, hit.graceMinutes * 60_000L)
                launchPackage(pkg)
            }
        )
    }

    override fun onInterrupt() {}
}
