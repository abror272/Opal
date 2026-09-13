package com.opal.app.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opal.app.ui.TabKey
import com.opal.app.ui.OpalTabIcon

/**
 * ============================================
 *  GLASS EFFECT — expect/actual
 * ============================================
 * iOS: haqiqiy UIVisualEffectView (Telegram'dagi kabi ultra-thin material).
 * Android: gradient + specular highlight bilan "glass" imitatsiya.
 */

/** Pastki navigatsiya paneli — iOS'da native glass, Android'da Compose glass. */
@Composable
expect fun GlassTabBar(selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)

/** Tab o'tish paytidagi frosted "veil" (butun ekranni qopladi va erib ketadi). */
@Composable
expect fun GlassVeil(alpha: Float, modifier: Modifier = Modifier)

/**
 * Umumiy shisha panel (ikkala platforma bir xil ko'rinadi).
 * Qorong'u fon ustida: yarim shaffof gradient + specular tepa nur + hairline border.
 */
@Composable
fun GlassPane(
    modifier: Modifier = Modifier,
    radius: Dp = 26.dp,
    base: Float = 0.07f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = base + 0.025f),
                        Color.White.copy(alpha = base * 0.55f)
                    )
                )
            )
            .border(
                BorderStroke(
                    0.5.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                    )
                ),
                shape
            )
    ) {
        // tepadagi specular nur
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.07f),
                        0.22f to Color.Transparent
                    )
                )
        )
        content()
    }
}

/** Karta shaklida GlassPane + ichki padding. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 26.dp,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (onClick != null) {
        Pressable(onClick = onClick, modifier = modifier) {
            GlassPane(Modifier.matchParentSize(), radius = radius) {
                Box(Modifier.matchParentSize().padding(padding), content = content)
            }
        }
    } else {
        GlassPane(modifier, radius = radius) {
            Box(Modifier.matchParentSize().padding(padding), content = content)
        }
    }
}

/** Bosilganda silliq masshtablanadigan konteyner (micro-interaction). */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressedScale: Float = 0.97f,
    content: @Composable BoxScope.() -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "pressScale"
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        content = content
    )
}

/**
 * Compose'da chizilgan glass tab bar — Android uchun asosiy implementatsiya.
 * iOS'da esa bu o'rniga native UIKit glass bar ishlaydi (Glass.ios.kt).
 */
@Composable
fun ComposeGlassTabBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPane(modifier = modifier, radius = 30.dp, base = 0.06f) {
        BoxWithConstraints(Modifier.matchParentSize()) {
            val count = TabKey.entries.size
            val itemW = maxWidth / count
            val pillX by animateDpAsState(
                targetValue = itemW * selectedIndex,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
                label = "pillX"
            )

            // Silliq sirg'aluvchi "liquid glass" pill
            Box(
                Modifier
                    .offset(x = pillX + 4.dp)
                    .width(itemW - 8.dp)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.07f))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                        RoundedCornerShape(20.dp)
                    )
            )

            Row(Modifier.fillMaxSize()) {
                TabKey.entries.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1f,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
                        label = "iconScale"
                    )
                    val tint by animateColorAsState(
                        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.42f),
                        animationSpec = tween(240),
                        label = "tint"
                    )
                    val src = remember { MutableInteractionSource() }
                    val pressed by src.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (pressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
                        label = "tabPress"
                    )

                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .clickable(interactionSource = src, indication = null) { onSelect(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        ) {
                            OpalTabIcon(tab, tint, Modifier.size(23.dp))
                        }
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
                        ) {
                            Text(
                                tab.title,
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
