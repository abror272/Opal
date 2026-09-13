package com.opal.app.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.cinterop.readValue
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSObject
import platform.UIKit.UIBlurEffect
import platform.UIKit.UIBlurEffectStyle
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIVisualEffectView
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.NSTextAlignment
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightMedium
import platform.objc.sel

/**
 * ============================================================
 *  iOS GLASS — haqiqiy UIVisualEffectView (Telegram iOS'dagi kabi)
 * ============================================================
 *  - Tab bar: SystemUltraThinMaterialDark blur + silliq sirg'aluvchi
 *    frosted pill (SystemThinMaterialLight) + SF Symbols ikonlar.
 *    Blur Compose content ostidagi hammasini jonli xiralashtiradi.
 *  - GlassVeil: tab o'tishida butun ekranni qoplaydigan glass qatlam,
 *    Compose spring animatsiyasi bilan erib ketadi.
 */

private val TAB_SYMBOLS = listOf("house.fill", "timer", "chart.bar.fill", "square.grid.2x2.fill", "person.fill")
private val TAB_TITLES = listOf("Home", "Fokus", "Statistika", "Ilovalar", "Profil")

/** UITapGestureRecognizer action'lari uchun proxy. */
private class TabTapProxy : NSObject() {
    var onTap: (Int) -> Unit = {}

    @ObjCAction
    fun tap(gesture: UITapGestureRecognizer) {
        val tag = gesture.view?.tag?.toInt() ?: -1
        if (tag >= 0) onTap(tag)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class NativeGlassTabBar : UIView {
    @OverrideInit
    constructor(frame: CValue<platform.CoreGraphics.CGRect>) : super(frame)

    private val blur = UIVisualEffectView(
        effect = UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterialDark)
    )

    private val pill = UIVisualEffectView(
        effect = UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleSystemThinMaterialLight)
    ).apply {
        layer.cornerRadius = 21.0
        layer.masksToBounds = true
    }

    private val holders = mutableListOf<UIView>()
    private val icons = mutableListOf<UIImageView>()
    private val labels = mutableListOf<UILabel>()

    private val proxy = TabTapProxy()
    var onSelect: (Int) -> Unit = {}
    var pillPosition: Float = 0f
    private var selectedIndex: Int = 0

    init {
        clipsToBounds = true
        layer.cornerRadius = 30.0
        layer.borderWidth = 0.5
        layer.borderColor = UIColor.whiteColor.colorWithAlphaComponent(0.14).CGColor

        addSubview(blur)
        addSubview(pill)

        for (i in TAB_SYMBOLS.indices) {
            val holder = UIView(frame = CGRectZero.readValue())
            holder.tag = i.toLong()
            holder.userInteractionEnabled = true

            val icon = UIImageView(image = UIImage.systemImageNamed(TAB_SYMBOLS[i]))
            icon.tintColor = UIColor.whiteColor.colorWithAlphaComponent(0.45)
            icon.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit

            val label = UILabel()
            label.text = TAB_TITLES[i]
            label.font = UIFont.systemFontOfSize(9.0, weight = UIFontWeightMedium)
            label.textColor = UIColor.whiteColor.colorWithAlphaComponent(0.9)
            label.textAlignment = NSTextAlignment.NSTextAlignmentCenter
            label.alpha = 0.0

            holder.addSubview(icon)
            holder.addSubview(label)
            addSubview(holder)

            holders += holder
            icons += icon
            labels += label
        }

        proxy.onTap = { index -> onSelect(index) }
        for (holder in holders) {
            holder.addGestureRecognizer(
                UITapGestureRecognizer(target = proxy, action = sel("tap:"))
            )
        }
    }

    fun applySelection(selected: Int) {
        if (selected == selectedIndex) return
        selectedIndex = selected
        for (i in icons.indices) {
            icons[i].tintColor = if (i == selected) {
                UIColor.whiteColor
            } else {
                UIColor.whiteColor.colorWithAlphaComponent(0.45)
            }
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val (w, h) = bounds.useContents { width to height }
        blur.setFrame(CGRectMake(0.0, 0.0, w, h))

        val itemW = w / TAB_SYMBOLS.size.toDouble()

        for (i in icons.indices) {
            holders[i].setFrame(CGRectMake(itemW * i, 4.0, itemW, h - 8.0))

            // Pill markaziga yaqinlik — ikonka silliq ko'tariladi, label erib chiqadi
            val distance = kotlin.math.abs(pillPosition - i).coerceAtMost(1.0).toFloat()
            val weight = (1.0f - distance).coerceIn(0f, 1f)

            icons[i].setFrame(
                CGRectMake(
                    (itemW - 24.0) / 2.0,
                    15.0 - 4.0 * weight.toDouble(),
                    24.0,
                    24.0
                )
            )
            labels[i].setFrame(CGRectMake(0.0, 38.0, itemW, 13.0))
            labels[i].alpha = weight.toDouble()
        }

        val pw = itemW - 10.0
        pill.setFrame(
            CGRectMake(itemW * pillPosition.toDouble() + 5.0, 6.0, pw, h - 12.0)
        )
    }
}

// iOS native glass implementations

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GlassTabBar(selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val progress = remember { Animatable(selectedIndex.toFloat()) }

    LaunchedEffect(selectedIndex) {
        progress.animateTo(
            selectedIndex.toFloat(),
            spring(dampingRatio = 0.72f, stiffness = 330f)
        )
    }

    UIKitView(
        factory = {
            NativeGlassTabBar(frame = CGRectZero.readValue())
        },
        update = { bar ->
            bar.onSelect = onSelect
            bar.applySelection(selectedIndex)
            bar.pillPosition = progress.value
            bar.setNeedsLayout()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    )
}

/**
 * iOS native glass pane implementation using UIVisualEffectView.
 * Provides a blurred background with optional corner radius.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GlassPane(
    modifier: Modifier,
    radius: Dp,
    base: Float,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier) {
        UIKitView(
            factory = {
                UIVisualEffectView(
                    effect = UIBlurEffect.effectWithStyle(
                        UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterialDark
                    )
                ).apply {
                    userInteractionEnabled = false
                }
            },
            update = { view ->
                // Convert Dp to pixel radius
                view.layer.cornerRadius = radius.toPx()
                view.layer.masksToBounds = true
                view.layer.borderWidth = 0.5
                view.layer.borderColor = UIColor.whiteColor.colorWithAlphaComponent(0.14).CGColor
            },
            modifier = Modifier.matchParentSize()
        )
        content()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable


@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GlassVeil(alpha: Float, modifier: Modifier) {
    UIKitView(
        factory = {
            UIVisualEffectView(
                effect = UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterialDark)
            ).apply {
                userInteractionEnabled = false
            }
        },
        update = { view ->
            view.alpha = alpha.coerceIn(0f, 1f).toDouble()
            view.isHidden = alpha < 0.02f
        },
        modifier = modifier
    )
}
