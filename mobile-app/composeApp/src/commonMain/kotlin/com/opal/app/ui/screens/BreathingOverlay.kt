package com.opal.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.platform.rememberSafePadding
import com.opal.app.theme.OpalColors
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import kotlinx.coroutines.delay
import kotlin.math.ceil

private const val CYCLE_MS = 12_000
private const val TOTAL_MS = 60_000

/** 1 daqiqalik nafas mashqi — 4s olish · 4s ushlash · 4s chiqarish (5 sikl). */
@Composable
fun BreathingOverlay(onClose: () -> Unit) {
    val insets = rememberSafePadding()
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (true) {
            delay(100)
            val next = elapsed + 100
            if (next >= TOTAL_MS) {
                elapsed = TOTAL_MS
                running = false
                break
            }
            elapsed = next
        }
    }

    val done = elapsed >= TOTAL_MS
    val inCycle = elapsed % CYCLE_MS
    val phase = when {
        done -> "Ajoyib ✨"
        inCycle < 4000 -> "Nafas oling"
        inCycle < 8000 -> "Ushlab turing"
        else -> "Nafas chiqaring"
    }
    val targetScale = if (!done && inCycle < 8000) 1.35f else 1f
    val scale by animateFloatAsState(targetScale, tween(4000, easing = LinearEasing), label = "breathScale")
    val c1 by animateColorAsState(
        when {
            done -> Color(0xFF86EFAC)
            inCycle < 4000 -> Color(0xFF86EFAC)
            inCycle < 8000 -> Color(0xFFB7F5CD)
            else -> Color(0xFF3D9970)
        },
        tween(1000), label = "breatheC1"
    )
    val c2 by animateColorAsState(if (inCycle < 8000) Color(0xFF5EEAD4) else Color(0xFF3D9970), tween(1000), label = "breatheC2")
    val secondsLeft = ceil((TOTAL_MS - elapsed) / 1000.0).toInt()
    val cycleNum = minOf(elapsed / CYCLE_MS + 1, 5)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05060F).copy(alpha = 0.97f))
    ) {
        // yulduzlar + markaziy nur
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0x1A5EEAD4), Color.Transparent)),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
            listOf(0.24f to 0.28f, 0.76f to 0.40f, 0.60f to 0.78f, 0.35f to 0.62f).forEach { (fx, fy) ->
                drawCircle(Color.White.copy(alpha = 0.4f), radius = 1.6f, center = Offset(size.width * fx, size.height * fy))
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = insets.calculateTopPadding() + 12.dp, bottom = insets.calculateBottomPadding() + 24.dp)
        ) {
            // top
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("🌬️ Nafas mashqi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 15.sp, color = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            // breathing circle
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color.White.copy(alpha = 0.10f), radius = size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                        drawCircle(Color.White.copy(alpha = 0.05f), radius = size.minDimension / 2f - 20f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                    }
                    Box(
                        Modifier
                            .size(190.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c1, c2)))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(targetState = phase, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "phase") { p ->
                            Text(p, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (done) "Mashq tugadi" else "${secondsLeft}s qoldi · $cycleNum/5 sikl",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // sikl nuqtalari
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { i ->
                    val active = cycleNum > i || done
                    val current = cycleNum == i && !done
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .width(if (active || current) 24.dp else 6.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) Color(0xFF86EFAC) else if (current) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f))
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // bottom
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .let {
                            if (done) it.background(Brush.linearGradient(listOf(Color(0xFF86EFAC), Color(0xFF5EEAD4))))
                            else if (running) it.background(Color.White.copy(alpha = 0.08f)).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                            else it.background(Color.White)
                        }
                        .clickable {
                            when {
                                done -> onClose()
                                running -> running = false
                                else -> running = true
                            }
                        }
                        .padding(horizontal = 28.dp, vertical = 11.dp)
                ) {
                    Text(
                        if (done) "Yopish" else if (running) "Pauza" else "Davom etish",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (done) Color(0xFF052E16) else if (running) Color.White.copy(alpha = 0.8f) else Color.Black
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "4 soniya oling · 4 ushlab turing · 4 chiqaring",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.30f)
                )
            }
        }
    }
}
