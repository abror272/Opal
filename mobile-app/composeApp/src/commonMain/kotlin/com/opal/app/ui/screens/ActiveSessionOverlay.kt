package com.opal.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.formatClock
import com.opal.app.data.formatMinutes
import com.opal.app.glass.GlassCard
import com.opal.app.glass.GlassPane
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.ui.components.GradientButton
import com.opal.app.ui.components.ProgressRing

/** Faol sessiya to'liq ekran overlay — running yoki completion holati. */
@Composable
fun ActiveSessionOverlay() {
    val sessionCtl = remember { AppGraph.sessions }
    val active by sessionCtl.active.collectAsState()
    val completion by sessionCtl.completion.collectAsState()
    val apps by AppGraph.repo.apps.collectAsState()

    var confirmExit by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0B18), Color(0xFF130F2A), Color(0xFF090812))
                )
            )
    ) {
        when {
            completion != null -> {
                CompletionView(
                    label = completion!!.label,
                    emoji = completion!!.emoji,
                    savedMinutes = completion!!.savedMinutes,
                    completedFully = completion!!.completedFully,
                    streakAfter = completion!!.streakAfter
                )
            }

            active != null -> {
                val a = active!!
                val elapsed by sessionCtl.elapsedSeconds.collectAsState()
                val remaining = (a.totalSeconds - elapsed).coerceAtLeast(0)

                // Har sekundda silliq (linear interpolatsiyali) sweep
                val frac by animateFloatAsState(
                    targetValue = sessionCtl.progressFraction,
                    animationSpec = tween(1000, easing = LinearEasing),
                    label = "ringFrac"
                )

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(14.dp))

                    // sarlavha
                    GlassPane(Modifier.fillMaxWidth(), radius = 22.dp, base = 0.05f) {
                        Row(
                            Modifier.matchParentSize().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(a.preset.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(a.preset.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                                Text("Fokus sessiyasi davom etmoqda", fontSize = 11.5.sp, color = OpalColors.TextTertiary)
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Countdown ring
                    ProgressRing(
                        progress = frac,
                        modifier = Modifier.size(250.dp),
                        strokeWidth = 16.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                formatClock(remaining),
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpalColors.TextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "qoldi",
                                fontSize = 13.sp,
                                color = OpalColors.TextTertiary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "🛡️ ${apps.count { it.blocked }} ta ilova blokda",
                                    fontSize = 11.5.sp,
                                    color = OpalColors.TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Bloklangan ilovalar chiplari
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        apps.filter { it.blocked }.take(6).forEach { app ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("${app.emoji} ${app.name}", fontSize = 11.5.sp, color = OpalColors.TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    GradientButton(
                        text = "Sessiyani yakunlash",
                        modifier = Modifier.fillMaxWidth()
                    ) { confirmExit = true }

                    Spacer(Modifier.height(14.dp))

                    Pressable(onClick = { /* placeholder: davom etish — hech narsa */ }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Davom etish 💪",
                            fontSize = 13.5.sp,
                            color = OpalColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(26.dp))
                }
            }
        }

        // Erta chiqish confirm
        if (confirmExit && active != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(Modifier.padding(horizontal = 32.dp).fillMaxWidth(), radius = 28.dp, padding = 22.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚪", fontSize = 30.sp)
                        Text(
                            "Erta chiqmoqchimisiz?",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpalColors.TextPrimary,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(
                            "Erta chiqish streak'ni 1 ga kamaytiradi.",
                            fontSize = 12.5.sp,
                            color = OpalColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Pressable(
                                onClick = { confirmExit = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(vertical = 13.dp)
                                ) {
                                    Text("Qolish", fontSize = 13.5.sp, color = OpalColors.TextPrimary, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                            Pressable(
                                onClick = {
                                    confirmExit = false
                                    sessionCtl.finish(early = true)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(50))
                                        .background(OpalColors.Danger.copy(alpha = 0.9f))
                                        .padding(vertical = 13.dp)
                                ) {
                                    Text("Chiqish", fontSize = 13.5.sp, color = Color.White, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionView(
    label: String,
    emoji: String,
    savedMinutes: Int,
    completedFully: Boolean,
    streakAfter: Int
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    if (completedFully) Brush.linearGradient(listOf(OpalColors.AccentDeep, OpalColors.Pink))
                    else Brush.linearGradient(listOf(OpalColors.Amber, OpalColors.Danger))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(if (completedFully) "✓" else "↓", fontSize = 44.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Text(
            if (completedFully) "Sessiya yakunlandi!" else "Erta chiqildi",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = OpalColors.TextPrimary,
            modifier = Modifier.padding(top = 22.dp)
        )
        Text(
            "$emoji $label",
            fontSize = 14.sp,
            color = OpalColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            if (completedFully) "+$savedMinutes daqiqa tejaldi 🎉" else "Streak: $streakAfter 🔥",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (completedFully) OpalColors.Success else OpalColors.Amber,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (completedFully) {
            Text(
                "Streak: $streakAfter 🔥",
                fontSize = 13.sp,
                color = OpalColors.TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
