package com.opal.app.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color as AColor
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.random.Random

/**
 * To'liq ekranli blok oynasi. Ikkita rejimda ishlaydi:
 *  - [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] — accessibility xizmati uchun
 *  - [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY] — "boshqa ilovalar ustida" ruxsati bilan
 * Fon-aktivlik cheklovlari (MIUI / Android 10+) bunga ta'sir qilmaydi.
 */
class BlockOverlay(
    private val service: Context,
    private val windowType: Int = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
) {

    private var host: FrameLayout? = null

    private var currentPkg: String = ""
    private var currentLabel: String = "Ilova"
    private var currentIcon: Drawable? = null
    private var dismissCb: () -> Unit = {}
    private var allowCb: () -> Unit = {}
    private var reasonTitle: String = ""
    private var reasonDetail: String = ""
    private var strictMode: Boolean = false

    var blockedPackage: String? = null
        private set

    val isShowing: Boolean get() = host != null

    private fun dpf(v: Float): Float = dp(v).toFloat()

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, service.resources.displayMetrics).toInt()

    private fun wm(): WindowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun show(
        packageName: String,
        label: String,
        icon: Drawable?,
        reasonTitle: String,
        reasonDetail: String,
        strict: Boolean,
        onDismiss: () -> Unit,
        onAllow: () -> Unit
    ) {
        currentPkg = packageName
        currentLabel = label
        currentIcon = icon
        this.reasonTitle = reasonTitle
        this.reasonDetail = reasonDetail
        strictMode = strict
        dismissCb = onDismiss
        allowCb = onAllow
        blockedPackage = packageName

        val root = host ?: createHost() ?: return
        renderBlock(root)
    }

    fun hide() {
        val h = host ?: return
        try {
            wm().removeView(h)
        } catch (_: Throwable) {
        }
        host = null
        blockedPackage = null
    }

    private fun createHost(): FrameLayout? {
        val root = FrameLayout(service).apply {
            setBackgroundColor(AColor.parseColor("#020204"))
            isClickable = true
        }
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        return try {
            wm().addView(root, p)
            host = root
            root
        } catch (_: Throwable) {
            null
        }
    }

    /* ==================== Blok ekrani ==================== */

    private fun renderBlock(root: FrameLayout) {
        root.removeAllViews()

        val column = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        )

        // ilova belgisi
        val iconBox = FrameLayout(service).apply {
            background = GradientDrawable().apply {
                cornerRadius = dpf(22f)
                setColor(AColor.parseColor("#14FFFFFF"))
                setStroke(dp(1f), AColor.parseColor("#1FFFFFFF"))
            }
        }
        if (currentIcon != null) {
            iconBox.addView(
                ImageView(service).apply { setImageDrawable(currentIcon) },
                FrameLayout.LayoutParams(dp(58f), dp(58f)).apply { gravity = Gravity.CENTER }
            )
        }
        column.addView(iconBox, LinearLayout.LayoutParams(dp(78f), dp(78f)))

        column.addView(space(28f))
        column.addView(text(currentLabel, 24f, true, "#FFFFFF"))
        column.addView(text("Opal tomonidan bloklandi", 24f, true, "#F2FFFFFF"))
        column.addView(space(14f))

        // Sabab chipi (qoida nomi yoki "Bloklangan ilova")
        if (reasonTitle.isNotEmpty()) {
            column.addView(
                text(reasonTitle, 11.5f, true, "#9FE8B5").apply {
                    background = GradientDrawable().apply {
                        cornerRadius = dpf(50f)
                        setColor(AColor.parseColor("#1F86EFAC"))
                        setStroke(dp(1f), AColor.parseColor("#40A7F3D0"))
                    }
                    setPadding(dp(14f), dp(7f), dp(14f), dp(7f))
                }
            )
            column.addView(space(10f))
        }

        val witty = WITTY[currentLabel.length % WITTY.size]
        column.addView(
            text(witty, 13f, false, "#73FFFFFF").apply {
                setLineSpacing(dp(4f).toFloat(), 1f)
                setPadding(dp(34f), 0, dp(34f), 0)
            }
        )

        if (reasonDetail.isNotEmpty()) {
            column.addView(space(8f))
            column.addView(text(reasonDetail, 11f, false, "#5CFFFFFF"))
        }

        column.addView(space(46f))

        val close = pill("Yopish", filled = true)
        close.setOnClickListener { dismissCb() }
        column.addView(close, LinearLayout.LayoutParams(dp(250f), dp(52f)))

        if (!strictMode) {
            column.addView(space(14f))
            val allow = text("Baribir ochish · 🧮 Battle Math", 11.5f, true, "#59FFFFFF").apply {
                setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
                setOnClickListener { renderMath(root) }
            }
            column.addView(allow)
        } else {
            column.addView(space(14f))
            column.addView(text("Qat'iy rejim — ochish yo'q", 11f, true, "#7AFFB4B4"))
        }
    }

    /* ==================== Battle Math ==================== */

    private fun renderMath(root: FrameLayout) {
        root.removeAllViews()

        val column = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20f), dp(24f), dp(20f), dp(24f))
        }
        root.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        )

        val rnd = Random(System.currentTimeMillis())
        val questions = Array(3) { MathQ(6 + rnd.nextInt(5), 5 + rnd.nextInt(6)) }
        val values = arrayOf("", "", "")
        var focus = 0
        var solved = false

        val status = text("Barcha masalalarni yeching:", 12.5f, true, "#8CFFFFFF")
        column.addView(status)
        column.addView(space(18f))

        val boxes = arrayOfNulls<TextView>(3)
        for (i in 0..2) {
            val row = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.addView(text("${questions[i].a} × ${questions[i].b} =", 21f, true, "#E6FFFFFF"))
            row.addView(spaceH(12f))
            val box = text("", 19f, true, "#BFE9FF").apply {
                typeface = Typeface.MONOSPACE
                background = GradientDrawable().apply {
                    cornerRadius = dpf(12f)
                    setColor(AColor.parseColor("#0F5EEAD4"))
                    setStroke(dp(2f), AColor.parseColor("#6686EFAC"))
                }
            }
            boxes[i] = box
            row.addView(box, LinearLayout.LayoutParams(dp(86f), dp(44f)))
            column.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(14f) }
            )
        }

        column.addView(space(26f))

        val keypadHolder = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
        column.addView(keypadHolder)

        fun refresh() {
            for (i in 0..2) boxes[i]?.text = values[i]
        }

        fun reset() {
            for (i in 0..2) {
                values[i] = ""
                questions[i] = MathQ(6 + rnd.nextInt(5), 5 + rnd.nextInt(6))
            }
            focus = 0
            refresh()
        }

        fun check() {
            if (solved) return
            // Faqat har bir katak o'z javobining uzunligiga yetganda tekshiramiz.
            val complete = (0..2).all { values[it].length >= questions[it].answer.toString().length }
            if (!complete) return
            val ok = (0..2).all { values[it].toIntOrNull() == questions[it].answer }
            if (ok) {
                solved = true
                status.text = "✓ $currentLabel ochildi!"
                status.setTextColor(AColor.parseColor("#6EE7B7"))
                root.postDelayed({ allowCb() }, 700)
            } else {
                status.text = "Noto'g'ri — yana urinib ko'ring"
                status.setTextColor(AColor.parseColor("#FF8FA3"))
                root.postDelayed({ reset() }, 750)
            }
        }

        fun press(k: String) {
            if (solved) return
            if (k == "del") {
                values[focus] = values[focus].dropLast(1)
            } else {
                if (values[focus].length >= 3) return
                values[focus] += k
                if (values[focus].length >= questions[focus].answer.toString().length && focus < 2) focus++
            }
            refresh()
            android.util.Log.d("OpalBlock", "press=$k values=${values.toList()} focus=$focus")
            check()
        }

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        keys.chunked(3).forEach { rowKeys ->
            val row = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
            rowKeys.forEach { k ->
                if (k.isEmpty()) {
                    row.addView(View(service), LinearLayout.LayoutParams(0, dp(52f), 1f).apply {
                        rightMargin = dp(5f)
                        bottomMargin = dp(6f)
                    })
                } else {
                    val btn = text(k, 21f, true, if (k == "⌫") "#9FD8FF" else "#FFFFFF").apply {
                        background = GradientDrawable().apply {
                            cornerRadius = dpf(16f)
                            setColor(AColor.parseColor("#0EFFFFFF"))
                            setStroke(dp(1f), AColor.parseColor("#1AFFFFFF"))
                        }
                        isClickable = true
                        setOnClickListener { press(k) }
                    }
                    row.addView(btn, LinearLayout.LayoutParams(0, dp(52f), 1f).apply {
                        rightMargin = dp(5f)
                        bottomMargin = dp(6f)
                    })
                }
            }
            keypadHolder.addView(row)
        }

        val back = text("← Orqaga", 12f, true, "#73FFFFFF").apply {
            setPadding(dp(8f), dp(16f), dp(8f), dp(4f))
            setOnClickListener { renderBlock(root) }
        }
        keypadHolder.addView(back)

        refresh()
    }

    private data class MathQ(val a: Int, val b: Int) {
        val answer: Int get() = a * b
    }

    /* ==================== yordamchilar ==================== */

    private fun space(dpVal: Float) = View(service).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(dpVal))
    }

    private fun spaceH(dpVal: Float) = View(service).apply {
        layoutParams = LinearLayout.LayoutParams(dp(dpVal), 1)
    }

    private fun text(value: String, size: Float, bold: Boolean, color: String): TextView =
        TextView(service).apply {
            text = value
            textSize = size
            setTextColor(AColor.parseColor(color))
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

    private fun pill(label: String, filled: Boolean): TextView =
        TextView(service).apply {
            text = label
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (filled) AColor.BLACK else AColor.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dpf(50f)
                if (filled) {
                    setColor(AColor.WHITE)
                } else {
                    setColor(AColor.parseColor("#14FFFFFF"))
                    setStroke(dp(1f), AColor.parseColor("#33FFFFFF"))
                }
            }
            isClickable = true
        }

    companion object {
        private val WITTY = listOf(
            "Siz bu qoidani kecha o'rnatgansiz. O'tgan siz to'g'ri edi.",
            "Kelajakdagi sizga minnatdorchilik bildiradi.",
            "Bu ilova sizning eng yaxshi versiyangizga yo'l to'sqinlik qiladi.",
            "Bir oz tinchlanish — eng yaxshi tanlov."
        )
    }
}
