package com.opal.app.glass

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.opal.app.theme.OpalColors
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
 * Shisha panel — iOS'da native UIVisualEffectView blur, Android'da yuqori unumdorlikdagi obsidian glass.
 */
@Composable
expect fun GlassPane(
    modifier: Modifier = Modifier,
    radius: Dp = 26.dp,
    base: Float = 0.07f,
    content: @Composable BoxScope.() -> Unit
)


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
            GlassPane(Modifier, radius = radius) {
                Box(Modifier.padding(padding), content = content)
            }
        }
    } else {
        GlassPane(modifier, radius = radius) {
            Box(Modifier.padding(padding), content = content)
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating pill
        GlassPane(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(68.dp),
            radius = 34.dp,
            base = 0.08f
        ) {
            BoxWithConstraints(Modifier.matchParentSize()) {
                val count = TabKey.entries.size
                val itemW = maxWidth / count
                val pillX by animateDpAsState(
                    targetValue = itemW * selectedIndex,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
                    label = "pillX"
                )

                // Liquid indicator shadow/glow
                Box(
                    Modifier
                        .offset(x = pillX)
                        .width(itemW)
                        .fillMaxHeight()
                        .background(
                            Brush.radialGradient(
                                listOf(OpalColors.Accent.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                )

                // Floating indicator
                Box(
                    Modifier
                        .offset(x = pillX + (itemW - 48.dp) / 2)
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
                            ),
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .border(
                            0.5.dp, Color.White.copy(alpha = 0.25f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                )

                Row(Modifier.fillMaxSize()) {
                    TabKey.entries.forEachIndexed { index, tab ->
                        val selected = index == selectedIndex
                        val tint by animateColorAsState(
                            targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
                            animationSpec = tween(250),
                            label = "tint"
                        )
                        
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelect(index) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            OpalTabIcon(tab, tint, Modifier.size(24.dp))
                            Text(
                                tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = tint,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
