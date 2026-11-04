import CoreText
import SwiftUI
import UIKit

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: opacity
        )
    }
}

/// Tokens from the design canvas, the same values as the Android app's AlokColors. Dark only in v1.
enum AlokColors {
    static let ground = Color(hex: 0x0E1014)        // screen background
    static let surface = Color(hex: 0x171A21)       // cards, tab bar, sheets
    static let surfaceRaised = Color(hex: 0x20242D) // icon tiles, inset banners
    static let outline = Color(hex: 0x2B303B)       // borders, dividers
    static let outlineStrong = Color(hex: 0x3A404D)

    static let text = Color(hex: 0xF3F1EC)
    static let textMuted = Color(hex: 0xA3A9B5)

    static let amber = Color(hex: 0xFFB547)         // light, primary action, values
    static let amberTint = Color(hex: 0x3A2D12)     // selected chip, tab indicator
    static let onAmber = Color(hex: 0x1A1200)

    static let green = Color(hex: 0x4FD6A5)         // done, supported, free quota
    static let viewfinder = Color(hex: 0x12151B)
    static let viewfinderDeep = Color(hex: 0x07080B)
}

/// Card 24, hero 28, sheet 32, banner 20, tile 16. Buttons and chips are capsules.
enum AlokRadius {
    static let card: CGFloat = 24
    static let hero: CGFloat = 28
    static let sheet: CGFloat = 32
    static let banner: CGFloat = 20
    static let tile: CGFloat = 16
}

enum AlokSpacing {
    static let screen: CGFloat = 20
    static let cardGap: CGFloat = 12
    static let minTouch: CGFloat = 48
}

/// Hind Siliguri covers Bangla and Latin. The TTFs are bundled (same files as Android) and registered at launch,
/// so no Info.plist font list is needed.
enum AlokFonts {
    static func register() {
        let urls = (Bundle.main.urls(forResourcesWithExtension: "ttf", subdirectory: nil) ?? [])
            + (Bundle.main.urls(forResourcesWithExtension: "ttf", subdirectory: "Fonts") ?? [])
        for url in urls {
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }

    static func name(_ weight: Font.Weight) -> String {
        switch weight {
        case .bold, .heavy, .black: "HindSiliguri-Bold"
        case .semibold: "HindSiliguri-SemiBold"
        case .medium: "HindSiliguri-Medium"
        default: "HindSiliguri-Regular"
        }
    }
}

extension Font {
    /// Hind Siliguri at `size` points, scaling with Dynamic Type like the system font.
    static func hind(_ size: CGFloat, _ weight: Font.Weight = .regular, relativeTo style: Font.TextStyle = .body) -> Font {
        .custom(AlokFonts.name(weight), size: size, relativeTo: style)
    }
}

/// The spec's type scale. Never below 13 pt. Bangla needs generous line height: pair body styles with `.lineSpacing(4)`.
enum AlokType {
    static let display = Font.hind(32, .bold, relativeTo: .largeTitle)
    static let headline = Font.hind(30, .bold, relativeTo: .title)
    static let headlineSmall = Font.hind(26, .bold, relativeTo: .title2)
    static let title = Font.hind(20, .bold, relativeTo: .title3)
    static let titleMedium = Font.hind(19, .bold, relativeTo: .headline)
    static let bodyLarge = Font.hind(17, .regular, relativeTo: .body)
    static let body = Font.hind(16, .regular, relativeTo: .body)
    static let bodySmall = Font.hind(14, .regular, relativeTo: .subheadline)
    static let button = Font.hind(18, .bold, relativeTo: .headline)
    static let label = Font.hind(15, .semibold, relativeTo: .subheadline)
    static let caption = Font.hind(13, .medium, relativeTo: .footnote)
}

/// UIKit bars that SwiftUI does not style on its own.
enum AlokAppearance {
    static func apply() {
        let tab = UITabBarAppearance()
        tab.configureWithOpaqueBackground()
        tab.backgroundColor = UIColor(AlokColors.surface)
        tab.shadowColor = UIColor(AlokColors.outline)
        let item = UITabBarItemAppearance()
        // Colours only: the system font keeps its own line height. Hind Siliguri is taller (Bangla vowel marks)
        // and made the labels sit on top of the icons in the iOS 27 tab bar.
        item.normal.iconColor = UIColor(AlokColors.textMuted)
        item.normal.titleTextAttributes = [.foregroundColor: UIColor(AlokColors.textMuted)]
        item.selected.iconColor = UIColor(AlokColors.amber)
        item.selected.titleTextAttributes = [.foregroundColor: UIColor(AlokColors.text)]
        tab.stackedLayoutAppearance = item
        tab.inlineLayoutAppearance = item
        tab.compactInlineLayoutAppearance = item
        UITabBar.appearance().standardAppearance = tab
        UITabBar.appearance().scrollEdgeAppearance = tab
    }
}
