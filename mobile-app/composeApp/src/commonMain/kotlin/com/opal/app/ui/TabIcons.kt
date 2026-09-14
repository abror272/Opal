package com.opal.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 3 ta asosiy tab (haqiqiy Opal kabi). Tartib GlassTabBar'lar bilan bir xil bo'lishi shart. */
enum class TabKey(val title: String) {
    HOME("Home"),
    APPS("My Apps"),
    TIMER("Timer")
}

/**
 * Tab ikonkalari — haqiqiy Opal uslubi:
 * Home = halqa (ring), My Apps = 4 nuqta, Timer = play uchburchagi.
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
            // halqa
            drawCircle(
                color = c,
                radius = 8.6f * s,
                center = pt(12f, 12f),
                style = Stroke(width = 2.2f * s, cap = StrokeCap.Round)
            )
        }

        TabKey.APPS -> {
            // 9 nuqta (3x3) — haqiqiy Opal "My Apps" ikonkasi
            val r = 1.9f * s
            listOf(4.4f, 11f, 17.6f).forEach { x ->
                listOf(4.4f, 11f, 17.6f).forEach { y ->
                    drawCircle(c, radius = r, center = pt(x, y))
                }
            }
        }

        TabKey.TIMER -> {
            // play uchburchagi
            val p = Path().apply {
                moveTo(8.5f * s, 5.5f * s)
                lineTo(19f * s, 12f * s)
                lineTo(8.5f * s, 18.5f * s)
                close()
            }
            drawPath(p, c)
        }
    }
}

fun defaultIconSize(): Dp = 24.dp
