//
//  GlassTabBar.swift
//  iosApp
//
//  Telegram-style floating glass tab bar.
//    • Frosted `.ultraThinMaterial` capsule with a top-lit hairline.
//    • The active pill is ONE view that travels between slots via
//      `matchedGeometryEffect` (no crossfading pills) — that single
//      moving geometry is what makes the motion feel continuous.
//    • Icon micro-bounce + label fade use the SAME spring as the
//      pill, so everything reads as one physical object.
//    • Light haptic on every change (Telegram signature).
//
//  iOS-ONLY (`#if os(iOS)`). Android renders its own Compose bar.
//

import SwiftUI
import UIKit // haptics — iOS only, inside the #if below

#if os(iOS)

public enum GlassTab: String, CaseIterable, Identifiable {

    case home, focus, stats, apps, profile

    public var id: String { rawValue }

    var title: String {
        switch self {
        case .home:    return "Home"
        case .focus:   return "Focus"
        case .stats:   return "Stats"
        case .apps:    return "Apps"
        case .profile: return "Profile"
        }
    }

    var icon: String {
        switch self {
        case .home:    return "house"
        case .focus:   return "brain.head.profile"
        case .stats:   return "chart.bar"
        case .apps:    return "square.grid.2x2"
        case .profile: return "person"
        }
    }

    var selectedIcon: String {
        switch self {
        case .home:    return "house.fill"
        case .focus:   return "brain.head.profile" // no .fill variant exists
        case .stats:   return "chart.bar.fill"
        case .apps:    return "square.grid.2x2.fill"
        case .profile: return "person.fill"
        }
    }
}

public struct GlassTabBar: View {

    @Binding private var selection: GlassTab
    @Namespace private var pill
    @Environment(\.colorScheme) private var colorScheme

    public init(selection: Binding<GlassTab>) {
        _selection = selection
    }

    public var body: some View {
        HStack(spacing: 2) {
            ForEach(GlassTab.allCases) { tab in
                button(for: tab)
            }
        }
        .padding(6)
        .glassEffect(
            cornerRadius: GlassStyle.barCornerRadius,
            material: .ultraThinMaterial,
            borderOpacity: 0.16,
            shadowOpacity: 0.20
        )
    }

    private func button(for tab: GlassTab) -> some View {
        let isSelected = selection == tab

        return Button {
            guard selection != tab else { return }
            // Telegram pairs every tab change with a light haptic.
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            withAnimation(GlassStyle.springSnappy) { selection = tab }
        } label: {
            VStack(spacing: 3) {
                Image(systemName: isSelected ? tab.selectedIcon : tab.icon)
                    .font(.system(size: 19, weight: .semibold))
                    // Micro-bounce, same spring as the pill → cohesive.
                    .scaleEffect(isSelected ? 1.08 : 1.0)
                    .animation(GlassStyle.springSnappy, value: isSelected)

                Text(tab.title)
                    .font(.system(size: 10, weight: .semibold))
                    .opacity(isSelected ? 1 : 0)
                    // Fixed height: selecting never reflows layout.
                    .frame(height: 12)
            }
            .foregroundStyle(isSelected ? Color.primary : Color.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 9)
            .contentShape(Capsule())
            .background {
                if isSelected {
                    // The pill is ONE view travelling between slots
                    // (matchedGeometryEffect) — Telegram-smooth.
                    Capsule()
                        .fill(Color.white.opacity(
                            colorScheme == .dark ? 0.10 : 0.16
                        ))
                        .overlay(
                            Capsule().strokeBorder(
                                Color.white.opacity(0.15), lineWidth: 0.5
                            )
                        )
                        .matchedGeometryEffect(id: "activePill", in: pill)
                }
            }
        }
        .buttonStyle(GlassButtonStyle())
        .accessibilityLabel(tab.title)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

#endif
