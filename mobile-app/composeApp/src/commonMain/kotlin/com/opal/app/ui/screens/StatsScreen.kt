package com.opal.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.AnimatedCount
import com.opal.app.ui.components.StatRingPill

private enum class Metric(val title: String, val desc: String) {
    SLEEP("Uyqu", "Uyqu ko'rsatkichi kechqurun, uyqu paytida va ertalab sodir bo'ladigan holatlarni va ularning keyingi kuningizga ta'sirini o'lchaydi."),
    FOCUS("Fokus", "Fokus ko'rsatkichi chalg'ituvchi ilovalarsiz o'tkazgan vaqtni va fokus sessiyalari sifatini o'lchaydi."),
    REST("Dam", "Dam ko'rsatkichi ekran vaqtini kamaytirish, tanaffuslar va tiklanishni o'lchaydi.")
}

@Composable
fun StatsScreen(onClose: () -> Unit) {
    val repo = remember { AppGraph.repo }
    val insets = rememberSafePadding()
    val stats by repo.stats.collectAsState()
    val profile by repo.profile.collectAsState()
    val sessions by repo.sessions.collectAsState()
    val apps by repo.apps.collectAsState()

    val scores = remember(profile, stats, sessions) { computeScores(profile, stats, sessions) }
    val today = stats.today
    val screenDelta = (today?.screenTimeMinutes ?: 0) - stats.avgDailyScreenMinutes
    val pickups = today?.pickups ?: 0
    val completedToday = sessions.count { it.completed }
    val distracting = apps.filter { it.category in listOf("Ijtimoiy", "Ijtimoiy tarmoq", "O'yinlar", "Video") }.sumOf { it.todayMinutes }

    var metric by remember { mutableStateOf(Metric.SLEEP) }

    Box(Modifier.fillMaxSize().background(OpalColors.Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OpalSpacing.xl)
        ) {
            Spacer(Modifier.height(insets.calculateTopPadding() + OpalSpacing.md))

            // ---- Header ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundCloseButton(onClose)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2039", fontSize = 22.sp, color = OpalColors.TextTertiary)
                    Text("Bugun", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary, modifier = Modifier.padding(horizontal = 16.dp))
                    Text("\u203A", fontSize = 22.sp, color = OpalColors.TextTertiary)
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(42.dp))
            }

            Spacer(Modifier.height(OpalSpacing.lg))

            // ---- Score arc ----
            ScoreArc(score = scores.score, delta = scores.delta, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(OpalSpacing.md))

            // ---- Selectable pills ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)) {
                StatRingPill(OpalIcon.Moon, scores.sleep, "Uyqu", scores.sleep / 100f, Modifier.weight(1f)) { metric = Metric.SLEEP }
                StatRingPill(OpalIcon.Hourglass, scores.focus, "Fokus", scores.focus / 100f, Modifier.weight(1f)) { metric = Metric.FOCUS }
                StatRingPill(OpalIcon.Plant, scores.rest, "Dam", scores.rest / 100f, Modifier.weight(1f)) { metric = Metric.REST }
            }

            Spacer(Modifier.height(OpalSpacing.xl))

            // ---- What is X Score? ----
            Text("${metric.title} ko'rsatkichi nima?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
            Text(
                metric.desc,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = OpalColors.TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(OpalSpacing.md))

            GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 16.dp) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(OpalSpacing.xl)) {
                    when (metric) {
                        Metric.SLEEP -> {
                            MetricRow("Uyqu", "0d", "Qisqa", 4f, 78f)
                            MetricRow("Olishlar", "$pickups marta", rating(pickups, 12, 30), (100 - pickups * 2).toFloat().coerceIn(6f, 100f), 42f)
                            MetricRow("Ekran vaqti (kech)", formatMinutes((today?.screenTimeMinutes ?: 0) * 3 / 10), if ((today?.screenTimeMinutes ?: 0) < 200) "Ajoyib" else "Yaxshi", (100 - (today?.screenTimeMinutes ?: 0) / 5f).coerceIn(8f, 100f), 55f)
                        }
                        Metric.FOCUS -> {
                            MetricRow("Fokus sessiyalari", "$completedToday ta", if (completedToday >= 2) "Ajoyib" else if (completedToday >= 1) "Yaxshi" else "Qisqa", (completedToday * 33 + 8).toFloat().coerceIn(6f, 100f), 38f)
                            MetricRow("Bugun tejaldi", formatMinutes(today?.savedMinutes ?: 0), if ((today?.savedMinutes ?: 0) >= 30) "Ajoyib" else "Yaxshi", ((today?.savedMinutes ?: 0) / 90f * 100).coerceIn(5f, 100f), 45f)
                            MetricRow("Chalg'ituvchi ilovalar", formatMinutes(distracting), if (distracting <= 30) "Ajoyib" else "Ko'p", (100 - distracting).toFloat().coerceIn(6f, 100f), 52f)
                        }
                        Metric.REST -> {
                            MetricRow("Ekran vaqti", formatMinutes(today?.screenTimeMinutes ?: 0), if (screenDelta <= 0) "Ajoyib" else "Yaxshi", (100 - (today?.screenTimeMinutes ?: 0) / 5f).coerceIn(6f, 100f), 48f)
                            MetricRow("O'rtachaga nisbat", (if (screenDelta <= 0) "▼ " else "▲ ") + formatMinutes(kotlin.math.abs(screenDelta)), if (screenDelta <= 0) "Ajoyib" else "Qisqa", (50 - screenDelta / 4f).coerceIn(6f, 100f), 50f)
                        }
                    }
                }
            }

            Spacer(Modifier.height(OpalSpacing.xl))

            // ---- Highlights ----
            Text("BUGUNGI NATIJALAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextTertiary, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(OpalSpacing.md))
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

private fun rating(value: Int, good: Int, ok: Int): String =
    if (value <= good) "Ajoyib" else if (value <= ok) "Yaxshi" else "Ko'p"

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

/** Yarim arc gauge — gradient + bracket + Opal Score. */
@Composable
private fun ScoreArc(score: Int, delta: Int, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f + 6f
            val diameter = (size.width - inset * 2).coerceAtMost(size.height * 1.7f)
            val arcSize = Size(diameter, diameter)
            val left = (size.width - diameter) / 2f
            val top = 20f
            // track
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(left, top), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // value
            drawArc(
                brush = Brush.linearGradient(listOf(OpalColors.Success, OpalColors.Accent, OpalColors.AccentDeep)),
                startAngle = 180f,
                sweepAngle = (180f * (score.coerceIn(0, 100) / 100f)).coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = Offset(left, top), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // bracket
            val bracket = Path().apply {
                moveTo(size.width * 0.27f, top + diameter / 2f + 6f)
                quadraticTo(size.width * 0.5f, top + diameter / 2f + 22f, size.width * 0.73f, top + diameter / 2f + 6f)
            }
            drawPath(bracket, Color.White.copy(alpha = 0.18f), style = Stroke(1.6f, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 52.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "$score",
                    style = TextStyle(
                        fontSize = 44.sp, fontWeight = FontWeight.ExtraBold,
                        color = OpalColors.Accent, letterSpacing = (-1).sp,
                        shadow = androidx.compose.ui.graphics.Shadow(color = OpalColors.Accent.copy(alpha = 0.5f), blurRadius = 22f)
                    )
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (delta >= 0) "▲" else "▼",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (delta >= 0) OpalColors.Success else OpalColors.Danger,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Text("Opal balli", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextTertiary)
        }
    }
}

/** Metrik qatori — sarlavha/qiymat/reyting + slayder (AVG belgisi bilan). */
@Composable
private fun MetricRow(title: String, value: String, ratingLabel: String, position: Float, avgAt: Float) {
    val color = when (ratingLabel) {
        "Ajoyib" -> OpalColors.Success
        "Ko'p" -> OpalColors.Danger
        else -> OpalColors.Amber
    }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                Spacer(Modifier.width(7.dp))
                Text(value, fontSize = 12.5.sp, color = OpalColors.TextTertiary)
            }
            Text(ratingLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
        Spacer(Modifier.height(9.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val w = maxWidth
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OpalColors.Track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(position.coerceIn(0f, 100f) / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(OpalColors.AccentDeep, OpalColors.Success)))
                )
            }
            // AVG belgisi
            Box(
                Modifier
                    .offset(x = w * (avgAt / 100f) - 16.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1A201C))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("O'RT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextSecondary)
            }
        }
    }
}
