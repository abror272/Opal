package com.opal.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opal.app.theme.GemRainbowGradient
import com.opal.app.theme.OpalColors

/**
 * Haqiqiy Opal kristali. 
 * Bir nechta qatlamli faceted Canvas chizish orqali prizma effekti beriladi.
 */
@Composable
fun CrystalGem(modifier: Modifier = Modifier, size: Dp = 220.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "gem")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        // Neon halo glow
        Box(
            Modifier
                .size(size * 0.9f)
                .background(
                    Brush.radialGradient(
                        listOf(OpalColors.Accent.copy(alpha = 0.2f * shimmer), Color.Transparent)
                    )
                )
        )

        Canvas(Modifier.size(size * 0.75f)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val w = this.size.width
            val h = this.size.height

            rotate(rotation, center) {
                // 1. Asosiy kristal formasi (olmos/faceted polygon)
                val crystalPath = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    lineTo(w, h * 0.4f)
                    lineTo(w * 0.8f, h * 0.9f)
                    lineTo(w * 0.2f, h * 0.9f)
                    lineTo(0f, h * 0.4f)
                    close()
                }
                drawPath(crystalPath, GemRainbowGradient, alpha = 0.8f)

                // 2. Facets (qirralar) — yorug'lik sinishi effekti
                drawFacet(w, h, 0.5f, 0f, 0.8f, 0.5f, Color.White.copy(alpha = 0.25f))
                drawFacet(w, h, 0.2f, 0.5f, 0.5f, 0.9f, Color.White.copy(alpha = 0.15f))
                drawFacet(w, h, 0.8f, 0.5f, 0.5f, 0.9f, Color.White.copy(alpha = 0.2f))
                drawFacet(w, h, 0f, 0.4f, 0.5f, 0f, Color.White.copy(alpha = 0.3f))
            }

            // 3. Markaziy "spark" — overlay light
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.45f * shimmer), Color.Transparent),
                    center = center,
                    radius = w * 0.4f
                ),
                blendMode = BlendMode.Overlay
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFacet(
    w: Float, h: Float,
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    color: Color
) {
    val p = Path().apply {
        moveTo(w * x1, h * y1)
        lineTo(w * x2, h * y2)
        lineTo(w * 0.5f, h * 0.5f) // center point for facet
        close()
    }
    drawPath(p, color)
}
