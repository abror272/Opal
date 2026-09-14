package com.opal.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.platform.rememberSafePadding
import com.opal.app.theme.OpalColors
import com.opal.app.ui.OpalIcon
import com.opal.app.ui.OpalIcons
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import opalapp.composeapp.generated.resources.Res
import opalapp.composeapp.generated.resources.opal_crystal
import opalapp.composeapp.generated.resources.timer_scene

private data class Slide(
    val img: DrawableResource?,
    val blend: Boolean,
    val badge: String,
    val title: String,
    val desc: String
)

private val SLIDES = listOf(
    Slide(Res.drawable.opal_crystal, true, "BLOKLASH", "Opal chalg'ituvchilarni siz uchun bloklaydi", "TikTok, Instagram va boshqalar — bir bosish bilan himoya ostida."),
    Slide(Res.drawable.timer_scene, false, "FOKUS", "Bir bosish. To'liq fokus.", "Flip-clock taymer bilan sessiya boshlang — ilovalar o'z-o'zidan bloklanadi."),
    Slide(null, false, "BALL", "Progressingizni chuqur his qiling", "Opal balli uyqu, fokus va damni bitta ko'rsatkichga jamsheydi.")
)

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val insets = rememberSafePadding()
    var step by remember { mutableIntStateOf(0) }
    val slide = SLIDES[step]

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05060F))
    ) {
        // ---- Fon ----
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(450)) + androidx.compose.animation.scaleIn(initialScale = 1.06f, animationSpec = tween(450))) togetherWith fadeOut(tween(300))
            },
            label = "onbBg"
        ) { s ->
            val sl = SLIDES[s]
            Box(Modifier.fillMaxSize()) {
                if (sl.img != null) {
                    Image(
                        painter = painterResource(sl.img),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(if (sl.blend) 0.45f else 0.40f)
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color(0xFF05060F).copy(alpha = 0.40f),
                                    0.5f to Color.Transparent,
                                    1f to Color(0xFF05060F)
                                )
                            )
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0x80324066), Color.Transparent),
                                    center = Offset(0.5f, 0.3f).let { Offset(Float.MAX_VALUE, 0f) } // placeholder
                                )
                            )
                    )
                    // yulduzlar
                    Canvas(Modifier.fillMaxSize()) {
                        val pts = listOf(
                            0.22f to 0.30f, 0.74f to 0.18f, 0.58f to 0.62f, 0.38f to 0.76f,
                            0.15f to 0.55f, 0.85f to 0.44f, 0.66f to 0.82f, 0.30f to 0.12f
                        )
                        pts.forEach { (fx, fy) ->
                            drawCircle(
                                Color.White.copy(alpha = 0.45f),
                                radius = 2f,
                                center = Offset(size.width * fx, size.height * fy)
                            )
                        }
                    }
                }
            }
        }

        // matn scrim
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF05060F).copy(alpha = 0.88f),
                            Color(0xFF05060F).copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(500f, 1100f),
                        radius = 900f
                    )
                )
        )

        // ---- Kontent ----
        Column(Modifier.fillMaxSize().padding(top = insets.calculateTopPadding() + 8.dp, bottom = insets.calculateBottomPadding() + 28.dp)) {
            // header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF86EFAC), Color(0xFFB18CFF), Color(0xFFFF9AD5)))
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Opal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(
                    "O'tkazib yuborish",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onDone() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // matn
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(400)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(400, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))) togetherWith
                        (fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(250)))
                },
                label = "onbText"
            ) { s ->
                val sl = SLIDES[s]
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(sl.badge, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.7f), letterSpacing = 2.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        sl.title,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        sl.desc,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // pastki: nuqtalar + tugma
            Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SLIDES.forEachIndexed { i, _ ->
                        val active = i == step
                        Box(
                            Modifier
                                .width(if (active) 28.dp else 8.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) Color(0xFF86EFAC) else Color.White.copy(alpha = 0.20f))
                                .clickable { step = i }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(50))
                        .clickable {
                            if (step < SLIDES.size - 1) step++ else onDone()
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (step == SLIDES.size - 1) "Boshlash" else "Davom etish",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        OpalIcons(OpalIcon.ChevronRight, Color.White, Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}
