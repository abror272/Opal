//
//  GlassStyle.swift
//  iosApp
//
//  Design tokens for the Telegram-style frosted-glass system.
//  Every visual knob lives here — call sites never hard-code values,
//  so the whole app can be re-tuned from one file.
//
//  ─────────────────────────────────────────────────────────────────
//  iOS-ONLY ENFORCEMENT
//  1. This file is wrapped in `#if os(iOS)` — on macOS/watchOS/visionOS
//     builds the compiler removes every symbol below.
//  2. The file physically lives in mobile-app/iosApp/iosApp/Glass/ and
//     is a member of the iosApp Xcode target only (see project.yml).
//     It is never referenced from the shared Kotlin/Compose code in
//     mobile-app/composeApp/, and Android does not use Swift at all —
//     so the glass effect cannot leak to Android by construction.
//  ─────────────────────────────────────────────────────────────────
//

import SwiftUI

#if os(iOS)

public enum GlassStyle {

    // ───────────────────────────────────────────────────────────────
    //  BLUR INTENSITY
    //  Ladder (most see-through → most opaque). All are hardware-
    //  accelerated system backdrops (UIBackdropEffectView on the GPU):
    //    .ultraThinMaterial  ← Telegram nav/tab bars  (default here)
    //    .thinMaterial       ← floating cards over colorful content
    //    .regularMaterial    ← dialogs / sheets
    //    .thickMaterial      ← dense side panels
    //    .bar / .chrome      ← system-chrome level
    // ───────────────────────────────────────────────────────────────
    public static let material: Material = .ultraThinMaterial

    /// A barely-there tint lift so panes read as physical glass even
    /// over flat, single-color backgrounds. Split by color scheme.
    public static let tintLightOpacity: Double = 0.06   // white lift, light mode
    public static let tintDarkOpacity: Double  = 0.12   // black lift, dark mode

    // ───────────────────────────────────────────────────────────────
    //  INNER BORDER — the single most "Telegram" detail.
    //  A hairline, top-lit stroke that makes the pane's edge catch
    //  the light. Tune `borderOpacity`:
    //    0.08  → almost invisible (large full-width sheets)
    //    0.18  → Telegram default (bars, floating pills) ← used here
    //    0.30  → maximum before it starts looking like an outline
    // ───────────────────────────────────────────────────────────────
    public static let borderWidth:   CGFloat = 0.5    // true hairline on 3x screens
    public static let borderOpacity: Double  = 0.18
    /// Top edge brighter than bottom = "light from above" cue.
    public static let borderTopBoost:   Double = 1.0  // top edge multiplier
    public static let borderBottomFade: Double = 0.30 // bottom edge multiplier

    // ───────────────────────────────────────────────────────────────
    //  SPECULAR SHEEN (diagonal light sweep, top-leading → center)
    //  Keep ≤ 0.08 or the glass starts looking like plastic.
    // ───────────────────────────────────────────────────────────────
    public static let sheenOpacity: Double = 0.07

    // ───────────────────────────────────────────────────────────────
    //  DEPTH & SHAPE
    // ───────────────────────────────────────────────────────────────
    public static let shadowColor:   Color   = .black
    public static let shadowOpacity: Double  = 0.14
    public static let shadowRadius:  CGFloat = 18
    public static let shadowY:       CGFloat = 8

    public static let cornerRadius:    CGFloat = 20  // cards, chips
    public static let barCornerRadius: CGFloat = 28  // floating tab bar

    // ───────────────────────────────────────────────────────────────
    //  MOTION — ONE spring for the whole glass system so every pane
    //  moves like a single physical object (that is Telegram's trick).
    // ───────────────────────────────────────────────────────────────
    /// Tab content crossfade, sheet entrances, large surfaces.
    public static let spring: Animation =
        .spring(response: 0.38, dampingFraction: 0.84, blendDuration: 0.1)

    /// Small controls: pill slide, icon bounce, press feedback.
    public static let springSnappy: Animation =
        .spring(response: 0.32, dampingFraction: 0.72, blendDuration: 0)
}

#endif
