package com.opal.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.DailyStatDto
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassPane
import com.opal.app.theme.OpalColors
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

private val MONTHS_UZ = listOf("Yan", "Fev", "Mar", "Apr", "May", "Iyn", "Iyl", "Avg", "Sen", "Okt", "Noy", "Dek")
private val WEEKDAY_ROW = mapOf(0 to "Du", 2 to "Ch", 4 to "Ju")

private fun levelOf(saved: Int, sessions: Int = 0): Int = when {
    saved <= 0 && sessions <= 0 -> 0
    saved < 45 -> 1
    saved < 90 -> 2
    saved < 150 -> 3
    else -> 4
}

private val LEVEL_COLORS = listOf(
    Color(0x0EFFFFFF),
    Color(0x3886EFAC),
    Color(0x7386EFAC),
    Color(0xB386EFAC),
    Color(0xFF86EFAC)
)

private val LEVEL_NAMES = listOf("Fokus yo'q", "Yengil kun", "O'rtacha kun", "Yaxshi kun", "Zo'r kun!")

private data class HeatCell(
    val date: LocalDate,
    val stat: DailyStatDto?,
    val isFuture: Boolean,
    val isToday: Boolean
) {
    val sessions: Int get() = stat?.sessions ?: 0
    val saved: Int get() = stat?.savedMinutes ?: 0
    val active: Boolean get() = sessions > 0 || saved > 0
    val label: String get() = "${date.dayOfMonth} ${MONTHS_UZ[date.monthNumber - 1]}"
}

/** GitHub-uslubidagi 5 haftalik izchillik xaritasi (Opal mint tilida). */
@Composable
fun ConsistencyHeatmap(days: List<DailyStatDto>, modifier: Modifier = Modifier) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val byDate = remember(days) { days.associateBy { it.date } }

    val cells = remember(days, today) {
        val start = today.minus(DatePeriod(days = 34))
        val backToMonday = start.dayOfWeek.isoDayNumber - 1
        val gridStart = start.minus(DatePeriod(days = backToMonday))
        val total = 35 + backToMonday
        val weeks = (total + 6) / 7
        (0 until weeks * 7).map { i ->
            val d = gridStart.plus(DatePeriod(days = i))
            HeatCell(
                date = d,
                stat = byDate[d.toString()],
                isFuture = d > today,
                isToday = d == today
            )
        }
    }

    val weeks = cells.size / 7
    val monthLabels = remember(cells) {
        val out = HashMap<Int, String>()
        var lastMonth = -1
        cells.forEachIndexed { i, c ->
            if (c.isFuture || c.date.dayOfWeek.isoDayNumber != 1) return@forEachIndexed
            val m = c.date.monthNumber
            if (m != lastMonth) {
                out[i / 7] = MONTHS_UZ[m - 1]
                lastMonth = m
            }
        }
        out
    }

    val activeDays = cells.count { !it.isFuture && it.active }
    val totalSaved = cells.filter { !it.isFuture }.sumOf { it.saved }

    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val selectedCell = selected?.let { d -> cells.firstOrNull { it.date == d } }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Izchillik", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
            Text(
                "$activeDays/35 kun · ${formatMinutes(totalSaved)}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = OpalColors.TextTertiary
            )
        }
        Spacer(Modifier.height(12.dp))

        GlassPane(Modifier.fillMaxWidth(), radius = 20.dp, base = 0.05f) {
            Column(Modifier.padding(14.dp)) {
                // oy yorliqlari
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(26.dp))
                    (0 until weeks).forEach {
                        Box(Modifier.weight(1f)) {
                            Text(
                                monthLabels[it] ?: "",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpalColors.TextTertiary,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))

                Row(Modifier.fillMaxWidth()) {
                    // hafta kunlari
                    Column(Modifier.width(26.dp), verticalArrangement = Arrangement.spacedBy(4.5.dp)) {
                        (0..6).forEach { r ->
                            Box(Modifier.height(13.dp), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    WEEKDAY_ROW[r] ?: "",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OpalColors.TextTertiary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(5.dp))
                    // katakchalar
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.5.dp)) {
                        (0 until weeks).forEach { w ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.5.dp)) {
                                (0..6).forEach { r ->
                                    val c = cells[w * 7 + r]
                                    if (c.isFuture) {
                                        Spacer(Modifier.height(13.dp))
                                    } else {
                                        val isSel = selected == c.date
                                        val lvl = levelOf(c.saved, c.sessions)
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(13.dp)
                                                .clip(RoundedCornerShape(3.5.dp))
                                                .background(LEVEL_COLORS[lvl])
                                                .then(
                                                    when {
                                                        isSel -> Modifier.border(1.5.dp, Color.White, RoundedCornerShape(3.5.dp))
                                                        c.isToday -> Modifier.border(1.dp, Color(0xFFE6FFF0), RoundedCornerShape(3.5.dp))
                                                        else -> Modifier
                                                    }
                                                )
                                                .clickable { selected = if (isSel) null else c.date }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // tanlangan kun
                AnimatedVisibility(
                    visible = selectedCell != null,
                    enter = fadeIn(tween(200)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                ) {
                    selectedCell?.let { c ->
                        val stat = c.stat
                        Column(
                            Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(listOf(Color(0x1A86EFAC), Color(0x05FFFFFF)))
                                )
                                .border(1.dp, Color(0x3886EFAC), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${c.date.dayOfMonth} ${MONTHS_UZ[c.date.monthNumber - 1]}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (levelOf(c.saved, c.sessions) >= 3) Color(0x2686EFAC) else Color(0x0FFFFFFF)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        LEVEL_NAMES[levelOf(c.saved, c.sessions)],
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (levelOf(c.saved, c.sessions) >= 3) OpalColors.MintLight else OpalColors.TextTertiary
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DayMetric("Tejaldi", formatMinutes(c.saved), true, Modifier.weight(1f))
                                DayMetric("Ekran vaqti", formatMinutes(stat?.screenTimeMinutes ?: 0), false, Modifier.weight(1f))
                                DayMetric("Olishlar", "${stat?.pickups ?: 0}", false, Modifier.weight(1f))
                            }
                            val goal = stat?.goalMinutes ?: 0
                            val screen = stat?.screenTimeMinutes ?: 0
                            if (goal > 0) {
                                Spacer(Modifier.height(10.dp))
                                val over = screen > goal
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "Kunlik maqsad (${goal / 60}h ekran)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OpalColors.TextTertiary
                                    )
                                    Text(
                                        if (over) "+${formatMinutes(screen - goal)} ortiq" else "✓ maqsadda",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (over) Color(0xFFFFD27A) else OpalColors.MintLight
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                val frac by animateFloatAsState(
                                    (screen.toFloat() / goal).coerceIn(0f, 1f),
                                    tween(600),
                                    label = "goal"
                                )
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(frac)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (over) Brush.horizontalGradient(listOf(Color(0xFFFFD27A), Color(0xFFFF9A62)))
                                                else Brush.horizontalGradient(listOf(Color(0xFF86EFAC), Color(0xFF5EEAD4)))
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                // legenda
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kam", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextTertiary)
                    Spacer(Modifier.width(6.dp))
                    LEVEL_COLORS.forEach {
                        Box(
                            Modifier
                                .padding(start = 3.dp)
                                .width(10.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(it)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Ko'p", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayMetric(label: String, value: String, accent: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (accent) Color(0xFFB7F5CD) else Color.White
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label.uppercase(),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = OpalColors.TextTertiary,
            letterSpacing = 0.6.sp
        )
    }
}
