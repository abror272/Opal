package com.opal.app.blocking

import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * Ekrandagi matnni AccessibilityNodeInfo daraxtidan yig'adi.
 *
 * MUHIM: bu kod HAR BIR content-change eventida ishlamaydi — xizmat tomonda
 * debounce + throttle bor. Bu yerda esa qat'iy byudjet:
 *  - 420 tugun (node)
 *  - 6000 belgi
 *  - 14 ms vaqt
 * Shuning uchun UI qotmaydi (og'ir sahifalarda ham).
 */
internal object ScreenTextScanner {

    private const val MAX_NODES = 420
    private const val MAX_CHARS = 6_000
    private const val BUDGET_MS = 14L
    private const val MAX_DEPTH = 20

    fun scan(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val out = StringBuilder(384)
        val nodes = ArrayDeque<AccessibilityNodeInfo>()
        val depths = ArrayDeque<Int>()
        nodes.add(root)
        depths.add(0)

        val started = SystemClock.uptimeMillis()
        var visited = 0

        while (nodes.isNotEmpty() && visited < MAX_NODES) {
            // Har 32 tugunda vaqtni tekshiramiz — SystemClock arzon emas.
            if ((visited and 31) == 0 && SystemClock.uptimeMillis() - started > BUDGET_MS) break

            val node = nodes.removeFirst()
            val depth = depths.removeFirst()
            visited++

            append(out, node.text)
            append(out, node.contentDescription)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) append(out, node.hintText)

            if (out.length >= MAX_CHARS) break
            if (depth >= MAX_DEPTH) continue

            val count = try {
                node.childCount
            } catch (_: Throwable) {
                0
            }
            for (i in 0 until count) {
                val child = try {
                    node.getChild(i)
                } catch (_: Throwable) {
                    null
                } ?: continue
                nodes.add(child)
                depths.add(depth + 1)
            }
        }
        return out.toString()
    }

    private fun append(out: StringBuilder, value: CharSequence?) {
        if (value.isNullOrBlank()) return
        if (out.length + value.length + 1 > MAX_CHARS) return
        out.append(value).append(' ')
    }
}
