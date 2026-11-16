package com.opal.app.blocking

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.opal.app.guard.GuardVerdict
import kotlin.random.Random

/**
 * 18+ kontent aniqlanganda chiziladigan to'liq ekranli to'siq.
 *
 * Dizayn tamoyillari:
 *  - Ekrandagi nojo'ya kontent BUTUNLAY yopiladi (opaque fon).
 *  - 5 soniyalik "nafas olish" hisoblagichi — impuls bilan bir bosishda
 *    chiqib ketishning oldini oladi (lekin hech qachon qotmaydi).
 *  - Faqat ikkita yo'l: "Chiqish" (bosh ekranga) yoki "Bu xato" (istisno).
 *  - Matnlar o'zini tiyishga undaydi, ayblamaydi.
 *
 * Xizmat uni [android.accessibilityservice.AccessibilityService] overlay'i
 * sifatida ham, TYPE_APPLICATION_OVERLAY sifatida ham ko'rsatishi mumkin.
 */
class GuardOverlay(
    private val service: Context,
    private val windowType: Int = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
) {

    private var host: FrameLayout? = null
    private var countdownView: TextView? = null
    private var primary: TextView? = null
    private var secondary: TextView? = null
    private var pulse: PulseRingView? = null

    private var exitCb: () -> Unit = {}
    private var falsePositiveCb: () -> Unit = {}
    private var remaining = 0
    private var selfTest = false

    private val handler = Handler(Looper.getMainLooper())

    var blockedPackage: String? = null
        private set

    var isShowing: Boolean = false
        private set

    private val tick = object : Runnable {
        override fun run() {
            if (!isShowing) return
            remaining -= 1
            if (remaining <= 0) {
                countdownView?.text = "Endi xavfsiz chiqishingiz mumkin"
                countdownView?.setTextColor(AColor.parseColor("#8FA9E8B8"))
                enableButtons(true)
            } else {
                countdownView?.text = "Nafas oling · $remaining"
                handler.postDelayed(this, 1_000L)
            }
        }
    }

    fun show(
        packageName: String,
        verdict: GuardVerdict,
        selfTest: Boolean = false,
        onExit: () -> Unit,
        onFalsePositive: () -> Unit
    ) {
        this.blockedPackage = packageName
        this.selfTest = selfTest
        this.exitCb = onExit
        this.falsePositiveCb = onFalsePositive
        this.remaining = if (selfTest) 1 else COUNTDOWN_SECONDS

        val root = host ?: createHost() ?: return
        render(root, verdict)
        isShowing = true

        // Yumshoq paydo bo'lish — to'satdan "sakrash" emas.
        root.alpha = 0f
        root.scaleX = 0.985f
        root.scaleY = 0.985f
        root.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        handler.removeCallbacks(tick)
        enableButtons(false)
        countdownView?.text = "Nafas oling · $remaining"
        if (remaining > 0) handler.postDelayed(tick, 1_000L) else enableButtons(true)
    }

    fun hide() {
        handler.removeCallbacks(tick)
        pulse?.let { it.animate().cancel(); it.alpha = 0f }
        val h = host ?: run {
            isShowing = false
            blockedPackage = null
            return
        }
        try {
            (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(h)
        } catch (_: Throwable) {
        }
        host = null
        isShowing = false
        blockedPackage = null
        countdownView = null
        primary = null
        secondary = null
        pulse = null
    }

    /* ==================== ko'rinish ==================== */

    private fun createHost(): FrameLayout? {
        val root = FrameLayout(service).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(AColor.parseColor("#0A0F14"), AColor.parseColor("#030507"))
            )
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            // "Orqaga" tugmasi to'siqni yopmasin — faqat ongli tanlov.
            setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.graphics.PixelFormat.OPAQUE
        ).apply { gravity = Gravity.TOP or Gravity.START }

        return try {
            (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(root, params)
            host = root
            root.post { root.requestFocus() }
            root
        } catch (_: Throwable) {
            null
        }
    }

    private fun render(root: FrameLayout, verdict: GuardVerdict) {
        root.removeAllViews()

        val scroll = ScrollView(service).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }
        val column = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28f), dp(34f), dp(28f), dp(28f))
        }
        scroll.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 18+ nishoni (atrofida "nafas oluvchi" halqa)
        val badge = TextView(service).apply {
            text = "!"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(AColor.parseColor("#FFD9DF"))
            background = GradientDrawable().apply {
                cornerRadius = dpf(26f)
                setColor(AColor.parseColor("#2EFF5A76"))
                setStroke(dp(1.5f), AColor.parseColor("#8CFF5A76"))
            }
        }
        val ring = PulseRingView(service)
        pulse = ring
        val badgeWrap = FrameLayout(service).apply {
            addView(
                ring,
                FrameLayout.LayoutParams(dp(128f), dp(128f)).apply { gravity = Gravity.CENTER }
            )
            addView(
                badge,
                FrameLayout.LayoutParams(dp(88f), dp(88f)).apply { gravity = Gravity.CENTER }
            )
        }
        column.addView(badgeWrap, LinearLayout.LayoutParams(dp(150f), dp(150f)))

        if (selfTest) {
            column.addView(space(12f))
            column.addView(
                label("SINOV REJIMI · shunday ko'rinadi", 10.5f, "#8FA9E8B8").apply {
                    background = GradientDrawable().apply {
                        cornerRadius = dpf(50f)
                        setColor(AColor.parseColor("#1FA9E8B8"))
                    }
                    setPadding(dp(12f), dp(5f), dp(12f), dp(5f))
                }
            )
        }

        column.addView(space(24f))
        column.addView(label("To'xtang.", 30f, "#FFFFFF", bold = true))
        column.addView(space(8f))
        column.addView(label("Bu sahifa sizga foyda bermaydi.", 15f, "#B3FFFFFF"))

        column.addView(space(24f))

        // O'zini tiyishga undovchi gaplar (har safar aralashib turadi).
        val nudges = NUDGES.shuffled(Random(System.nanoTime())).take(3)
        val card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
            background = GradientDrawable().apply {
                cornerRadius = dpf(22f)
                setColor(AColor.parseColor("#0DFFFFFF"))
                setStroke(dp(1f), AColor.parseColor("#1FFFFFFF"))
            }
        }
        for ((index, nudge) in nudges.withIndex()) {
            if (index > 0) card.addView(space(12f))
            card.addView(
                label(nudge, 12.5f, "#9EFFFFFF").apply {
                    gravity = Gravity.START
                    setLineSpacing(dp(3f).toFloat(), 1f)
                }
            )
        }
        column.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        column.addView(space(18f))
        column.addView(
            label(
                "Sabab: ${verdict.reasonLabel()}",
                11f,
                "#99FF8FA3"
            )
        )

        column.addView(space(26f))
        countdownView = label("Nafas oling · $remaining", 12f, "#73FFFFFF").also {
            column.addView(it)
        }
        column.addView(space(12f))

        primary = TextView(service).apply {
            text = if (selfTest) "Sinovni yopish" else "Chiqish va orqaga qaytish"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(AColor.BLACK)
            background = GradientDrawable().apply {
                cornerRadius = dpf(50f)
                setColor(AColor.parseColor("#A9E8B8"))
            }
            isClickable = true
            setOnClickListener {
                if (remaining > 0) return@setOnClickListener
                val cb = exitCb
                hide()
                cb()
            }
        }
        column.addView(primary, LinearLayout.LayoutParams(dp(300f), dp(50f)))

        if (!selfTest) {
            column.addView(space(8f))
            secondary = TextView(service).apply {
                text = "Bu xato — davom etish"
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(AColor.parseColor("#73FFFFFF"))
                setPadding(dp(14f), dp(9f), dp(14f), dp(9f))
                isClickable = true
                setOnClickListener {
                    if (remaining > 0) return@setOnClickListener
                    val cb = falsePositiveCb
                    hide()
                    cb()
                }
            }
            column.addView(secondary)
        } else {
            secondary = null
        }

        column.addView(space(10f))
        column.addView(label("Lumirra · kontent himoyasi", 10f, "#40FFFFFF"))
    }

    private fun enableButtons(enabled: Boolean) {
        primary?.apply {
            isEnabled = enabled
            isClickable = enabled
            alpha = if (enabled) 1f else 0.38f
        }
        secondary?.apply {
            isEnabled = enabled
            isClickable = enabled
            alpha = if (enabled) 1f else 0.28f
        }
    }

    /* ==================== yordamchilar ==================== */

    private fun dpf(v: Float): Float = dp(v).toFloat()

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, service.resources.displayMetrics).toInt()

    private fun space(value: Float): View = View(service).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(value))
    }

    private fun label(text: String, size: Float, color: String, bold: Boolean = false): TextView =
        TextView(service).apply {
            this.text = text
            textSize = size
            setTextColor(AColor.parseColor(color))
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

    companion object {
        private const val COUNTDOWN_SECONDS = 5

        /** Faqat o'zini tiyishga undovchi, ayblamaydigan gaplar. */
        private val NUDGES = listOf(
            "🌬   Bir nafas oling. Impuls 30 soniyada o'tadi — qaror qoladi.",
            "🛡   Siz ekran emas, o'zingizni boshqarasiz.",
            "⏳   Keyingi 5 daqiqa — keyingi 5 yilingizdan qimmatroq.",
            "🧠   Bu hozir muhim tuyuladi. Ertaga esa — afsus.",
            "🤍   O'zingizni hurmat qiling: bu sahifa sizga mos emas.",
            "🚪   Orqaga qaytish — kuchsizlik emas, eng kuchli tanlov.",
            "🌱   Bugungi tanlovingiz ertangi sizni quradi.",
            "🔒   Siz bu himoyani o'zingiz so'ragansiz. O'sha sizga ishoning.",
            "☀️   Telefonni qo'ying, 10 soniya turing, suv iching.",
            "🎯   Diqqat — eng qimmat resursingiz. Uni behuda sarflamang."
        )
    }
}

/**
 * 18+ nishoni atrofidagi sekin "nafas oluvchi" halqa.
 * Og'ir animatsiya emas (faqat alpha + scale) — qurilma qotmaydi.
 */
private class PulseRingView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * context.resources.displayMetrics.density
        color = AColor.parseColor("#FF5A76")
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2_600L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { anim ->
            val v = anim.animatedValue as Float
            alpha = 0.46f - 0.30f * v
            val s = 0.86f + 0.14f * v
            scaleX = s
            scaleY = s
        }
    }

    init {
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        val r = minOf(width, height) / 2f - paint.strokeWidth * 2f
        canvas.drawCircle(width / 2f, height / 2f, r, paint)
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
