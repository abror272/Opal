package com.opal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.formatMinutes
import com.opal.app.data.weekdayLabelUz
import com.opal.app.glass.GlassCard
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.AnimatedCount
import com.opal.app.ui.components.StatChip
import com.opal.app.ui.components.TrendBadge
import com.opal.app.ui.components.TrendLineChart
import com.opal.app.ui.components.WeekBarChart
import com.opal.app.ui.components.WeeklyReportCard

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
            .padding(horizontal = OpalSpacing.xl)
    ) {
        Spacer(Modifier.height(OpalSpacing.sm))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Statistika", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary, letterSpacing = (-0.3).sp)
            LivePill(online)
        }
        Text(
            "Oxirgi 7 kun tahlili",
            fontSize = 13.sp,
            color = OpalColors.TextSecondary,
            modifier = Modifier.padding(top = 3.dp)
        )

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Xulosa chip'lari ----
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
            StatChip(OpalIcon.Shield, formatMinutes(stats.weekSavedMinutes), "Hafta tejaldi", Modifier.weight(1f), OpalColors.Success)
            StatChip(OpalIcon.Phone, formatMinutes(stats.weekScreenMinutes), "Hafta ekrani", Modifier.weight(1f), OpalColors.Accent)
        }
        Spacer(Modifier.height(OpalSpacing.sm))
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
            StatChip(OpalIcon.Chart, formatMinutes(stats.avgDailyScreenMinutes), "O'rtacha kun", Modifier.weight(1f), OpalColors.MintLight)
            StatChip(
                if (stats.trendPercent <= 0) OpalIcon.TrendDown else OpalIcon.TrendUp,
                "${stats.trendPercent}%",
                "Trend",
                Modifier.weight(1f),
                if (stats.trendPercent <= 0) OpalColors.Success else OpalColors.Danger
            )
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Haftalik hisobot xulosasi ----
        WeeklyReportCard(stats = stats, profile = profile)

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Bugungi maqsad ----
        GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 16.dp) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Bugun", fontSize = 13.sp, color = OpalColors.TextSecondary)
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
                Spacer(Modifier.height(OpalSpacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OpalIcons(OpalIcon.Clock, OpalColors.TextTertiary, Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Ekran vaqti: ${formatMinutes(today.screenTimeMinutes)}  •  Olishlar: ${today.pickups}",
                        fontSize = 12.sp,
                        color = OpalColors.TextTertiary
                    )
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Bar chart ----
        GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 16.dp) {
            Column {
                Text("Ekran vaqti (7 kun)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                Text(
                    "Maqsad: ${formatMinutes(stats.goalMinutes)} / kun",
                    fontSize = 11.5.sp,
                    color = OpalColors.TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(OpalSpacing.md))
                WeekBarChart(
                    values = stats.days.map { it.screenTimeMinutes },
                    labels = labels,
                    todayIndex = todayIdx
                )
            }
        }

        Spacer(Modifier.height(OpalSpacing.md))

        // ---- Line chart ----
        GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 16.dp) {
            Column {
                Text("Tejalgan vaqt tendensiyasi", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                Text(
                    "Fokus sessiyalari natijasi",
                    fontSize = 11.5.sp,
                    color = OpalColors.TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(OpalSpacing.md))
                TrendLineChart(
                    values = stats.days.map { it.savedMinutes },
                    labels = labels
                )
            }
        }

        Spacer(Modifier.height(OpalSpacing.xxxl))
    }
}

@Composable
private fun LivePill(online: Boolean) {
    val color = if (online) OpalColors.Success else OpalColors.TextTertiary
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(
                if (online) "Jonli" else "Demo",
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
