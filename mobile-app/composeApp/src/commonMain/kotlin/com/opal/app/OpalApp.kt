package com.opal.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.opal.app.data.AppGraph
import com.opal.app.glass.GlassTabBar
import com.opal.app.glass.GlassVeil
import com.opal.app.platform.rememberSafePadding
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalTheme
import com.opal.app.ui.TabKey
import com.opal.app.ui.screens.ActiveSessionOverlay
import com.opal.app.ui.screens.AppsScreen
import com.opal.app.ui.screens.FocusScreen
import com.opal.app.ui.screens.HomeScreen
import com.opal.app.ui.screens.ProfileScreen
import com.opal.app.ui.screens.StatsScreen

/**
 * OPAL MOBILE — root.
 * Tab o'tish: spring parallax slide + fade + iOS'da haqiqiy glass "veil" erishi.
 */
@Composable
fun OpalApp() {
    OpalTheme {
        val insets = rememberSafePadding()
        val repo = remember { AppGraph.repo }
        val sessionCtl = remember { AppGraph.sessions }

        LaunchedEffect(Unit) { repo.refreshAll() }

        var tab by remember { mutableStateOf(TabKey.HOME) }

        // Tab o'tish glass veil (Telegram uslubi — iOS'da UIVisualEffectView)
        val veil = remember { Animatable(0f) }
        var firstRender by remember { mutableStateOf(true) }
        LaunchedEffect(tab) {
            if (firstRender) {
                firstRender = false
            } else {
                veil.snapTo(1f)
                veil.animateTo(0f, tween(520, easing = CubicBezierEasing(0.33f, 0f, 0.2f, 1f)))
            }
        }

        val active by sessionCtl.active.collectAsState()
        val completion by sessionCtl.completion.collectAsState()
        val showOverlay = active != null || completion != null

        Box(
            Modifier
                .fillMaxSize()
                .background(OpalColors.Bg)
        ) {
            // dekorativ neon glow blob'lar
            Box(
                Modifier
                    .offset(x = (-140).dp, y = (-110).dp)
                    .size(380.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0x2E7C5CFF), Color.Transparent))
                    )
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 130.dp, y = 160.dp)
                    .size(340.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0x26FF7AD9), Color.Transparent))
                    )
            )

            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(insets.calculateTopPadding()))

                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            val enter = slideInHorizontally(
                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 380f),
                                initialOffsetX = { full -> if (forward) full / 4 else -full / 4 }
                            ) + fadeIn(tween(240))
                            val exit = slideOutHorizontally(
                                animationSpec = tween(220),
                                targetOffsetX = { full -> if (forward) -full / 6 else full / 6 }
                            ) + fadeOut(tween(170))
                            enter togetherWith exit
                        },
                        label = "tabContent"
                    ) { key ->
                        when (key) {
                            TabKey.HOME -> HomeScreen(
                                onStartFocus = { tab = TabKey.FOCUS },
                                onOpenStats = { tab = TabKey.STATS }
                            )
                            TabKey.FOCUS -> FocusScreen()
                            TabKey.STATS -> StatsScreen()
                            TabKey.APPS -> AppsScreen()
                            TabKey.PROFILE -> ProfileScreen()
                        }
                    }
                }

                GlassTabBar(
                    selectedIndex = tab.ordinal,
                    onSelect = { index -> tab = TabKey.entries[index] },
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                Spacer(Modifier.height(insets.calculateBottomPadding() + 10.dp))
            }

            // Tab o'tish paytidagi frosted veil
            GlassVeil(alpha = veil.value, modifier = Modifier.fillMaxSize())

            // Faol sessiya overlay
            AnimatedVisibility(
                visible = showOverlay,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = 0.86f, stiffness = 300f),
                    initialOffsetY = { it }
                ) + fadeIn(tween(220)),
                exit = slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { it }
                ) + fadeOut(tween(200))
            ) {
                ActiveSessionOverlay()
            }
        }
    }
}
