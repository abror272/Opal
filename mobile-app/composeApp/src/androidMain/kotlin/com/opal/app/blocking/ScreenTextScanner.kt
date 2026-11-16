package com.opal.app.blocking

import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** Ekrandan olingan ko'rinadigan matn + manzil satri. */
internal data class ScreenSnapshot(
    val text: String,
    val addressBar: String
)

/**
 * Ekrandagi KO'RINADIGAN matnni yig'adi.
 *
 * Muhim farqlar (aniqlik uchun):
 *  - faqat `isVisibleToUser` tugunlar (RecyclerView'dagi ko'rinmayotgan
 *    elementlar hisobga olinmaydi — YouTube'da yolg'on bloklarning asosiy sababi shu edi);
 *  - manzil satri alohida ajratiladi (faqat yuqoridagi EditText/url maydoni);
 *  - qat'iy byudjet: 300 tugun / 5000 belgi / 12 ms.
 */
internal object ScreenTextScanner {

    private const val MAX_NODES = 800
    private const val MAX_CHARS = 8_000
    private const val BUDGET_MS = 24L
    private const val MAX_DEPTH = 28

    private val URL_RE = Regex("""^(https?://)?[a-z0-9.-]+\.[a-z]{2,24}(/\S*)?$""", RegexOption.IGNORE_CASE)

    fun scan(root: AccessibilityNodeInfo?, screenHeight: Int): ScreenSnapshot {
        if (root == null) return ScreenSnapshot("", "")
        val body = StringBuilder(384)
        val address = StringBuilder(0)
        val nodes = ArrayDeque<AccessibilityNodeInfo>()
        val depths = ArrayDeque<Int>()
        val bounds = Rect()
        nodes.add(root)
        depths.add(0)

        val started = SystemClock.uptimeMillis()
        var visited = 0

        while (nodes.isNotEmpty() && visited < MAX_NODES) {
            if ((visited and 31) == 0 && SystemClock.uptimeMillis() - started > BUDGET_MS) break

            val node = nodes.removeFirst()
            val depth = depths.removeFirst()
            visited++

            val visible = try {
                node.isVisibleToUser
            } catch (_: Throwable) {
                false
            }
            if (!visible) continue

            val text = firstText(node)
            if (!text.isNullOrBlank()) {
                if (body.length + text.length + 1 <= MAX_CHARS) body.append(text).append(' ')
                if (isAddressBar(node, text, screenHeight, bounds)) {
                    if (address.length + text.length + 1 <= 400) address.append(text).append(' ')
                }
            }

            if (body.length >= MAX_CHARS) break
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
        return ScreenSnapshot(body.toString(), address.toString().trim())
    }

    private fun firstText(node: AccessibilityNodeInfo): String? {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun isAddressBar(
        node: AccessibilityNodeInfo,
        text: String,
        screenHeight: Int,
        bounds: Rect
    ): Boolean {
        if (screenHeight <= 0) return false
        if (text.length > 300 || text.contains('\n')) return false
        try {
            node.getBoundsInScreen(bounds)
        } catch (_: Throwable) {
            return false
        }
        // Manzil satri ekranning yuqori qismida bo'ladi.
        if (bounds.top < 0 || bounds.top > screenHeight * 0.24) return false

        val cls = node.className?.toString().orEmpty()
        val viewId = node.viewIdResourceName.orEmpty()
        val looksLikeUrl = URL_RE.matches(text.trim()) ||
            text.startsWith("http://") || text.startsWith("https://")

        val isEditable = cls.contains("EditText", ignoreCase = true) ||
            cls.contains("AutoCompleteTextView", ignoreCase = true) ||
            viewId.contains("url", ignoreCase = true) ||
            viewId.contains("address", ignoreCase = true) ||
            viewId.contains("omnibox", ignoreCase = true)

        // EditText/URL maydoni bo'lsa — manzil satri. Oddiy sarlavha (TextView) hisobga olinmaydi.
        return isEditable && (looksLikeUrl || text.contains('.'))
    }
}
