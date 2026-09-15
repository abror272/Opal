package com.opal.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * HAQIQIY foydalanish statistikasi — Android UsageStats'dan.
 * Demo/simulyatsiya yo'q: hamma raqam qurilmadan olinadi.
 */

private const val EVENT_FOREGROUND = 1 // MOVE_TO_FOREGROUND / ACTIVITY_RESUMED

private fun usm(): UsageStatsManager? = try {
    opalContext().getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
} catch (_: Throwable) {
    null
}

/** Hisobga olinmaydigan tizim paketlari. */
private val IGNORED = setOf(
    "com.opal.app",
    "com.miui.home",
    "com.android.systemui",
    "com.miui.securitycenter",
    "com.android.settings",
    "com.google.android.inputmethod.latin",
    "com.google.android.gms",
    "com.android.vending",
    "com.miui.aod",
    "com.android.permissioncontroller",
    "com.google.android.permissioncontroller",
    "com.miui.securityadd",
    "com.android.providers.settings"
)

private fun sdf(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun startOfDay(offsetDays: Int): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -offsetDays)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun endOfDay(offsetDays: Int): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -offsetDays)
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

/** Oxirgi [days] kun uchun haqiqiy ekran vaqti (daqiqa). Kalit: yyyy-MM-dd. */
actual fun realScreenTimeByDay(days: Int): Map<String, Int> {
    val u = usm() ?: return emptyMap()
    val out = LinkedHashMap<String, Int>()
    val label = sdf()
    for (i in (days - 1).downTo(0)) {
        val start = startOfDay(i)
        val end = minOf(endOfDay(i), System.currentTimeMillis())
        if (end <= start) {
            out[label.format(Date(start))] = 0
            continue
        }
        var totalMs = 0L
        try {
            val stats = u.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            stats?.forEach { s ->
                if (s.packageName == null || IGNORED.contains(s.packageName)) return@forEach
                totalMs += s.totalTimeInForeground
            }
        } catch (_: Throwable) {
        }
        out[label.format(Date(start))] = (totalMs / 60_000L).toInt()
    }
    return out
}

/** Bugungi haqiqiy ekran vaqti (daqiqa). */
actual fun realScreenTimeToday(): Int = realScreenTimeByDay(1).values.firstOrNull() ?: 0

/** Bugungi haqiqiy ilova ochishlar (pickup) soni. */
actual fun realPickupsToday(): Int {
    val u = usm() ?: return 0
    val start = startOfDay(0)
    val now = System.currentTimeMillis()
    var count = 0
    var lastPkg: String? = null
    try {
        val events = u.queryEvents(start, now)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType != EVENT_FOREGROUND) continue
            val pkg = e.packageName ?: continue
            if (IGNORED.contains(pkg)) {
                lastPkg = pkg
                continue
            }
            if (pkg != lastPkg) count++
            lastPkg = pkg
        }
    } catch (_: Throwable) {
    }
    return count
}

/** Bugun eng ko'p ishlatilgan ilovalar (paket → daqiqa), kamayish tartibida. */
fun realTodayUsageByApp(): List<Pair<String, Int>> {
    val u = usm() ?: return emptyList()
    val start = startOfDay(0)
    val now = System.currentTimeMillis()
    return try {
        u.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
            ?.filter { !IGNORED.contains(it.packageName) && it.totalTimeInForeground > 60_000L }
            ?.map { it.packageName to (it.totalTimeInForeground / 60_000L).toInt() }
            ?.sortedByDescending { it.second }
            ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}

/** Hozir ekranda turgan ilova paketi (UsageEvents'dan). */
fun currentForegroundPackage(): String? {
    val u = usm() ?: return null
    val now = System.currentTimeMillis()
    var last: String? = null
    try {
        val events = u.queryEvents(now - 90_000L, now)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType != EVENT_FOREGROUND) continue
            val pkg = e.packageName ?: continue
            last = pkg
        }
    } catch (_: Throwable) {
    }
    return last
}
