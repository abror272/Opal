package com.opal.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.opal.app.glass.GlassCard
import com.opal.app.glass.GlassPane
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.FocusRatingCard
import com.opal.app.ui.components.GradientButton
import com.opal.app.ui.components.ProgressRing
import com.opal.app.ui.components.RATING_LEVELS

/** Faol sessiya to'liq ekran overlay — running yoki completion holati. */
@Composable
fun ActiveSessionOverlay() {
    val sessionCtl = remember { AppGraph.sessions }
    val active by sessionCtl.active.collectAsState()
    val completion by sessionCtl.completion.collectAsState()
    val installed by AppGraph.repo.installedApps.collectAsState()
    val blockedPkgs by AppGraph.repo.blockedPackages.collectAsState()

    var confirmExit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { AppGraph.repo.loadDeviceData() }

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

                val frac by animateFloatAsState(
                    targetValue = sessionCtl.progressFraction,
                    animationSpec = tween(1000, easing = LinearEasing),
                    label = "ringFrac"
                )

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = OpalSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(OpalSpacing.md))

                    GlassPane(Modifier.fillMaxWidth(), radius = OpalRadius.lg, base = 0.05f) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color.White.copy(alpha = 0.07f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(a.preset.emoji, fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(OpalSpacing.md))
                            Column {
                                Text(a.preset.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpalColors.TextPrimary)
                                Text("Fokus sessiyasi davom etmoqda", fontSize = 11.5.sp, color = OpalColors.TextTertiary)
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

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
                            Spacer(Modifier.height(OpalSpacing.md))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OpalIcons(OpalIcon.Shield, OpalColors.Success, Modifier.size(12.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${blockedPkgs.size} ta ilova blokda",
                                        fontSize = 11.5.sp,
                                        color = OpalColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)
                    ) {
                        installed.filter { blockedPkgs.contains(it.packageName) }.take(8).forEach { app ->
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val ic = app.icon
                                if (ic != null) {
                                    Image(
                                        bitmap = ic,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp).clip(RoundedCornerShape(5.dp))
                                    )
                                } else {
                                    Text("📱", fontSize = 11.sp)
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(app.label, fontSize = 11.5.sp, color = OpalColors.TextSecondary, maxLines = 1)
                            }
                        }
                        if (blockedPkgs.isEmpty()) {
                            Text(
                                "Bloklangan ilova yo'q — Ilovalarim bo'limida tanlang",
                                fontSize = 11.sp,
                                color = OpalColors.TextTertiary
                            )
                        }
                    }

                    Spacer(Modifier.height(OpalSpacing.xxl))

                    GradientButton(
                        text = "Sessiyani yakunlash",
                        modifier = Modifier.fillMaxWidth(),
                        icon = OpalIcon.Check
                    ) { confirmExit = true }

                    Spacer(Modifier.height(OpalSpacing.md))

                    Pressable(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Davom etish",
                            fontSize = 13.5.sp,
                            color = OpalColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(OpalSpacing.xxl))
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
                GlassCard(Modifier.padding(horizontal = OpalSpacing.xxxl).fillMaxWidth(), radius = OpalRadius.xl, padding = 22.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(OpalColors.DangerSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            OpalIcons(OpalIcon.TrendDown, OpalColors.Danger, Modifier.size(24.dp))
                        }
                        Text(
                            "Erta chiqmoqchimisiz?",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpalColors.TextPrimary,
                            modifier = Modifier.padding(top = OpalSpacing.md)
                        )
                        Text(
                            "Erta chiqish streak'ni 1 ga kamaytiradi.",
                            fontSize = 12.5.sp,
                            color = OpalColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Spacer(Modifier.height(OpalSpacing.lg))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)) {
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
    var picked by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = OpalSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    if (completedFully) Brush.linearGradient(listOf(OpalColors.AccentDeep, OpalColors.MintLight))
                    else Brush.linearGradient(listOf(OpalColors.Amber, OpalColors.Danger))
                ),
            contentAlignment = Alignment.Center
        ) {
            OpalIcons(
                if (completedFully) OpalIcon.Check else OpalIcon.TrendDown,
                Color.White,
                Modifier.size(44.dp)
            )
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
            if (completedFully) "+$savedMinutes daqiqa tejaldi" else "Streak: $streakAfter",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (completedFully) OpalColors.Success else OpalColors.Amber,
            modifier = Modifier.padding(top = OpalSpacing.md)
        )
        if (completedFully) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                OpalIcons(OpalIcon.Flame, OpalColors.Amber, Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "Streak: $streakAfter",
                    fontSize = 13.sp,
                    color = OpalColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        // ---- Fokus baholash ----
        FocusRatingCard(
            early = !completedFully,
            picked = picked,
            onPick = { idx ->
                picked = idx
                AppGraph.repo.rescoreSession(label, RATING_LEVELS[idx].score)
            }
        )
    }
}
