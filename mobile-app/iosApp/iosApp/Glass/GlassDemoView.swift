//
//  GlassDemoView.swift
//  iosApp
//
//  Runnable reference implementation of the glass system:
//  colorful drifting backdrop + glass header + glass cards + the
//  Telegram-style glass tab bar with crossfaded pages.
//
//  ▸ PREVIEW: open in Xcode and use the preview canvas on
//    `GlassDemoView_Previews` (no need to touch App.swift / Compose).
//  ▸ INTEGRATION: to run it as the app root, temporarily swap
//    `ContentView()` for `GlassDemoView()` in App.swift.
//
//  ▸ THE PATTERN for sharing feature code across platforms:
//    platform-neutral logic (models, view models, presets) lives in
//    files WITHOUT any #if; every UIKit/SwiftUI touchpoint is wrapped:
//
//        #if os(iOS)
//            // SwiftUI + glass — compiled into the iosApp target
//        #endif
//        // Android equivalent lives in mobile-app/composeApp (Kotlin)
//

import SwiftUI
import UIKit

#if os(iOS)

public struct GlassDemoView: View {

    @State private var selection: GlassTab = .home

    public init() {}

    public var body: some View {
        ZStack {
            // Glass needs busy, colorful content BEHIND it to be
            // visible — never judge glass over a flat white canvas.
            GlassBackdrop()

            VStack(spacing: 16) {
                header
                Spacer()
                page(for: selection)
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 4)
            // Crossfade driver — scoped to the pages container ONLY,
            // so it never fights the tab bar's own spring.
            .animation(GlassStyle.spring, value: selection)

            VStack {
                Spacer()
                GlassTabBar(selection: $selection)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 8)
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Gradient(colors: [.orange, .pink]))
                .frame(width: 44, height: 44)
                .overlay(
                    Text("A")
                        .font(.headline)
                        .foregroundColor(.white)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text("Aziz Karimov")
                    .font(.headline)
                Text("@aziz.dev · 2h 14m saved today")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(14)
        .glassEffect()
    }

    // MARK: - Pages (Telegram does a fast crossfade — never a push)

    @ViewBuilder
    private func page(for tab: GlassTab) -> some View {
        switch tab {
        case .home:    homePage
        case .focus:   focusPage
        case .stats:   statsPage
        case .apps:    appsPage
        case .profile: profilePage
        }
    }

    private func card(_ title: String, _ value: String, _ icon: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 21, weight: .semibold))
                .frame(width: 44, height: 44)
                .background(Circle().fill(Color.white.opacity(0.12)))
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.title3.weight(.bold))
                Text(title)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.bold))
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .glassEffect(material: .thinMaterial)
    }

    private var homePage: some View {
        VStack(spacing: 12) {
            card("Screen time today", "4h 32m", "hourglass")
            card("Saved by blocking", "2h 14m", "leaf.fill")
            card("Focus streak", "12 days", "flame.fill")
        }
        .id(GlassTab.home)
        .transition(.opacity)
    }

    private var focusPage: some View {
        VStack(spacing: 12) {
            card("Deep Focus", "45 min", "brain.head.profile")
            card("Work session", "90 min", "briefcase.fill")
            card("Sleep", "8h 00m", "moon.fill")
        }
        .id(GlassTab.focus)
        .transition(.opacity)
    }

    private var statsPage: some View {
        VStack(spacing: 12) {
            card("Weekly focus", "18h 06m", "chart.bar.fill")
            card("Best day", "Tue · 3h 40m", "star.fill")
            card("Blocked attempts", "27", "shield.fill")
        }
        .id(GlassTab.stats)
        .transition(.opacity)
    }

    private var appsPage: some View {
        VStack(spacing: 12) {
            card("Blocked apps", "10", "lock.fill")
            card("Strict mode", "On", "key.fill")
        }
        .id(GlassTab.apps)
        .transition(.opacity)
    }

    private var profilePage: some View {
        VStack(spacing: 12) {
            card("Gems earned", "7 of 8", "diamond.fill")

            // UIKit escape hatch in action: this chip keeps a DARK
            // blur even in light mode (Telegram side-panel trick).
            Text("Fixed dark blur chip")
                .font(.caption.weight(.semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(
                    BlurEffectView(variant: .fixedDarkUltraThin)
                )
                .clipShape(Capsule())
        }
        .id(GlassTab.profile)
        .transition(.opacity)
    }
}

// MARK: - Drifting color backdrop (glass visibility reference)

struct GlassBackdrop: View {

    @State private var drift = false

    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)

            RadialGradient(
                gradient: Gradient(colors: [Color.teal.opacity(0.55), .clear]),
                center: UnitPoint(x: 0.18, y: 0.12),
                startRadius: 10, endRadius: 520
            )
            .scaleEffect(drift ? 1.15 : 0.95)

            RadialGradient(
                gradient: Gradient(colors: [Color.orange.opacity(0.50), .clear]),
                center: UnitPoint(x: 0.88, y: 0.38),
                startRadius: 10, endRadius: 460
            )
            .scaleEffect(drift ? 0.92 : 1.18)

            RadialGradient(
                gradient: Gradient(colors: [Color.pink.opacity(0.42), .clear]),
                center: UnitPoint(x: 0.50, y: 1.05),
                startRadius: 10, endRadius: 540
            )
            .scaleEffect(drift ? 1.12 : 0.94)
        }
        .ignoresSafeArea()
        .onAppear {
            withAnimation(
                .easeInOut(duration: 12).repeatForever(autoreverses: true)
            ) {
                drift = true
            }
        }
    }
}

// MARK: - Preview

struct GlassDemoView_Previews: PreviewProvider {
    static var previews: some View {
        GlassDemoView()
        GlassDemoView()
            .environment(\.colorScheme, .dark)
    }
}

#endif
