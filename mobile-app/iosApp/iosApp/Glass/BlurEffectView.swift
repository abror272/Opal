//
//  BlurEffectView.swift
//  iosApp
//
//  UIKit escape hatches. ~95% of the app should use `.glassEffect()`
//  (SwiftUI Material). Reach for these only when you need:
//    • a FIXED appearance blur — `systemUltraThinMaterialDark` stays
//      dark even in light mode (Telegram's side-panel trick),
//    • pixel-exact system vibrancy for text/icons (UIVibrancyEffect),
//    • UIKit containers (cells, headers) inside UIKit screens.
//
//  PERFORMANCE: UIVisualEffectView is the exact same hardware-backed
//  backdrop the system nav bar uses — one GPU sample per frame.
//
//  iOS-ONLY: UIKit does not exist on other platforms; everything is
//  guarded with `#if os(iOS)`.
//

#if os(iOS)
import SwiftUI
import UIKit

// MARK: - Plain blur with appearance control

public struct BlurEffectView: UIViewRepresentable {

    public enum Variant {
        /// Follows the system color scheme.
        case ultraThin, thin, regular, thick
        /// Always dark, even in light mode.
        case fixedDarkUltraThin
        /// Always light, even in dark mode.
        case fixedLightUltraThin

        var blurEffect: UIBlurEffect {
            switch self {
            case .ultraThin:           return UIBlurEffect(style: .systemUltraThinMaterial)
            case .thin:                return UIBlurEffect(style: .systemThinMaterial)
            case .regular:             return UIBlurEffect(style: .systemMaterial)
            case .thick:               return UIBlurEffect(style: .systemThickMaterial)
            case .fixedDarkUltraThin:  return UIBlurEffect(style: .systemUltraThinMaterialDark)
            case .fixedLightUltraThin: return UIBlurEffect(style: .systemUltraThinMaterialLight)
            }
        }
    }

    public var variant: Variant

    public init(variant: Variant = .ultraThin) {
        self.variant = variant
    }

    public func makeUIView(context: Context) -> UIVisualEffectView {
        UIVisualEffectView(effect: variant.blurEffect)
    }

    public func updateUIView(_ uiView: UIVisualEffectView, context: Context) {
        // ⚠️ To ANIMATE a blur change, wrap the call site in
        // `withAnimation { }` — UIKit animates the `effect` property
        // natively. Never assign `nil` mid-animation (classic flicker).
        if (uiView.effect as? UIBlurEffect)?.style != variant.blurEffect.style {
            uiView.effect = variant.blurEffect
        }
    }
}

// MARK: - True system vibrancy for SwiftUI content

/// Puts SwiftUI content inside a real `UIVibrancyEffect` group so
/// text/icons blend with whatever is behind the glass — exactly how
/// Telegram renders labels on its blurred headers.
///
///     BlurEffectView(variant: .ultraThin)
///         .overlay(
///             VibrancyEffectView(style: .label) {
///                 Text("Today")
///             }
///         )
///
/// Note: SwiftUI's `Material` ALREADY applies automatic vibrancy to
/// `.primary` / `.secondary` foreground styles, so plain
/// `Text(...).foregroundStyle(.primary)` over a material is vibrant
/// for free. Use this wrapper only when you need one of the exact
/// UIKit vibrancy styles (label / fill / separator / …).
public struct VibrancyEffectView<Content: View>: UIViewRepresentable {

    private let style: UIVibrancyEffectStyle
    private let content: Content

    public init(
        style: UIVibrancyEffectStyle = .label,
        @ViewBuilder content: () -> Content
    ) {
        self.style = style
        self.content = content()
    }

    public func makeCoordinator() -> Coordinator { Coordinator() }

    public final class Coordinator {
        var host: UIHostingController<Content>?
    }

    public func makeUIView(context: Context) -> UIVisualEffectView {
        let blur = UIBlurEffect(style: .systemUltraThinMaterial)
        let vibrancyView = UIVisualEffectView(
            effect: UIVibrancyEffect(blurEffect: blur, style: style)
        )

        let host = UIHostingController(rootView: content)
        host.view.backgroundColor = .clear
        host.view.translatesAutoresizingMaskIntoConstraints = false
        context.coordinator.host = host

        vibrancyView.contentView.addSubview(host.view)
        NSLayoutConstraint.activate([
            host.view.leadingAnchor.constraint(
                equalTo: vibrancyView.contentView.leadingAnchor),
            host.view.trailingAnchor.constraint(
                equalTo: vibrancyView.contentView.trailingAnchor),
            host.view.topAnchor.constraint(
                equalTo: vibrancyView.contentView.topAnchor),
            host.view.bottomAnchor.constraint(
                equalTo: vibrancyView.contentView.bottomAnchor)
        ])
        return vibrancyView
    }

    public func updateUIView(_ uiView: UIVisualEffectView, context: Context) {
        // Keep the hosted SwiftUI tree in sync with state changes.
        context.coordinator.host?.rootView = content
    }
}

#endif
