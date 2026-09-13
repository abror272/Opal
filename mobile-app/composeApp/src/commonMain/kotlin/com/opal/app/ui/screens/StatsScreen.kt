package com.opal.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.theme.OpalColors
import com.opal.app.ui.components.AnimatedCount
import com.opal.app.ui.components.StatChip
import com.opal.app.ui.components.TrendBadge
import com.opal.app.ui.components.TrendLineChart
import com.opal.app.ui.components.WeekBarChart
import com.opal.app.ui.components.WeeklyReportCard
import com.opal.app.data.weekdayLabelUz

@Composable
fun StatsScreen() {
    val repo = remember { AppGraph.repo }
    val stats by repo.stats.collectAsState()
    val profile by repo.profile.collectAsState()
    val online by repo.online.collectAsState()

    val today = stats.today
    val todayIdx = stats.days.indexOfLast { today != null && it.date == today.date }
        .let { if (it < 0) stats.days.lastIndex else it }
    val labels = stats.days.map { weekdayLabelUz(it.date) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Statistika", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .padding(0.dp)
            ) {
                Text(
                    if (online) "🟢 Jonli" else "⚪️ Demo",
                    fontSize = 12.sp,
                    color = if (online) OpalColors.Success else OpalColors.TextTertiary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            "Oxirgi 7 kun tahlili",
            fontSize = 13.sp,
            color = OpalColors.TextSecondary,
            modifier = Modifier.padding(top = 3.dp)
        )

        Spacer(Modifier.height(16.dp))

        // ---- Xulosa chip'lari ----
        Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip("🛡️", formatMinutes(stats.weekSavedMinutes), "Hafta tejaldi", Modifier.weight(1f))
            StatChip("📱", formatMinutes(stats.weekScreenMinutes), "Hafta ekrani", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatChip("📊", formatMinutes(stats.avgDailyScreenMinutes), "O'rtacha kun", Modifier.weight(1f))
            StatChip(if (stats.trendPercent <= 0) "✅" else "⚠️", "${stats.trendPercent}%", "Trend", Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // ---- Haftalik hisobot xulosasi (web bilan bir xil) ----
        WeeklyReportCard(stats = stats, profile = profile)

        Spacer(Modifier.height(18.dp))

        // ---- Bugungi maqsad ----
        GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 16.dp) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Bugun", fontSize = 14.sp, color = OpalColors.TextSecondary)
                    AnimatedCount(
                        value = today?.savedMinutes ?: 0,
                        suffix = " daqiqa tejaldi",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpalColors.TextPrimary
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                TrendBadge(stats.trendPercent)
            }
            if (today != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Ekran vaqti: ${formatMinutes(today.screenTimeMinutes)} • Olishlar: ${today.pickups}",
                    fontSize = 12.sp,
                    color = OpalColors.TextTertiary
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- Bar chart ----
        GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 16.dp) {
            Column {
                Text("Ekran vaqti (7 kun)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                Text(
                    "Maqsad: ${formatMinutes(stats.goalMinutes)} / kun",
                    fontSize = 11.5.sp,
                    color = OpalColors.TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(14.dp))
                WeekBarChart(
                    values = stats.days.map { it.screenTimeMinutes },
                    labels = labels,
                    todayIndex = todayIdx
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- Line chart ----
        GlassCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = 16.dp) {
            Column {
                Text("Tejalgan vaqt tendensiyasi", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                Text(
                    "Fokus sessiyalari natijasi",
                    fontSize = 11.5.sp,
                    color = OpalColors.TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(14.dp))
                TrendLineChart(
                    values = stats.days.map { it.savedMinutes },
                    labels = labels
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}
