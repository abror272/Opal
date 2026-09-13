# Opal iOS — Telegram-style Glass System

Native SwiftUI glassmorphism for `iosApp`. **iOS only** — every Swift
file is wrapped in `#if os(iOS)` and is a member of the `iosApp` Xcode
target only (`project.yml` globs `sources: path: iosApp`). Android
never touches this code; its Compose counterpart is
`mobile-app/composeApp/.../glass/OpalGlass.kt`.

## Files

| File | What it is |
|---|---|
| `GlassStyle.swift` | All tuning tokens (blur, border, sheen, shadow, springs) |
| `GlassEffect.swift` | `.glassEffect()` modifier + `GlassButtonStyle` press feedback |
| `BlurEffectView.swift` | UIKit escape hatch: fixed-appearance blur + true `UIVibrancyEffect` |
| `GlassTabBar.swift` | Telegram-style floating glass tab bar (matched-geometry pill + haptics) |
| `GlassDemoView.swift` | Runnable demo + the `#if os(iOS)` shared-code pattern |

New `.swift` files are picked up automatically on the next
`xcodegen generate`.

## Fine-tuning (from `GlassStyle.swift`)

**Blur intensity** — swap `GlassStyle.material`:

| Material | Feel | Use for |
|---|---|---|
| `.ultraThinMaterial` | most see-through | nav/tab bars (default) |
| `.thinMaterial` | light frost | floating cards |
| `.regularMaterial` | medium frost | dialogs, sheets |
| `.thickMaterial` | heavy frost | dense panels |
| `.bar` / `.chrome` | near-opaque | system chrome |

**Border opacity** — `GlassStyle.borderOpacity`:
`0.08` nearly invisible (large sheets) · `0.18` Telegram default ·
`0.30` upper bound before it reads as an outline. Always keep
`borderWidth` at `0.5` (true hairline on 3×).

**Other knobs**: `sheenOpacity` (keep ≤ 0.08), `shadowOpacity`/`shadowRadius`
(depth), `borderTopBoost` / `borderBottomFade` (top-lit edge ratio),
`spring` / `springSnappy` (whole-app motion coherence).

## Performance rules

1. **Never** fake glass with `.blur(radius:)` on content — that is a
   raster filter over your view tree and drops frames. Materials are
   GPU backdrops (one sample/frame, auto-paused when covered).
2. Budget: ≤ 4 visible material layers per screen (tab bar + header
   + 2 cards is fine).
3. Animate **geometry** (matched-geometry pill, offset, opacity) —
   never recreate or animate materials themselves.
4. Don't wrap glass in `drawingGroup()` / `compositingGroup()` —
   rasterizing breaks backdrop sampling.
5. Pills/highlights inside glass = plain fills, not nested materials.

## Dark mode

Materials, tint lift and the pill all flip automatically via
`colorScheme`. Fixed-appearance blur is available through
`BlurEffectView(variant: .fixedDarkUltraThin / .fixedLightUltraThin)`.
