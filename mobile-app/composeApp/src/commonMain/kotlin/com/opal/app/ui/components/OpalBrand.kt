package com.opal.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.theme.OpalColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import opalapp.composeapp.generated.resources.Res
import opalapp.composeapp.generated.resources.gem_century
import opalapp.composeapp.generated.resources.gem_first
import opalapp.composeapp.generated.resources.gem_iron_will
import opalapp.composeapp.generated.resources.gem_motivated
import opalapp.composeapp.generated.resources.gem_night_owl
import opalapp.composeapp.generated.resources.gem_opal_plus
import opalapp.composeapp.generated.resources.gem_pride
import opalapp.composeapp.generated.resources.gem_time_lord
import opalapp.composeapp.generated.resources.opal_crystal

/** Opal wordmark — yuqorida kichik bo'shliqli "O" halqasi + "pal". */
@Composable
fun OpalWordmark(textSize: Int = 20, boxSize: Dp = 21.dp) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.Bottom) {
        Canvas(Modifier.size(boxSize)) {
            val sw = 2.9f * (size.minDimension / 22f)
            drawArc(
                color = Color.White,
                startAngle = -75f,
                sweepAngle = 330f,
                useCenter = false,
                topLeft = Offset(sw / 2f, sw / 2f),
                size = Size(size.width - sw, size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        Text(
            "pal",
            fontSize = textSize.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )
    }
}

/** Opal profil tugmasi — olti burchakli ramka + odam silueti. */
@Composable
fun HexAvatar(onClick: () -> Unit, size: Dp = 36.dp) {
    Canvas(
        Modifier
            .size(size)
            .clickable(onClick = onClick)
    ) {
        val s = this.size.minDimension / 36f
        val hex = Path().apply {
            moveTo(18f * s, 2.5f * s)
            lineTo(31.5f * s, 9.2f * s)
            lineTo(31.5f * s, 26.8f * s)
            lineTo(18f * s, 33.5f * s)
            lineTo(4.5f * s, 26.8f * s)
            lineTo(4.5f * s, 9.2f * s)
            close()
        }
        drawPath(hex, OpalColors.Accent.copy(alpha = 0.07f))
        drawPath(hex, OpalColors.Accent, style = Stroke(width = 1.7f * s))
        drawCircle(Color(0xFFD8FFE6), radius = 3.4f * s, center = Offset(18f * s, 14.2f * s))
        val body = Path().apply {
            moveTo(11.6f * s, 26.5f * s)
            quadraticTo(18f * s, 19.5f * s, 24.4f * s, 26.5f * s)
            close()
        }
        drawPath(body, Color(0xFFD8FFE6))
    }
}

/** Kristall qahramon — haqiqiy opal rasmi, mint nur, tosh poydevor, yon parilklar. */
@Composable
fun CrystalHero(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    crystalSize: Dp = 176.dp
) {
    val inf = rememberInfiniteTransition(label = "crystal")
    val float by inf.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(6500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )
    val pulse by inf.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(crystalSize + 40.dp),
            contentAlignment = Alignment.Center
        ) {
            // mint ambient glow
            Box(
                Modifier
                    .size(crystalSize + 20.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0x3396F0BE),
                                Color(0x1A5EEAD4),
                                Color.Transparent
                            )
                        )
                    )
            )
            // sparkles
            Canvas(Modifier.size(crystalSize + 40.dp)) {
                val w = size.width
                val h = size.height
                val pts = listOf(
                    Offset(w * 0.10f, h * 0.30f) to 2.5f,
                    Offset(w * 0.90f, h * 0.22f) to 2f,
                    Offset(w * 0.84f, h * 0.72f) to 2.5f,
                    Offset(w * 0.16f, h * 0.80f) to 1.8f
                )
                pts.forEach { (p, r) ->
                    drawCircle(
                        color = Color(0xFFD9FFE8).copy(alpha = 0.15f + 0.7f * pulse * (r / 2.5f)),
                        radius = r,
                        center = p
                    )
                }
            }
            // crystal image (radial mask removes black edges)
            Image(
                painter = painterResource(Res.drawable.opal_crystal),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(crystalSize)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .graphicsLayer {
                        translationY = float
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                center = center,
                                radius = size.minDimension * 0.52f
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .clickable(onClick = onClick)
            )
        }

        // tosh poydevor
        Box(
            Modifier
                .width(128.dp)
                .height(26.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF262B28), Color(0xFF14171A), Color(0xFF0A0C0E))),
                    RoundedCornerShape(10.dp)
                )
        )
        Box(
            Modifier
                .width(168.dp)
                .height(12.dp)
                .background(
                    Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)),
                    RoundedCornerShape(50)
                )
        )
    }
}

/** Score'ni 3 pill markaziga bog'lovchi ingichka qavs. */
@Composable
fun ScoreBracket(modifier: Modifier = Modifier, width: Dp = 216.dp) {
    Canvas(modifier.width(width).height(22.dp)) {
        val w = size.width
        val h = size.height
        val third = w / 6f
        val col = Color.White.copy(alpha = 0.16f)
        val midY = h * 0.55f
        drawLine(col, Offset(w / 2f, 0f), Offset(w / 2f, midY), strokeWidth = 1.3f)
        drawLine(col, Offset(third, midY), Offset(w - third, midY), strokeWidth = 1.3f)
        listOf(third, w / 2f, w - third).forEach { x ->
            drawLine(col, Offset(x, midY), Offset(x, h), strokeWidth = 1.3f)
        }
    }
}

/** Gem kalitini ras mga aylantiradi. */
fun gemDrawable(key: String): DrawableResource = when (key) {
    "first" -> Res.drawable.gem_first
    "motivated" -> Res.drawable.gem_motivated
    "night-owl" -> Res.drawable.gem_night_owl
    "pride" -> Res.drawable.gem_pride
    "iron-will" -> Res.drawable.gem_iron_will
    "opal-plus" -> Res.drawable.gem_opal_plus
    "time-lord" -> Res.drawable.gem_time_lord
    "century" -> Res.drawable.gem_century
    else -> Res.drawable.gem_first
}

/** Haqiqiy tosh rasmi (mix-blend-screen effekti bilan). */
@Composable
fun GemImage(gemKey: String, size: Dp, unlocked: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(gemDrawable(gemKey)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                alpha = if (unlocked) 1f else 0.3f
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        center = center,
                        radius = this.size.minDimension * 0.62f
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    )
}
