package com.opal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.SESSION_PRESETS
import com.opal.app.data.formatClock
import com.opal.app.glass.GlassCard
import com.opal.app.glass.GlassPane
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalGradient
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.GlassButton
import kotlinx.coroutines.launch

@Composable
fun FocusScreen() {
    val sessionCtl = remember { AppGraph.sessions }
    val active by sessionCtl.active.collectAsState()
    val repo = remember { AppGraph.repo }
    val profile by repo.profile.collectAsState()
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf(SESSION_PRESETS.first()) }
    var minutes by remember { mutableIntStateOf(45) }

    // Preset tanlanganda uning standart davomiyligini qo'yamiz
    fun selectPreset(index: Int) {
        selected = SESSION_PRESETS[index]
        minutes = SESSION_PRESETS[index].minutes
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = OpalSpacing.xl)
    ) {
        Spacer(Modifier.height(OpalSpacing.sm))

        Text(
            "Timer",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OpalColors.TextPrimary,
            letterSpacing = (-0.3).sp
        )

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Scoreboard ----
        val digit = TextStyle(
            fontSize = 62.sp,
            fontWeight = FontWeight.Bold,
            color = OpalColors.MintLight,
            letterSpacing = 2.sp,
            shadow = Shadow(color = OpalColors.Accent.copy(alpha = 0.55f), blurRadius = 26f)
        )
        GlassPane(Modifier.fillMaxWidth().height(150.dp), radius = OpalRadius.lg, base = 0.06f) {
            // tashqi ramka (scoreboard frame)
            Box(
                Modifier
                    .matchParentSize()
                    .border(1.6.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(OpalRadius.lg))
            )
            Box(Modifier.matchParentSize().padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatClock(minutes * 60L).substringBefore(":"), style = digit)
                        Text(
                            " : ",
                            style = TextStyle(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpalColors.MintLight.copy(alpha = 0.5f),
                                shadow = Shadow(color = OpalColors.Accent.copy(alpha = 0.5f), blurRadius = 24f)
                            )
                        )
                        Text(formatClock(minutes * 60L).substringAfter(":"), style = digit)
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.62f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.28f))
                    )
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.lg))

        if (active != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(OpalColors.Success))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sessiya davom etmoqda — ${active?.preset?.label}",
                    fontSize = 13.sp,
                    color = OpalColors.TextSecondary
                )
            }
            Spacer(Modifier.height(OpalSpacing.md))
        }

        // ---- Preset chips ----
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)
        ) {
            SESSION_PRESETS.forEachIndexed { i, p ->
                val isSelected = p.type == selected.type
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) OpalColors.AccentSoft else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (isSelected) OpalColors.Accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(50)
                        )
                        .clickable { selectPreset(i) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.emoji, fontSize = 14.sp)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            p.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) OpalColors.TextPrimary else OpalColors.TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Duration selector ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton(OpalIcon.Minus) { minutes = (minutes - 5).coerceAtLeast(5) }
            Box(
                Modifier
                    .padding(horizontal = OpalSpacing.lg)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 26.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${minutes}d",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpalColors.TextPrimary
                )
            }
            StepButton(OpalIcon.Plus) { minutes = (minutes + 5).coerceAtMost(480) }
        }

        Spacer(Modifier.height(OpalSpacing.xl))

        // ---- Start ----
        GlassButton(
            text = "Start Timer",
            modifier = Modifier.fillMaxWidth(),
            icon = OpalIcon.Play,
            enabled = active == null
        ) {
            sessionCtl.start(selected.copy(minutes = minutes))
        }

        Spacer(Modifier.height(OpalSpacing.lg))

        // ---- Block / Strict ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)
        ) {
            TogglePill(
                icon = OpalIcon.Lock,
                label = if (profile.protectionEnabled) "Block Apps On" else "Block Apps Off",
                on = profile.protectionEnabled,
                modifier = Modifier.weight(1f)
            ) { checked -> scope.launch { repo.setProtection(checked) } }

            TogglePill(
                icon = OpalIcon.Zap,
                label = if (profile.strictMode) "Strict On" else "Strict Off",
                on = profile.strictMode,
                modifier = Modifier.weight(1f)
            ) { checked -> scope.launch { repo.setStrict(checked) } }
        }

        Spacer(Modifier.height(OpalSpacing.xxl))

        // ---- Recent ----
        val sessions by repo.sessions.collectAsState()
        if (sessions.isNotEmpty()) {
            Text("Yaqinda", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
            Spacer(Modifier.height(OpalSpacing.md))
            sessions.take(4).forEach { s ->
                GlassCard(Modifier.fillMaxWidth(), radius = OpalRadius.md, padding = 13.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) { Text(s.emoji, fontSize = 17.sp) }
                        Spacer(Modifier.width(OpalSpacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(s.label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = OpalColors.TextPrimary)
                            Text("${s.durationMinutes} daqiqa", fontSize = 11.sp, color = OpalColors.TextTertiary)
                        }
                        OpalIcons(
                            if (s.completed) OpalIcon.Check else OpalIcon.TrendDown,
                            if (s.completed) OpalColors.Success else OpalColors.Danger,
                            Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(OpalSpacing.sm))
            }
        }

        Spacer(Modifier.height(OpalSpacing.xxxl))
    }
}

@Composable
private fun StepButton(icon: OpalIcon, onClick: () -> Unit) {
    Pressable(onClick = onClick) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            OpalIcons(icon, OpalColors.TextPrimary, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TogglePill(
    icon: OpalIcon,
    label: String,
    on: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit
) {
    val tint = if (on) OpalColors.Success else OpalColors.TextTertiary
    Pressable(onClick = { onChange(!on) }, modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.2.dp, if (on) tint.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpalIcons(icon, tint, Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    label,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) OpalColors.TextPrimary else OpalColors.TextSecondary
                )
            }
        }
    }
}
