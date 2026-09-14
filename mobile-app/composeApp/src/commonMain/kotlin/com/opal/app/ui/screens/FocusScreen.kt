package com.opal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.SESSION_DURATIONS
import com.opal.app.data.SESSION_PRESETS
import com.opal.app.glass.GlassCard
import com.opal.app.glass.GlassPane
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalGradient
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.GradientCircle

@Composable
fun FocusScreen() {
    val sessionCtl = remember { AppGraph.sessions }
    val active by sessionCtl.active.collectAsState()
    val repo = remember { AppGraph.repo }
    val history by repo.sessions.collectAsState()

    var customMinutes by remember { mutableIntStateOf(30) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OpalSpacing.xl)
    ) {
        Spacer(Modifier.height(OpalSpacing.sm))

        Text("Fokus sessiyasi", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary, letterSpacing = (-0.3).sp)
        Text(
            "Rejim tanlang — chalg'ituvchilar darhol bloklanadi",
            fontSize = 13.sp,
            color = OpalColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(OpalSpacing.lg))

        if (active != null) {
            GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.lg, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(OpalColors.Success)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Sessiya davom etmoqda — ${active?.preset?.label}",
                        fontSize = 13.5.sp,
                        color = OpalColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(OpalSpacing.lg))
        }

        // ---- Maxsus davomiylik ----
        Text("Davomiylik", fontSize = 13.sp, color = OpalColors.TextTertiary)
        Spacer(Modifier.height(OpalSpacing.sm))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)
        ) {
            SESSION_DURATIONS.forEach { d ->
                val selected = d == customMinutes
                Pressable(onClick = { customMinutes = d }) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .let {
                                if (selected) it.background(OpalGradient)
                                else it.background(Color.White.copy(alpha = 0.07f))
                            }
                            .border(
                                0.5.dp,
                                if (selected) Color.Transparent else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${d}d",
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color(0xFF04241A) else OpalColors.TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Preset grid ----
        val rows = SESSION_PRESETS.chunked(2)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.md)) {
                row.forEachIndexed { i, preset ->
                    val minutes = if (preset.type == "CUSTOM") customMinutes else preset.minutes
                    val isCustom = preset.type == "CUSTOM"
                    val isActive = active?.preset?.type == preset.type

                    Pressable(
                        onClick = {
                            if (active == null) sessionCtl.start(preset.copy(minutes = minutes))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        GlassPane(
                            Modifier.fillMaxWidth().height(168.dp),
                            radius = OpalRadius.lg,
                            base = if (isActive) 0.13f else 0.06f
                        ) {
                            if (isCustom) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .border(1.dp, OpalGradient, RoundedCornerShape(OpalRadius.lg))
                                )
                            }
                            Column(
                                Modifier.matchParentSize().padding(15.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    GradientCircle(index = i, modifier = Modifier.size(46.dp)) {
                                        Text(preset.emoji, fontSize = 22.sp)
                                    }
                                    if (isActive) {
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(OpalColors.Success.copy(alpha = 0.16f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("FAOL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OpalColors.Success)
                                        }
                                    }
                                }
                                Column {
                                    Text(
                                        preset.label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OpalColors.TextPrimary
                                    )
                                    Text(
                                        "${minutes} daqiqa",
                                        fontSize = 12.sp,
                                        color = OpalColors.Accent,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                    Text(
                                        preset.desc,
                                        fontSize = 10.5.sp,
                                        color = OpalColors.TextTertiary,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(OpalSpacing.md))
        }

        // ---- Tarix ----
        Spacer(Modifier.height(OpalSpacing.sm))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Yaqinda", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
            if (history.isNotEmpty()) {
                Text("${history.size} sessiya", fontSize = 11.5.sp, color = OpalColors.TextTertiary)
            }
        }
        Spacer(Modifier.height(OpalSpacing.md))
        history.take(5).forEach { s ->
            GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.md, padding = 13.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.emoji, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(OpalSpacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(s.label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = OpalColors.TextPrimary)
                        Text(
                            "${s.durationMinutes} daqiqa rejim",
                            fontSize = 11.sp,
                            color = OpalColors.TextTertiary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OpalIcons(
                            if (s.completed) OpalIcon.Check else OpalIcon.TrendDown,
                            if (s.completed) OpalColors.Success else OpalColors.Danger,
                            Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${s.savedMinutes}d",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (s.completed) OpalColors.Success else OpalColors.Danger
                        )
                    }
                }
            }
            Spacer(Modifier.height(OpalSpacing.sm))
        }

        Spacer(Modifier.height(OpalSpacing.xxxl))
    }
}
