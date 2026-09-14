package com.opal.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.platform.rememberSafePadding
import kotlinx.coroutines.delay
import kotlin.random.Random

private val WITTY = listOf(
    "Siz bu qoidani kecha o'rnatgansiz. O'tgan siz to'g'ri edi.",
    "Kelajakdagi sizga minnatdorchilik bildiradi.",
    "Bu ilova sizning eng yaxshi versiyangizga yo'l to'sqinlik qiladi.",
    "Bir oz tinchlanish — eng yaxshi tanlov."
)

/** "X — Opal tomonidan bloklandi" ekrani + Battle Math bilan ochish. */
@Composable
fun BlockedAppScreen(
    appName: String,
    appIcon: ImageBitmap?,
    onDismiss: () -> Unit,
    onAllow: () -> Unit
) {
    var challenge by remember { mutableStateOf(false) }
    val insets = rememberSafePadding()
    val witty = WITTY[appName.length % WITTY.size]

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF020204))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0x598FD9FF),
                        Color(0x33B18CFF),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width / 2f, size.height * 0.33f)
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = insets.calculateTopPadding() + 72.dp, bottom = insets.calculateBottomPadding() + 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // ilova belgisi (xiralashtirilgan)
            Box(
                Modifier
                    .size(78.dp)
                    .graphicsLayer {
                        alpha = 0.92f
                        scaleX = 1.02f
                        scaleY = 1.02f
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Text("📱", fontSize = 34.sp)
                }
            }

            Spacer(Modifier.height(26.dp))

            Text(
                appName,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
            Text(
                "Opal tomonidan bloklandi",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Text(
                witty,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 44.dp)
            )

            Spacer(Modifier.weight(1f))

            // Yopish
            Box(
                Modifier
                    .fillMaxWidth(0.78f)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Yopish", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(Modifier.height(14.dp))

            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { challenge = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Baribir ochish · 🧮 Battle Math",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }

        // Battle Math
        AnimatedVisibility(
            visible = challenge,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(200))
        ) {
            BattleMathScreen(
                appName = appName,
                onClose = { challenge = false },
                onSolved = onAllow
            )
        }
    }
}

private data class MathQ(val a: Int, val b: Int) {
    val answer: Int get() = a * b
}

private fun makeQ(rnd: Random) = MathQ(6 + rnd.nextInt(5), 5 + rnd.nextInt(6))

/** 3 ta ko'paytirish masalasi — noto'g'ri bo'lsa yangi savollar va silkinish. */
@Composable
private fun BattleMathScreen(appName: String, onClose: () -> Unit, onSolved: () -> Unit) {
    val insets = rememberSafePadding()
    val rnd = remember { Random(System.currentTimeMillis()) }
    var questions by remember { mutableStateOf(List(3) { makeQ(rnd) }) }
    var values by remember { mutableStateOf(listOf("", "", "")) }
    var focus by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var shakeX by remember { mutableFloatStateOf(0f) }

    // Har bir katak o'z javobining uzunligiga yetganda tekshiramiz.
    val allFilled = values.indices.all { values[it].length >= questions[it].answer.toString().length }

    LaunchedEffect(values, solved) {
        if (!allFilled || solved) return@LaunchedEffect
        delay(90)
        val ok = values.indices.all { values[it].toIntOrNull() == questions[it].answer }
        if (ok) {
            solved = true
            status = "✓ $appName ochildi!"
            delay(650)
            onSolved()
        } else {
            status = "Noto'g'ri — yana urinib ko'ring"
            listOf(-40f, 40f, -27f, 27f, -10f, 0f).forEach { shakeX = it; delay(55) }
            values = listOf("", "", "")
            questions = List(3) { makeQ(rnd) }
            focus = 0
            status = ""
        }
    }

    fun press(k: String) {
        if (solved) return
        val idx = focus
        if (k == "del") {
            values = values.toMutableList().also { it[idx] = it[idx].dropLast(1) }
            return
        }
        if (values[idx].length >= 3) return
        val grown = values[idx] + k
        values = values.toMutableList().also { it[idx] = grown }
        val expected = questions[idx].answer.toString().length
        if (grown.length >= expected && idx < 2) focus = idx + 1
    }

    val keypad = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF2050608))
            .padding(
                top = insets.calculateTopPadding() + 14.dp,
                bottom = insets.calculateBottomPadding() + 20.dp
            )
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            // header
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp) }

                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        "⚔ BATTLE MATH",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(Modifier.width(40.dp))
            }

            Spacer(Modifier.height(24.dp))

            // savollar
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeX }
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (solved) Color(0x1A34D399) else Color.White.copy(alpha = 0.045f)
                    )
                    .border(
                        1.dp,
                        if (solved) Color(0x6634D399) else Color.White.copy(alpha = 0.10f),
                        RoundedCornerShape(26.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    status.ifEmpty { "Barcha masalalarni yeching:" },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (solved) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(16.dp))
                questions.forEachIndexed { i, q ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${q.a} × ${q.b} =",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(
                            Modifier
                                .width(86.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (solved) Color(0x1A34D399)
                                    else if (i == focus) Color(0x145EEAD4)
                                    else Color.Transparent
                                )
                                .border(
                                    2.dp,
                                    if (solved) Color(0xB334D399)
                                    else if (i == focus) Color(0x9986EFAC)
                                    else Color(0x4086EFAC),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { focus = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                values[i],
                                fontFamily = FontFamily.Monospace,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBFE9FF)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // keypad
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                keypad.chunked(3).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { k ->
                            if (k.isEmpty()) {
                                Spacer(Modifier.weight(1f).height(54.dp))
                            } else {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.055f))
                                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                        .clickable { press(k) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (k == "del") "⌫" else k,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (k == "del") Color(0xFF9FD8FF) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
