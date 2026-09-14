package com.opal.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.data.AppGraph
import com.opal.app.data.SessionPreset
import com.opal.app.data.formatClock
import com.opal.app.glass.Pressable
import com.opal.app.theme.OpalGradient
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalRadius
import com.opal.app.theme.OpalSpacing
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import com.opal.app.ui.components.GlassButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import opalapp.composeapp.generated.resources.Res
import opalapp.composeapp.generated.resources.timer_scene

private val TIMER_PRESETS = listOf(
    SessionPreset("DEEP_FOCUS", "Deep Focus", "🧠", 45, ""),
    SessionPreset("WORK", "Ish rejimi", "💼", 90, ""),
    SessionPreset("STUDY", "O'qish", "📚", 60, ""),
    SessionPreset("SLEEP", "Uyqu", "🌙", 480, ""),
    SessionPreset("CUSTOM", "Maxsus", "⚡", 30, "")
)

@Composable
fun FocusScreen() {
    val sessionCtl = remember { AppGraph.sessions }
    val active by sessionCtl.active.collectAsState()
    val repo = remember { AppGraph.repo }
    val profile by repo.profile.collectAsState()
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf(TIMER_PRESETS.first()) }
    var duration by remember { mutableIntStateOf(45) }
    var blockOn by remember { mutableStateOf(true) }
    var strict by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // ---- Immersiv fon ----
        Image(
            painter = painterResource(Res.drawable.timer_scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF05060F).copy(alpha = 0.70f),
                        0.5f to Color(0xFF05060F).copy(alpha = 0.35f),
                        1f to Color(0xFF05060F).copy(alpha = 0.85f)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF05060F).copy(alpha = 0.80f),
                        0.18f to Color.Transparent
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.72f to Color.Transparent,
                        1f to Color(0xFF05060F)
                    )
                )
        )

        // ---- Kontent ----
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OpalSpacing.xl)
        ) {
            Spacer(Modifier.height(OpalSpacing.sm))

            Text(
                "Timer",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OpalColors.TextPrimary.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(OpalSpacing.xxl))

            // ---- LCD soat ----
            TimerClock(
                clock = formatClock(duration * 60L),
                progress = 0f,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(OpalSpacing.xxl))

            // ---- Preset chiplar ----
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(OpalSpacing.sm)
            ) {
                TIMER_PRESETS.forEach { p ->
                    val isSelected = p.type == selected.type
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.30f))
                            .border(
                                1.dp,
                                if (isSelected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(50)
                            )
                            .clickable {
                                selected = p
                                duration = minOf(p.minutes, 180)
                            }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.emoji, fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                p.label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(OpalSpacing.lg))

            // ---- Stepper ----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepButton(OpalIcon.Minus, enabled = duration > 15) { duration = (duration - 15).coerceAtLeast(15) }
                Box(
                    Modifier
                        .padding(horizontal = OpalSpacing.md)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.40f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 26.dp, vertical = 11.dp)
                ) {
                    Text(
                        "${duration}d",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                StepButton(OpalIcon.Plus, enabled = duration < 180) { duration = (duration + 15).coerceAtMost(180) }
            }

            Spacer(Modifier.height(OpalSpacing.lg))

            // ---- Start ----
            GlassButton(
                text = "Start Timer",
                modifier = Modifier.fillMaxWidth(),
                icon = OpalIcon.Play,
                enabled = active == null
            ) {
                sessionCtl.start(selected.copy(minutes = duration))
            }

            Spacer(Modifier.height(OpalSpacing.md))

            // ---- Block / Strict ----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerToggle(
                    icon = OpalIcon.Lock,
                    label = "Block Apps ${if (blockOn) "On" else "Off"}",
                    on = blockOn,
                    onColor = OpalColors.Accent
                ) {
                    blockOn = !blockOn
                    scope.launch { repo.setProtection(blockOn) }
                }
                Spacer(Modifier.width(OpalSpacing.sm))
                TimerToggle(
                    icon = OpalIcon.Zap,
                    label = "Strict ${if (strict) "On" else "Off"}",
                    on = strict,
                    onColor = OpalColors.Danger
                ) {
                    strict = !strict
                    scope.launch { repo.setStrict(strict) }
                }
            }

            Spacer(Modifier.height(OpalSpacing.xxxl))
        }
    }
}

/** Metall-glass korpusdagi LCD soat. */
@Composable
private fun TimerClock(clock: String, progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFC8D8E8).copy(alpha = 0.30f),
                        Color(0xFF8FA5C0).copy(alpha = 0.25f),
                        Color(0xFF3D5170).copy(alpha = 0.30f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
            .padding(7.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xE60A0F16))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                .padding(vertical = 28.dp, horizontal = 16.dp)
        ) {
            // skan chiziqlari
            Canvas(Modifier.matchParentSize()) {
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        Color(0xFF86EFAC).copy(alpha = 0.06f),
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += 3f
                }
            }
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    clock,
                    style = TextStyle(
                        fontSize = 62.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = Color(0xFFCFEEFF),
                        shadow = Shadow(color = OpalColors.Accent.copy(alpha = 0.6f), blurRadius = 30f)
                    )
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(OpalGradient)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: OpalIcon, enabled: Boolean, onClick: () -> Unit) {
    Pressable(onClick = { if (enabled) onClick() }) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = if (enabled) 0.15f else 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            OpalIcons(icon, Color.White.copy(alpha = if (enabled) 0.85f else 0.3f), Modifier.size(17.dp))
        }
    }
}

@Composable
private fun TimerToggle(
    icon: OpalIcon,
    label: String,
    on: Boolean,
    onColor: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) onColor.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.35f))
            .border(1.dp, if (on) onColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OpalIcons(icon, if (on) onColor else Color.White.copy(alpha = 0.55f), Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (on) Color.White else Color.White.copy(alpha = 0.55f)
            )
        }
    }
}
