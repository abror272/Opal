package com.opal.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.computeScores
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.glass.Pressable
import com.opal.app.platform.rememberSafePadding
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.theme.OpalSweep
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.AnimatedCount
import com.opal.app.ui.components.StatRingPill

@Composable
fun StatsScreen(onClose: () -> Unit) {
    val repo = remember { AppGraph.repo }
    val insets = rememberSafePadding()
    val stats by repo.stats.collectAsState()
    val profile by repo.profile.collectAsState()
    val sessions by repo.sessions.collectAsState()

    val scores = remember(profile, stats, sessions) { computeScores(profile, stats, sessions) }
    val today = stats.today

    Box(
        Modifier
            .fillMaxSize()
            .background(OpalColors.Bg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OpalSpacing.xl)
        ) {
            Spacer(Modifier.height(insets.calculateTopPadding() + OpalSpacing.md))

            // ---- Header ----
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundCloseButton(onClose)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2039", fontSize = 22.sp, color = OpalColors.TextTertiary)
                    Text(
                        "Bugun",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpalColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text("\u203A", fontSize = 22.sp, color = OpalColors.TextTertiary)
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(42.dp))
            }

            Spacer(Modifier.height(OpalSpacing.xl))

            // ---- Arc gauge ----
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ArcScoreGauge(
                    score = scores.overall,
                    improving = scores.improving,
                    modifier = Modifier.fillMaxWidth().height(170.dp)
                )
            }

            Spacer(Modifier.height(OpalSpacing.sm))

            // ---- 3 pills ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
                StatRingPill(OpalIcon.Moon, scores.sleep, "Sleep", scores.sleep / 100f, Modifier.weight(1f))
                StatRingPill(OpalIcon.Hourglass, scores.focus, "Focus", scores.focus / 100f, Modifier.weight(1f))
                StatRingPill(OpalIcon.Plant, scores.rest, "Rest", scores.rest / 100f, Modifier.weight(1f))
            }

            Spacer(Modifier.height(OpalSpacing.xxl))

            // ---- Info ----
            Text("Sleep Score nima?", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
            Text(
                "Uyqu ko'rsatkichi kechqurun, uyqu paytida va ertalab sodir bo'ladigan holatlarni va ularning keyingi kuningizga ta'sirini o'lchaydi.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = OpalColors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(OpalSpacing.xl))

            // ---- Detail rows ----
            val sleepMinutes = sessions.filter { it.type == "SLEEP" }.sumOf { it.durationMinutes }
            DetailRow(
                left = "Uyqu",
                value = if (sleepMinutes > 0) formatMinutes(sleepMinutes) else "0d",
                status = if (sleepMinutes >= 360) "Yaxshi" else "Qisqa",
                statusColor = if (sleepMinutes >= 360) OpalColors.Success else OpalColors.Danger,
                fraction = (sleepMinutes / 480f).coerceIn(0.04f, 1f),
                barColor = if (sleepMinutes >= 360) OpalColors.Success else OpalColors.Danger
            )
            Spacer(Modifier.height(OpalSpacing.xl))
            DetailRow(
                left = "Ekran vaqti",
                value = formatMinutes(today?.screenTimeMinutes ?: 0),
                status = if ((today?.screenTimeMinutes ?: 0) <= stats.goalMinutes) "Ajoyib" else "Ko'p",
                statusColor = if ((today?.screenTimeMinutes ?: 0) <= stats.goalMinutes) OpalColors.Success else OpalColors.Amber,
                fraction = ((today?.screenTimeMinutes ?: 0) / (stats.goalMinutes.coerceAtLeast(1) * 2f)).coerceIn(0.04f, 1f),
                barColor = OpalColors.Accent,
                centerLabel = "AVG"
            )
            Spacer(Modifier.height(OpalSpacing.xl))
            DetailRow(
                left = "Olishlar",
                value = "${today?.pickups ?: 0}",
                status = if ((today?.pickups ?: 0) <= 50) "Ajoyib" else "Ko'p",
                statusColor = if ((today?.pickups ?: 0) <= 50) OpalColors.Success else OpalColors.Amber,
                fraction = ((today?.pickups ?: 0) / 120f).coerceIn(0.04f, 1f),
                barColor = OpalColors.Success
            )

            Spacer(Modifier.height(OpalSpacing.xxl))

            // ---- Weekly summary ----
            GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 16.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bu hafta tejaldi", fontSize = 13.sp, color = OpalColors.TextSecondary)
                        AnimatedCount(
                            value = stats.weekSavedMinutes,
                            suffix = " daqiqa",
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OpalIcons(OpalIcon.Flame, OpalColors.Amber, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${profile.streakDays}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
                    }
                }
            }

            Spacer(Modifier.height(insets.calculateBottomPadding() + OpalSpacing.xxxl))
        }
    }
}

@Composable
private fun RoundCloseButton(onClick: () -> Unit) {
    Pressable(onClick = onClick) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            OpalIcons(OpalIcon.ChevronLeft, OpalColors.TextPrimary, Modifier.size(19.dp))
        }
    }
}

@Composable
private fun ArcScoreGauge(score: Int, improving: Boolean, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 13.dp.toPx()
            val inset = stroke / 2f + 4f
            val diameter = size.width - inset * 2
            val arcSize = Size(diameter, diameter)
            // track (top half: 180..360)
            drawArc(
                color = OpalColors.Track,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            val sweep = 180f * (score.coerceIn(0, 100) / 100f)
            drawArc(
                brush = OpalSweep,
                startAngle = 180f,
                sweepAngle = sweep.coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                AnimatedCount(
                    value = score,
                    style = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Bold, color = OpalColors.Accent, letterSpacing = (-1).sp)
                )
                Spacer(Modifier.width(5.dp))
                Canvas(Modifier.padding(top = 10.dp).size(14.dp)) {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(p, if (improving) OpalColors.Accent else OpalColors.Danger)
                }
            }
            Text(
                "Opal Score",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OpalColors.TextTertiary,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
private fun DetailRow(
    left: String,
    value: String,
    status: String,
    statusColor: Color,
    fraction: Float,
    barColor: Color,
    centerLabel: String? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(left, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text(value, fontSize = 13.sp, color = OpalColors.TextTertiary)
            }
            Text(status, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
        }
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OpalColors.Track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(barColor)
                )
            }
            if (centerLabel != null) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1A201C))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(centerLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextSecondary)
                }
            }
        }
    }
}
