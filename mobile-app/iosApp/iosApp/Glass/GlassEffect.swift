//
//  GlassEffect.swift
//  iosApp
//
//  `.glassEffect()` — makes any view a Telegram-style frosted pane:
//  hardware-accelerated blur + tint lift + specular sheen + a thin,
//  top-lit hairline border + soft depth shadow.
//
//  PERFORMANCE CONTRACT
//  ────────────────────
//  The blur is a SwiftUI `Material`, backed by the system backdrop
//  view (UIBackdropEffectView). It is:
//    • composited on the GPU — one backdrop sample per frame,
//    • automatically paused when fully covered by opaque content,
//    • NOT a CIFilter. Never fake glass with `.blur(radius:)` on
//      content: `.blur()` is a raster filter over YOUR view tree and
//      it *will* drop frames while scrolling. A Material samples what
//      is BEHIND it — that is real glassmorphism; `.blur()` is not.
//
//  iOS-ONLY: the entire file is inside `#if os(iOS)`. The symbol does
//  not exist on any other platform; cross-platform feature code must
//  call it inside `#if os(iOS)` (see GlassDemoView.swift for the
//  pattern). Android uses the Compose counterpart (OpalGlass.kt).
//

import SwiftUI

#if os(iOS)

// MARK: - ViewModifier

public struct GlassEffect: ViewModifier {

    public var cornerRadius: CGFloat
    public var material: Material
    public var borderOpacity: Double
    public var shadowOpacity: Double

    @Environment(\.colorScheme) private var colorScheme

    public init(
        cornerRadius: CGFloat = GlassStyle.cornerRadius,
        material: Material = GlassStyle.material,
        borderOpacity: Double = GlassStyle.borderOpacity,
        shadowOpacity: Double = GlassStyle.shadowOpacity
    ) {
        self.cornerRadius = cornerRadius
        self.material = material
        self.borderOpacity = borderOpacity
        self.shadowOpacity = shadowOpacity
    }

    public func body(content: Content) -> some View {
        let tint = colorScheme == .dark
            ? Color.black.opacity(GlassStyle.tintDarkOpacity)
            : Color.white.opacity(GlassStyle.tintLightOpacity)

        return content
            .background {
                ZStack {
                    // 1 ── The frosted blur itself (GPU backdrop).
                    //      `style: .continuous` = iOS squircle corners.
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .fill(material)

                    // 2 ── Tint lift so the pane stays readable over
                    //      both bright and dark backdrops.
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .fill(tint)

                    // 3 ── Specular sheen sweeping from top-leading.
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color.white.opacity(GlassStyle.sheenOpacity),
                            .clear
                        ]),
                        startPoint: .topLeading,
                        endPoint: .center
                    )

                    // 4 ── Hairline inner border, lit from above.
                    //      This is the detail that "sells" the glass —
                    //      keep it at 0.5pt, never thicker.
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .strokeBorder(
                            LinearGradient(
                                gradient: Gradient(stops: [
                                    .init(
                                        color: .white.opacity(
                                            borderOpacity * GlassStyle.borderTopBoost
                                        ),
                                        location: 0
                                    ),
                                    .init(
                                        color: .white.opacity(borderOpacity),
                                        location: 0.55
                                    ),
                                    .init(
                                        color: .white.opacity(
                                            borderOpacity * GlassStyle.borderBottomFade
                                        ),
                                        location: 1
                                    )
                                ]),
                                startPoint: .top,
                                endPoint: .bottom
                            ),
                            lineWidth: GlassStyle.borderWidth
                        )
                }
                .shadow(
                    color: GlassStyle.shadowColor.opacity(shadowOpacity),
                    radius: GlassStyle.shadowRadius,
                    y: GlassStyle.shadowY
                )
            }
    }
}

// MARK: - View extension

extension View {

    /// Applies a Telegram-style frosted-glass pane behind `self`.
    ///
    ///     Text("Deep Focus · 45 min")
    ///         .font(.headline)
    ///         .padding(20)
    ///         .glassEffect()
    ///
    ///     sessionCard
    ///         .glassEffect(material: .thinMaterial, cornerRadius: 24)
    ///
    /// - Note: **iOS only** — guarded by `#if os(iOS)`.
    public func glassEffect(
        cornerRadius: CGFloat = GlassStyle.cornerRadius,
        material: Material = GlassStyle.material,
        borderOpacity: Double = GlassStyle.borderOpacity,
        shadowOpacity: Double = GlassStyle.shadowOpacity
    ) -> some View {
        modifier(
            GlassEffect(
                cornerRadius: cornerRadius,
                material: material,
                borderOpacity: borderOpacity,
                shadowOpacity: shadowOpacity
            )
        )
    }
}

// MARK: - Press feedback for interactive glass

/// Telegram-style press reaction: the pane compresses ~3% and dims
/// slightly, then springs back. Pair with `.buttonStyle(GlassButtonStyle())`.
public struct GlassButtonStyle: ButtonStyle {

    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .opacity(configuration.isPressed ? 0.92 : 1.0)
            .animation(GlassStyle.springSnappy, value: configuration.isPressed)
    }
}

#endif
