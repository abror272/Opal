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
import androidx.compose.ui.graphics.Brush
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
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        Text("Fokus sessiyasi", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = OpalColors.TextPrimary)
        Text(
            "Rejim tanlang — chalg'ituvchilar darhol bloklanadi",
            fontSize = 13.sp,
            color = OpalColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (active != null) {
            GlassCard(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape).background(OpalColors.Success)
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
            Spacer(Modifier.height(16.dp))
        }

        // ---- Davomiylik tanlash (CUSTOM uchun) ----
        Text("Maxsus davomiylik", fontSize = 13.sp, color = OpalColors.TextTertiary)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SESSION_DURATIONS.forEach { d ->
                val selected = d == customMinutes
                Pressable(onClick = { customMinutes = d }) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) OpalGradient
                                else Color.White.copy(alpha = 0.07f)
                            )
                            .border(
                                0.5.dp,
                                if (selected) Color.Transparent else Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "${d}d",
                            fontSize = 12.5.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else OpalColors.TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- Preset grid ----
        val rows = SESSION_PRESETS.chunked(2)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { preset ->
                    val minutes = if (preset.type == "CUSTOM") customMinutes else preset.minutes
                    val isCustom = preset.type == "CUSTOM"
                    val isActive = active?.preset?.type == preset.type

                    Pressable(
                        onClick = {
                            if (active == null) {
                                sessionCtl.start(preset.copy(minutes = minutes))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        GlassPane(
                            Modifier
                                .fillMaxWidth()
                                .height(158.dp),
                            radius = 24.dp,
                            base = if (isActive) 0.12f else 0.06f
                        ) {
                            if (isCustom) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .border(1.dp, OpalGradient, RoundedCornerShape(24.dp))
                                )
                            }
                            Column(
                                Modifier
                                    .matchParentSize()
                                    .padding(15.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(preset.emoji, fontSize = 30.sp)
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
            Spacer(Modifier.height(12.dp))
        }

        // ---- Tarix ----
        Spacer(Modifier.height(10.dp))
        Text("Yaqinda", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
        Spacer(Modifier.height(10.dp))
        history.take(5).forEach { s ->
            GlassCard(Modifier.fillMaxWidth(), radius = 20.dp, padding = 13.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = OpalColors.TextPrimary)
                        Text(
                            "${s.durationMinutes} daqiqa rejim",
                            fontSize = 11.sp,
                            color = OpalColors.TextTertiary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        if (s.completed) "+${s.savedMinutes}d" else "↓ ${s.savedMinutes}d",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (s.completed) OpalColors.Success else OpalColors.Danger
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(28.dp))
    }
}
