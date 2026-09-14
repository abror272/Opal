package com.opal.app.blocking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.opal.app.R
import com.opal.app.data.RuleSpec
import com.opal.app.data.isRuleActiveNow

/**
 * QOIDA KUZATUVCHISI (web'dagi `rule-watcher.tsx` ekvivalenti).
 * Qoida oynasi boshlanganda / tugaganda bildirishnoma chiqaradi.
 * Birinchi baholash faqat "snapshot" — spam yo'q.
 */
class RuleNotifier(private val context: Context) {

    private var lastActive: Set<String>? = null

    private fun nm(): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun channelId(): String {
        val id = "opal_rules"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = nm()
            if (mgr.getNotificationChannel(id) == null) {
                val ch = NotificationChannel(
                    id,
                    "Opal qoidalari",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Qoida boshlanishi va tugashi haqida xabar" }
                mgr.createNotificationChannel(ch)
            }
        }
        return id
    }

    private fun post(id: Int, title: String, text: String) {
        val n = NotificationCompat.Builder(context, channelId())
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            nm().notify(id, n)
        } catch (_: Throwable) {
        }
    }

    /** Har 15 sekundda chaqiriladi. */
    fun sync(rules: List<RuleSpec>, nowMinutes: Int) {
        val active = rules.filter { isRuleActiveNow(it, nowMinutes) }.map { it.id }.toSet()
        val prev = lastActive
        lastActive = active
        if (prev == null) return // birinchi snapshot — jim

        val started = active - prev
        val ended = prev - active

        for (id in started) {
            val r = rules.firstOrNull { it.id == id } ?: continue
            val detail = if (r.mode == "blockAll") "Hamma ilovalar bloklandi"
            else if (r.mode == "allow") "${r.subtitle.ifBlank { "Tanlangan ilovalar ochildi" }}"
            else "Chalg'ituvchi ilovalar bloklandi"
            post(
                id = id.hashCode() and 0xffff,
                title = "${r.icon} ${r.title} boshlandi",
                text = "$detail · ${r.start}–${r.end}"
            )
        }
        for (id in ended) {
            val r = rules.firstOrNull { it.id == id } ?: continue
            post(
                id = (id.hashCode() and 0xffff) + 1,
                title = "${r.icon} ${r.title} tugadi",
                text = "Ilovalar yana ochiq"
            )
        }
    }

    fun clear() {
        lastActive = null
    }
}
