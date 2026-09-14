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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opal.app.data.AppGraph
import com.opal.app.glass.GlassTabBar
import com.opal.app.glass.GlassVeil
import com.opal.app.platform.rememberMarkOnboarded
import com.opal.app.platform.rememberOnboarded
import com.opal.app.platform.rememberSafePadding
import com.opal.app.theme.OpalColors
import com.opal.app.theme.OpalTheme
import com.opal.app.ui.TabKey
import com.opal.app.ui.components.SessionPill
import com.opal.app.ui.screens.ActiveSessionOverlay
import com.opal.app.ui.screens.AppsScreen
import com.opal.app.ui.screens.BreathingOverlay
import com.opal.app.ui.screens.FocusScreen
import com.opal.app.ui.screens.HomeScreen
import com.opal.app.ui.screens.OnboardingScreen
import com.opal.app.ui.screens.ProfileScreen
import com.opal.app.ui.screens.StatsScreen

/** Home'dan ochiladigan to'liq ekran oynalar. */
private enum class OpalOverlay { PROFILE, STATS }

/**
 * OPAL MOBILE — root. Birinchi ochilishda onboarding, keyin asosiy ilova.
 */
@Composable
fun OpalApp() {
    OpalTheme {
        val onboarded = rememberOnboarded()
        val markOnboarded = rememberMarkOnboarded()
        var showOnboarding by remember { mutableStateOf(!onboarded) }

        if (showOnboarding) {
            OnboardingScreen(
                onDone = {
                    markOnboarded()
                    showOnboarding = false
                }
            )
        } else {
            MainShell()
        }
    }
}

@Composable
private fun MainShell() {
    val insets = rememberSafePadding()
    val repo = remember { AppGraph.repo }
    val sessionCtl = remember { AppGraph.sessions }

    LaunchedEffect(Unit) { repo.refreshAll() }

    // Bloklash xizmati holatini doimiy kuzatish — MIUI uni jimgina o'chirib qo'yadi.
    LaunchedEffect(Unit) {
        while (true) {
            repo.refreshBlockingService()
            kotlinx.coroutines.delay(2000)
        }
    }

    var tab by remember { mutableStateOf(TabKey.HOME) }
    var overlay by remember { mutableStateOf<OpalOverlay?>(null) }
    var breathingOpen by remember { mutableStateOf(false) }

    // Tab o'tish glass veil
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
    // To'liq ekranli sessiya oynasi faqat Taymer tabida; boshqa tablarda — SessionPill.
    val showOverlay = completion != null || (active != null && tab == TabKey.TIMER)

    Box(
        Modifier
            .fillMaxSize()
            .background(OpalColors.Bg)
    ) {
        // dekorativ mint glow blob'lar
        Box(
            Modifier
                .offset(x = (-150).dp, y = (-140).dp)
                .size(400.dp)
                .background(Brush.radialGradient(listOf(Color(0x1FA9E8B8), Color.Transparent)))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 140.dp, y = 180.dp)
                .size(360.dp)
                .background(Brush.radialGradient(listOf(Color(0x147FE7D0), Color.Transparent)))
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
                            onStartFocus = { tab = TabKey.TIMER },
                            onOpenProfile = { overlay = OpalOverlay.PROFILE },
                            onOpenStats = { overlay = OpalOverlay.STATS },
                            onBreathe = { breathingOpen = true },
                            onOpenApps = { tab = TabKey.APPS }
                        )
                        TabKey.APPS -> AppsScreen()
                        TabKey.TIMER -> FocusScreen()
                    }
                }
            }

            // Faol sessiya suzuvchi pill (boshqa tablarda)
            AnimatedVisibility(
                visible = active != null && completion == null && tab != TabKey.TIMER,
                enter = slideInVertically { it } + fadeIn(tween(200)),
                exit = slideOutVertically { it } + fadeOut(tween(180))
            ) {
                Column {
                    SessionPill(
                        controller = sessionCtl,
                        onClick = { tab = TabKey.TIMER },
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(Modifier.height(10.dp))
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

        // Profile overlay
        AnimatedVisibility(
            visible = overlay == OpalOverlay.PROFILE,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(220)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
        ) {
            ProfileScreen(onClose = { overlay = null })
        }

        // Stats (Today) overlay
        AnimatedVisibility(
            visible = overlay == OpalOverlay.STATS,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(220)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
        ) {
            StatsScreen(onClose = { overlay = null })
        }

        // Nafas mashqi overlay
        AnimatedVisibility(
            visible = breathingOpen,
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(200))
        ) {
            BreathingOverlay(onClose = { breathingOpen = false })
        }

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
