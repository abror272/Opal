package com.opal.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 5 ta asosiy tab. Tartib GlassTabBar'lar bilan bir xil bo'lishi shart. */
enum class TabKey(val title: String) {
    HOME("Home"),
    FOCUS("Fokus"),
    STATS("Statistika"),
    APPS("Ilovalar"),
    PROFILE("Profil")
}

/**
 * Material-icons kutubxonasiga bog'lanmasdan o'z minimal ikonlarimiz.
 * 24x24 viewport'da chiziladi, masshtablanadi.
 */
@Composable
fun OpalTabIcon(tab: TabKey, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawTabIcon(tab, tint)
    }
}

private fun DrawScope.drawTabIcon(tab: TabKey, c: Color) {
    val s = size.minDimension / 24f

    fun pt(x: Float, y: Float) = Offset(x * s, y * s)

    when (tab) {
        TabKey.HOME -> {
            val path = Path().apply {
                moveTo(3.6f, 11.2f)
                lineTo(12f, 3.8f)
                lineTo(20.4f, 11.2f)
                lineTo(20.4f, 19.2f)
                cubicTo(20.4f, 20.0f, 19.9f, 20.4f, 19.2f, 20.4f)
                lineTo(4.8f, 20.4f)
                cubicTo(4.1f, 20.4f, 3.6f, 20.0f, 3.6f, 19.2f)
                close()
            }
            drawPath(path, c)
            // eshik
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = pt(10.4f, 14.6f),
                size = Size(3.2f * s, 5.8f * s),
                cornerRadius = CornerRadius(1.4f * s)
            )
        }

        TabKey.FOCUS -> {
            drawCircle(
                color = c,
                radius = 7.4f * s,
                center = pt(12f, 13.4f),
                style = Stroke(width = 1.9f * s, cap = StrokeCap.Round)
            )
            drawLine(c, pt(12f, 13.4f), pt(12f, 8.6f), strokeWidth = 1.9f * s, cap = StrokeCap.Round)
            drawLine(c, pt(12f, 13.4f), pt(15.2f, 15.2f), strokeWidth = 1.9f * s, cap = StrokeCap.Round)
            // tepadagi tugma
            drawLine(c, pt(9.8f, 3.4f), pt(14.2f, 3.4f), strokeWidth = 1.9f * s, cap = StrokeCap.Round)
        }

        TabKey.STATS -> {
            drawRoundRect(c, pt(4.2f, 12.4f), Size(3.7f * s, 8f * s), CornerRadius(1.6f * s))
            drawRoundRect(c, pt(10.15f, 6.8f), Size(3.7f * s, 13.6f * s), CornerRadius(1.6f * s))
            drawRoundRect(c, pt(16.1f, 14.8f), Size(3.7f * s, 5.6f * s), CornerRadius(1.6f * s))
        }

        TabKey.APPS -> {
            val r = CornerRadius(2.1f * s)
            drawRoundRect(c, pt(4f, 4f), Size(7.1f * s, 7.1f * s), r)
            drawRoundRect(c, pt(12.9f, 4f), Size(7.1f * s, 7.1f * s), r)
            drawRoundRect(c, pt(4f, 12.9f), Size(7.1f * s, 7.1f * s), r)
            drawRoundRect(c, pt(12.9f, 12.9f), Size(7.1f * s, 7.1f * s), r)
        }

        TabKey.PROFILE -> {
            drawCircle(c, radius = 4.1f * s, center = pt(12f, 7.7f))
            val body = Path().apply {
                moveTo(4.6f, 20.6f)
                cubicTo(4.6f, 15.4f, 8.1f, 13.7f, 12f, 13.7f)
                cubicTo(15.9f, 13.7f, 19.4f, 15.4f, 19.4f, 20.6f)
                close()
            }
            drawPath(body, c)
        }
    }
}

/** Kichik util — Dp bo'sh joy uchun. */
fun defaultIconSize(): Dp = 24.dp
