// Swift port of engine/src/main/kotlin/com/alokchitri/engine/Models.kt.
// Keep the two in step: engine/golden/recommendations.json is checked by both test suites.
import Foundation

/// What the user wants from the shot. Matches the six tiles on the Goal screen.
public enum ShotGoal: String, CaseIterable, Codable, Sendable {
    case portrait = "PORTRAIT"
    case foodProduct = "FOOD_PRODUCT"
    case indoorLowLight = "INDOOR_LOW_LIGHT"
    case motion = "MOTION"
    case nightCity = "NIGHT_CITY"
    case landscapeSunset = "LANDSCAPE_SUNSET"
}

public enum LensType: String, CaseIterable, Codable, Sendable {
    case main = "MAIN"
    case ultraWide = "ULTRA_WIDE"
    case tele = "TELE"
}

/// afSingle = "AF-S" (focus once), afContinuous = "AF-C" (keeps tracking).
public enum FocusMode: String, Codable, Sendable {
    case afSingle = "AF_SINGLE"
    case afContinuous = "AF_CONTINUOUS"
}

public enum CaptureFormat: String, Codable, Sendable {
    case jpeg = "JPEG"
    case raw = "RAW"
}

public enum Stability: String, Codable, Sendable {
    case handheldOk = "HANDHELD_OK"
    case brace = "BRACE"
    case tripodRecommended = "TRIPOD_RECOMMENDED"
}

/// Notes the UI turns into a sentence. `allCases` order is the order the UI should show them.
public enum Advice: String, CaseIterable, Codable, Comparable, Sendable {
    case preferNightMode = "PREFER_NIGHT_MODE"
    case preferAuto = "PREFER_AUTO"
    case overexposureRisk = "OVEREXPOSURE_RISK"
    case underexposureRisk = "UNDEREXPOSURE_RISK"
    case highNoise = "HIGH_NOISE"
    case motionBlurRisk = "MOTION_BLUR_RISK"

    private var order: Int { Advice.allCases.firstIndex(of: self)! }
    public static func < (a: Advice, b: Advice) -> Bool { a.order < b.order }
}

/// `gauge` = how many of the 5 bars on the Meter screen are lit.
public enum LightLevel: Int, Codable, Comparable, Sendable {
    case veryDark = 1, low, medium, bright, sunny

    public var gauge: Int { rawValue }
    public var name: String {
        switch self {
        case .veryDark: "VERY_DARK"
        case .low: "LOW"
        case .medium: "MEDIUM"
        case .bright: "BRIGHT"
        case .sunny: "SUNNY"
        }
    }
    public static func < (a: LightLevel, b: LightLevel) -> Bool { a.rawValue < b.rawValue }
}

/// Scene brightness. `ev100` is the exposure value at ISO 100. `colorTempK` is an optional estimate of the light's colour.
public struct LightReading: Sendable, Equatable {
    public var ev100: Double
    public var colorTempK: Int?
    public init(ev100: Double, colorTempK: Int? = nil) {
        self.ev100 = ev100
        self.colorTempK = colorTempK
    }
}

/// What this phone's camera can do. On iPhone it comes from AVCaptureDevice.activeFormat.
public struct DeviceLimits: Sendable, Equatable {
    public static let defaultAperture = 1.8

    public var isoMin: Int
    public var isoMax: Int
    public var shutterMinSeconds: Double
    public var shutterMaxSeconds: Double
    public var aperture: Double
    public var lenses: Set<LensType>
    public var rawSupported: Bool
    public var hasOis: Bool
    public var manualSupported: Bool

    public init(
        isoMin: Int,
        isoMax: Int,
        shutterMinSeconds: Double,
        shutterMaxSeconds: Double,
        aperture: Double = DeviceLimits.defaultAperture,
        lenses: Set<LensType> = [.main],
        rawSupported: Bool = false,
        hasOis: Bool = false,
        manualSupported: Bool = true
    ) {
        self.isoMin = isoMin
        self.isoMax = isoMax
        self.shutterMinSeconds = shutterMinSeconds
        self.shutterMaxSeconds = shutterMaxSeconds
        self.aperture = aperture
        self.lenses = lenses.union([.main])
        self.rawSupported = rawSupported
        self.hasOis = hasOis
        self.manualSupported = manualSupported
    }
}

public struct Recommendation: Sendable, Equatable {
    public var iso: Int
    public var shutterSeconds: Double
    public var whiteBalanceK: Int
    public var focus: FocusMode
    /// The goal's exposure bias (e.g. +0.3 for food). Already baked into `iso` and `shutterSeconds`; show it only as a hint.
    public var evBias: Double
    public var lens: LensType
    public var format: CaptureFormat
    public var stability: Stability
    public var advice: Set<Advice>
    public var lightLevel: LightLevel

    public var shutterLabel: String { formatShutter(shutterSeconds) }
    /// Advice in the order the UI should show it.
    public var orderedAdvice: [Advice] { advice.sorted() }
}

public enum RecommendationResult: Sendable, Equatable {
    case success(Recommendation)
    /// The camera does not allow manual ISO/shutter. The UI should say so and suggest Auto or Night mode.
    case manualUnsupported
}
