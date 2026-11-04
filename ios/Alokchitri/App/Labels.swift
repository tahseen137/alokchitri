import Foundation
import AlokEngine

/// Engine enums -> string keys. The engine has no wording; it all lives in Localizable.xcstrings.
extension LensType {
    func labelKey(withZoom: Bool = true) -> String {
        switch self {
        case .main: withZoom ? "lens_main_x" : "lens_main"
        case .ultraWide: withZoom ? "lens_ultra_wide_x" : "lens_ultra_wide"
        case .tele: withZoom ? "lens_tele_x" : "lens_tele"
        }
    }

    /// Ultra-wide, main, tele: the order lenses are shown in.
    var sortOrder: Int {
        switch self {
        case .ultraWide: 0
        case .main: 1
        case .tele: 2
        }
    }
}

extension LightLevel {
    var labelKey: String {
        switch self {
        case .veryDark: "level_very_dark"
        case .low: "level_low"
        case .medium: "level_medium"
        case .bright: "level_bright"
        case .sunny: "level_sunny"
        }
    }
}

extension Stability {
    var labelKey: String {
        switch self {
        case .handheldOk: "stab_ok"
        case .brace: "stab_brace"
        case .tripodRecommended: "stab_tripod"
        }
    }
}

extension FocusMode {
    var labelKey: String { self == .afSingle ? "focus_afs" : "focus_afc" }
}

extension CaptureFormat {
    var labelKey: String { self == .raw ? "format_raw" : "format_jpeg" }
}

extension Advice {
    var textKey: String {
        switch self {
        case .preferNightMode: "advice_night"
        case .preferAuto: "advice_auto"
        case .overexposureRisk: "advice_over"
        case .underexposureRisk: "advice_under"
        case .highNoise: "advice_noise"
        case .motionBlurRisk: "advice_blur"
        }
    }
}

extension ShotGoal {
    var titleKey: String {
        switch self {
        case .portrait: "goal_portrait"
        case .foodProduct: "goal_food"
        case .indoorLowLight: "goal_indoor"
        case .motion: "goal_motion"
        case .nightCity: "goal_night"
        case .landscapeSunset: "goal_landscape"
        }
    }

    var systemImage: String {
        switch self {
        case .portrait: "person"
        case .foodProduct: "fork.knife"
        case .indoorLowLight: "lightbulb"
        case .motion: "figure.run"
        case .nightCity: "building.2"
        case .landscapeSunset: "mountain.2"
        }
    }
}

/// "+0.3", "0.0", "-0.7"
func formatEv(_ bias: Double) -> String {
    if bias > 0.001 { return String(format: "+%.1f", bias) }
    if bias < -0.001 { return String(format: "%.1f", bias) }
    return "0.0"
}

extension Recommendation {
    /// The four values shown as chips on a saved recipe.
    var chips: [String] { ["ISO \(iso)", shutterLabel, "\(whiteBalanceK)K", formatEv(evBias)] }
}
