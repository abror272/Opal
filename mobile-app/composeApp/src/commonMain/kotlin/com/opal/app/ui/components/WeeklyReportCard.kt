package com.opal.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.StatsResponseDto
import com.opal.app.data.UserProfileDto
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.theme.OpalColors
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import androidx.compose.foundation.background

/**
 * HAFTALIK HISOBOT kartasi — web ilovadagi "Weekly Report" imzosining KMP nusxasi.
 * Haftada tejalgan vaqt + trend + eng yaxshi kun + streak.
 * (Web'dagi PNG eksport platformaga bog'liq — mobile'da xulosa ko'rinish beriladi.)
 */

private val DOW_UZ = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sha", "Ya")

@Composable
fun WeeklyReportCard(
    stats: StatsResponseDto,
    profile: UserProfileDto,
    modifier: Modifier = Modifier
) {
    val days = stats.days
    val savedValues = days.map { it.savedMinutes }
    val weekSaved = savedValues.sum()
    val bestIdx = savedValues.indices.maxByOrNull { savedValues[it] } ?: 0
    val todayIdx = days.indexOfLast { it.date == stats.today?.date }.let { if (it < 0) days.lastIndex else it }
    val rangeLabel = remember(days) {
        if (days.isEmpty()) "" else {
            val first = days.first().date.takeLast(5).replace("-", ".")
            val last = days.last().date.takeLast(5).replace("-", ".")
            "$first – $last"
        }
    }

    GlassCard(modifier.fillMaxWidth(), radius = 28.dp, padding = 18.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "HAFTALIK HISOBOT",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.Success,
                    letterSpacing = 1.4.sp
                )
                Text(
                    rangeLabel,
                    fontSize = 10.5.sp,
                    color = OpalColors.TextTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatMinutes(weekSaved),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OpalColors.MintLight
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "tejaldi",
                    fontSize = 13.sp,
                    color = OpalColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
                Spacer(Modifier.weight(1f))
                TrendBadge(stats.trendPercent)
            }

            Spacer(Modifier.height(14.dp))
            WeekBarChart(
                values = savedValues,
                labels = days.mapIndexed { i, d -> if (i == bestIdx) "★" else DOW_UZ.getOrElse(i) { "" } },
                todayIndex = todayIdx,
                chartHeight = 96.dp
            )

            Spacer(Modifier.height(14.dp))

            // eng yaxshi kun qatori
            if (savedValues.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.045f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OpalIcons(OpalIcon.Trophy, OpalColors.Amber, Modifier.size(15.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Eng yaxshi kun — ${formatMinutes(savedValues[bestIdx])}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OpalColors.Amber,
                        modifier = Modifier.weight(1f)
                    )
                    OpalIcons(OpalIcon.Flame, OpalColors.Amber, Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${profile.streakDays} kun",
                        fontSize = 11.sp,
                        color = OpalColors.TextSecondary
                    )
                }
            }
        }
    }
}
