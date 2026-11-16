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
import com.opal.app.guard.GuardKind
import com.opal.app.guard.GuardSensitivity
import com.opal.app.guard.GuardVerdict
import com.opal.app.guard.NsfwClassifier
import com.opal.app.guard.ScreenCapture
import com.opal.app.guard.adultGuardCooldownUntil
import com.opal.app.guard.adultGuardSensitivity
import com.opal.app.guard.isAdultGuardVisualEnabled
import com.opal.app.guard.isAdultGuardEnabled
import com.opal.app.guard.recordAdultGuardFalsePositive
import com.opal.app.guard.recordAdultGuardHit
import com.opal.app.guard.setAdultGuardCooldown
import java.util.concurrent.Executors

/**
 * QAT'IY BLOKLASH + KONTENT HIMOYASI.
 *
 * 1) Ilova bloklash: foydalanuvchi bloklangan ilovani ochsa — darhol
 *    to'liq ekranli "Panoh tomonidan bloklandi" oynasini (TYPE_ACCESSIBILITY_OVERLAY)
 *    ustiga chizadi.
 *
 * 2) Kontent himoyasi: (a) ekrandagi ko'rinadigan matn + manzil satri
 *    [AdultContentGuard] orqali, (b) ekran rasmi [NsfwClassifier] (TFLite)
 *    orqali tekshiriladi. Ikkalasi ham qurilmada, hech narsa saqlanmaydi.
 *    Aniqlash byudjetli (debounce, throttle, node limiti, vizual 1.8s) — UI qotmaydi.
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

    // kontent skanerlash holati
    private var lastGuardScanAt = 0L
    private var lastGuardTriggerAt = 0L
    private var pendingScanPkg: String? = null
    private var receiverRegistered = false

    // vizual (rasm/video) skanerlash holati
    private val visualExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var visualBusy = false
    private var pageGeneration = 0L
    private var lastVisualScanAt = 0L
    private var pendingVisualPkg: String? = null
    private var visualScheduled = false
    private var classifier: NsfwClassifier? = null

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val KEEP_ALIVE_ID = 0x0A12
        private const val KEEP_ALIVE_CHANNEL = "opal_service"

        /** Content-change eventlar juda ko'p — shu sababli kechikish + throttle. */
        private const val GUARD_DEBOUNCE_MS = 420L
        private const val GUARD_MIN_INTERVAL_MS = 900L
        private const val GUARD_TRIGGER_COOLDOWN_MS = 2_500L
        private const val GUARD_FALSE_POSITIVE_MS = 30 * 60_000L

        /** Rasm/video tekshiruvi — kamroq, lekin barqaror. */
        private const val VISUAL_DEBOUNCE_MS = 450L
        private const val VISUAL_MIN_INTERVAL_MS = 700L

        /** Hech qachon kontent uchun skanerlanmaydigan paketlar. */
        private val SCAN_SKIP = setOf(
            "com.android.systemui",
            "com.miui.securitycenter",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.miui.home",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.sohu.inputmethod.sogou",
            "com.sohu.inputmethod.sogou.xiaomi",
            "com.baidu.input",
            "com.iflytek.inputmethod",
            "com.miui.packageinstaller",
            "com.android.packageinstaller"
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
    private val visualScan = Runnable { runVisualScan() }

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
        handler.removeCallbacks(visualScan)
        runCatching {
            visualExecutor.execute {
                runCatching { classifier?.close() }
                classifier = null
            }
            visualExecutor.shutdown()
        }
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
                            "Panoh himoyasi",
                            android.app.NotificationManager.IMPORTANCE_MIN
                        ).apply { description = "Bloklash va kontent himoyasi faol" }
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
                .setContentTitle("Panoh himoyasi faol")
                .setContentText("Ilovalar va kontent kuzatilmoqda")
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

        // ---- kontent himoyasi: kontent o'zgardi ----
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
        // Yangi sahifa/oyna — eski skrinshot natijalari endi tegishli emas.
        pageGeneration++

        // Boshqa ilova ochildi — overlay'larni yopamiz.
        val shown = overlay.blockedPackage
        if (shown != null && shown != pkg) overlay.hide()

        val guardShown = guardOverlay.blockedPackage
        if (guardShown != null && guardShown != pkg) guardOverlay.hide()

        evaluate(pkg, recordOpens = true)
        scheduleGuardScan(pkg)
    }

    /* ==================== kontent skanerlash ==================== */

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
        scheduleVisualScan(pkg)
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
        android.util.Log.d("PanohGuard", "root=${root != null} pkg=$pkg")
        val screenHeight = try {
            resources.displayMetrics.heightPixels
        } catch (_: Throwable) {
            0
        }
        val snapshot = try {
            ScreenTextScanner.scan(root, screenHeight)
        } catch (_: Throwable) {
            ScreenSnapshot("", "")
        } finally {
            try {
                root?.recycle()
            } catch (_: Throwable) {
            }
        }
        android.util.Log.d(
            "PanohGuard",
            "text=${snapshot.text.length} addr=${snapshot.addressBar.length} pkg=$pkg"
        )
        if (snapshot.text.length < 3 && snapshot.addressBar.isBlank()) return

        val sensitivity = runCatching { adultGuardSensitivity() }.getOrDefault(
            GuardSensitivity.MEDIUM
        )
        val verdict = try {
            AdultContentGuard.evaluate(
                GuardInput(
                    screenText = snapshot.text,
                    addressBarText = snapshot.addressBar,
                    appLabel = appLabel(pkg)
                ),
                sensitivity
            )
        } catch (_: Throwable) {
            GuardVerdict()
        }
        android.util.Log.d(
            "PanohGuard",
            "verdict=${verdict.kind} sig=${verdict.signals} pkg=$pkg"
        )
        if (!verdict.isBlocking) return

        triggerGuard(pkg, verdict)
    }

    /* ==================== vizual (rasm/video) tekshiruv ==================== */

    private fun scheduleVisualScan(pkg: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (pkg.isBlank() || isIgnoredPackage(pkg)) return
        if (guardOverlay.isShowing || overlay.isShowing) return
        if (!runCatching { isAdultGuardEnabled() }.getOrDefault(false)) return
        if (!runCatching { isAdultGuardVisualEnabled() }.getOrDefault(false)) return
        if (adultGuardCooldownUntil(pkg) > System.currentTimeMillis()) return
        if (!isScreenOn()) return
        pendingVisualPkg = pkg
        // Muhim: har bir content event'da taymerni QAYTA BOSHLAMAYMIZ —
        // aks holda video oqimida rasm tekshiruvi umuman ishlamaydi.
        if (visualScheduled) return
        visualScheduled = true
        handler.postDelayed(visualScan, VISUAL_DEBOUNCE_MS)
    }

    private fun runVisualScan() {
        visualScheduled = false
        val pkg = pendingVisualPkg ?: return
        pendingVisualPkg = null

        if (visualBusy) return
        if (pkg != foregroundPkg) return
        if (isIgnoredPackage(pkg)) return
        if (guardOverlay.isShowing || overlay.isShowing) return
        if (!runCatching { isAdultGuardEnabled() }.getOrDefault(false)) return
        if (!runCatching { isAdultGuardVisualEnabled() }.getOrDefault(false)) return
        if (adultGuardCooldownUntil(pkg) > System.currentTimeMillis()) return
        if (!isScreenOn()) return
        if (!isVisualCandidate(pkg)) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastVisualScanAt < VISUAL_MIN_INTERVAL_MS) return
        lastVisualScanAt = now
        visualBusy = true
        val gen = pageGeneration

        ScreenCapture.capture(this, visualExecutor) { bitmap ->
            if (bitmap == null) {
                visualBusy = false
                return@capture
            }
            visualExecutor.execute {
                try {
                    val sensitivity = runCatching { adultGuardSensitivity() }
                        .getOrDefault(GuardSensitivity.MEDIUM)
                    val threshold = AdultContentGuard.visualThreshold(sensitivity)
                    val model = classifier ?: NsfwClassifier(this).also { classifier = it }
                    val score = runCatching { model.score(bitmap) }.getOrDefault(0f)
                    val luma = runCatching {
                        var sum = 0L
                        var count = 0
                        var yy = 0
                        while (yy < bitmap.height) {
                            var xx = 0
                            while (xx < bitmap.width) {
                                val c = bitmap.getPixel(xx, yy)
                                sum += ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)) / 3
                                count++
                                xx += 32
                            }
                            yy += 32
                        }
                        if (count > 0) (sum / count).toInt() else -1
                    }.getOrDefault(-1)
                    android.util.Log.d(
                        "PanohVision",
                        "score=${String.format("%.3f", score)} threshold=$threshold " +
                            "lum=$luma gen=$gen/$pageGeneration pkg=$pkg"
                    )
                    // Bir kadr — darhol qaror. Avvalgi "strike" tizimi sahifa
                    // almashganda eski ballni olib o'tib, kech bloklarga sabab bo'lardi.
                    if (score >= threshold) {
                        handler.post {
                            if (foregroundPkg == pkg &&
                                gen == pageGeneration &&
                                !guardOverlay.isShowing
                            ) {
                                triggerGuard(
                                    pkg,
                                    GuardVerdict(GuardKind.VISUAL, listOf("vizual"))
                                )
                            }
                        }
                    }
                } finally {
                    visualBusy = false
                    runCatching { bitmap.recycle() }
                }
            }
        }
    }

    /** Matnli, rasmli yoki video ko'rsatadigan ilovalar (rasm tekshiruvi uchun). */
    private fun isVisualCandidate(pkg: String): Boolean {
        // Brauzer, ijtimoiy tarmoq, galereya, video — accessibility oynasi bor ilovalar.
        // Tizim/launcher allaqachon SCAN_SKIP'da.
        return pkg != packageName && pkg !in SCAN_SKIP
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
                kind = GuardKind.TEXT,
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
