// Swift port of engine/src/main/kotlin/com/alokchitri/engine/RecommendationEngine.kt.
// Any rule change goes into both files, then regenerate engine/golden/recommendations.json (see ios/README.md).
import Foundation

/// The rules that turn "what I want" + "how bright it is" + "what my camera can do" into manual settings.
///
/// The exposure maths: for a correctly exposed frame, t = 100 * N^2 / (ISO * 2^EV), where EV is the scene's
/// EV at ISO 100. Each goal picks a shutter speed first (what the picture needs), then derives the lowest ISO
/// that exposes it. If that ISO is too noisy, ISO is capped and the shutter gets slower, which may mean
/// advising a tripod. If the scene is so bright that even base ISO is too much, the shutter gets faster.
public enum RecommendationEngine {
    private static let baseIso = 100

    /// Full stops of shutter speed, seconds. Filtered to what the camera allows.
    private static let shutterStops: [Double] = [
        1.0 / 8000, 1.0 / 4000, 1.0 / 2000, 1.0 / 1000, 1.0 / 500, 1.0 / 250, 1.0 / 125, 1.0 / 60,
        1.0 / 30, 1.0 / 15, 1.0 / 8, 1.0 / 4, 1.0 / 2, 1.0, 2.0, 4.0, 8.0, 15.0, 30.0,
    ]

    /// Third-stop ISO steps that Pro mode sliders typically show.
    private static let isoStops: [Int] = [
        50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500,
        3200, 4000, 5000, 6400, 8000, 10000, 12800,
    ]

    private enum Snap { case nearest, slower }

    public static func recommend(
        goal: ShotGoal,
        reading: LightReading,
        device: DeviceLimits,
        allowRaw: Bool = true
    ) -> RecommendationResult {
        if !device.manualSupported { return .manualUnsupported }

        let profile = GoalProfile.of(goal)
        let level = LightMeter.levelOf(reading.ev100)
        var advice = Set<Advice>()

        let n2 = device.aperture * device.aperture
        let exposureFactor = pow(2.0, reading.ev100 - profile.evBias) // 2^EV, with the goal's bias folded in
        func isoFor(_ t: Double) -> Double { 100.0 * n2 / (t * exposureFactor) }
        func shutterFor(_ iso: Double) -> Double { 100.0 * n2 / (iso * exposureFactor) }

        let isoFloor = Double(max(baseIso, device.isoMin))
        let deviceIsoMax = Double(device.isoMax)
        let isoCeiling = Double(max(min(profile.isoCeiling, device.isoMax), Int(isoFloor)))
        let maxShutter = min(profile.maxShutter, device.shutterMaxSeconds)

        var isoLimit = isoCeiling
        var snap = Snap.nearest
        var shutter = min(profile.preferredShutter, maxShutter)
        var iso = isoFor(shutter)

        if iso < isoFloor {
            // Plenty of light: stay at base ISO and let the shutter get faster.
            iso = isoFloor
            shutter = shutterFor(iso)
        } else if iso > isoCeiling {
            // Too dark for the preferred shutter at a clean ISO: cap ISO, slow the shutter down.
            iso = isoCeiling
            shutter = shutterFor(iso)
            snap = .slower
        }

        var allowPastMaxShutter = false
        if shutter > maxShutter {
            // Even a long shutter is not enough at the ISO ceiling: pin the shutter, push ISO as far as the camera allows.
            shutter = maxShutter
            snap = .nearest
            let needed = isoFor(shutter)
            iso = min(needed, deviceIsoMax)
            isoLimit = deviceIsoMax
            if iso > isoCeiling { advice.insert(.highNoise) }
            if needed > deviceIsoMax {
                if profile.freezesMotion {
                    // Freezing motion is the point of this goal, so we say honestly that we cannot.
                    shutter = min(shutterFor(iso), device.shutterMaxSeconds)
                    snap = .slower
                    allowPastMaxShutter = true
                    advice.insert(.motionBlurRisk)
                } else {
                    advice.insert(.underexposureRisk)
                }
            }
        }

        var overexposed = false
        if shutter < device.shutterMinSeconds {
            shutter = device.shutterMinSeconds
            overexposed = true
            advice.insert(.overexposureRisk)
        }

        // Snap to the steps a Pro mode slider offers.
        let stops = shutterStops.filter {
            $0 >= device.shutterMinSeconds * 0.999 && $0 <= device.shutterMaxSeconds * 1.001
        }
        var finalShutter: Double
        if stops.isEmpty {
            finalShutter = shutter
        } else if snap == .slower {
            finalShutter = stops.first { $0 >= shutter * 0.999 } ?? stops.last!
        } else {
            finalShutter = nearest(shutter, stops)
        }
        if !allowPastMaxShutter && finalShutter > maxShutter * 1.001 {
            finalShutter = stops.last { $0 <= maxShutter * 1.001 } ?? maxShutter
        }

        let isoLow = overexposed ? Double(device.isoMin) : isoFloor
        let isoHigh = max(isoLimit, isoLow)
        let rawIso = min(max(isoFor(finalShutter), isoLow), isoHigh)
        let allowedIso = isoStops.filter { Double($0) >= isoLow * 0.999 && Double($0) <= isoHigh * 1.001 }
        let finalIso = allowedIso.isEmpty
            ? roundToInt(rawIso)
            : roundToInt(nearest(rawIso, allowedIso.map(Double.init)))

        let limit = device.hasOis ? 1.0 / 30 : 1.0 / 60
        let ratio = finalShutter / limit
        let stability: Stability = ratio <= 1.0001 ? .handheldOk : (ratio <= 2.0001 ? .brace : .tripodRecommended)

        if level <= .low && !profile.freezesMotion { advice.insert(.preferNightMode) }
        if level == .sunny && !profile.freezesMotion { advice.insert(.preferAuto) }

        return .success(
            Recommendation(
                iso: finalIso,
                shutterSeconds: finalShutter,
                whiteBalanceK: whiteBalance(goal, reading, level),
                focus: goal == .motion ? .afContinuous : .afSingle,
                evBias: profile.evBias,
                lens: lens(goal, reading.ev100, device),
                format: format(goal, level, device, allowRaw),
                stability: stability,
                advice: advice,
                lightLevel: level
            )
        )
    }

    private static func whiteBalance(_ goal: ShotGoal, _ reading: LightReading, _ level: LightLevel) -> Int {
        // Sunsets keep their warmth under daylight white balance, so never "correct" them.
        if goal == .landscapeSunset { return 5600 }
        if let k = reading.colorTempK {
            return min(max(roundToInt(Double(k) / 100.0) * 100, 2500), 8000)
        }
        switch level {
        case .veryDark: return 3800
        case .low: return 4200
        case .medium: return 4800
        case .bright: return 5500
        case .sunny: return 5600
        }
    }

    private static func lens(_ goal: ShotGoal, _ ev100: Double, _ device: DeviceLimits) -> LensType {
        // Telephoto sensors are small and dim: only use them when there is enough light.
        if goal == .portrait && device.lenses.contains(.tele) && ev100 >= 9.0 { return .tele }
        if goal == .landscapeSunset && device.lenses.contains(.ultraWide) && ev100 >= 9.0 { return .ultraWide }
        return .main
    }

    /// RAW only where it pays: very high contrast (sunset) or very low light, and only if the user allows it.
    private static func format(_ goal: ShotGoal, _ level: LightLevel, _ device: DeviceLimits, _ allowRaw: Bool) -> CaptureFormat {
        allowRaw && device.rawSupported && (goal == .landscapeSunset || level <= .low) ? .raw : .jpeg
    }

    /// Closest value in log space, which is how stops work. Ties go to the first, like Kotlin's minByOrNull.
    private static func nearest(_ value: Double, _ stops: [Double]) -> Double {
        var best = stops[0]
        var bestDistance = abs(log(value / best))
        for stop in stops.dropFirst() {
            let d = abs(log(value / stop))
            if d < bestDistance { best = stop; bestDistance = d }
        }
        return best
    }
}

struct GoalProfile {
    /// The shutter this kind of picture wants, seconds.
    let preferredShutter: Double
    /// Exposure bias in stops: + brighter, - darker.
    let evBias: Double
    /// The noisiest ISO we are happy to recommend.
    let isoCeiling: Int
    /// The slowest shutter we will recommend. Slower than this, ISO goes up instead.
    let maxShutter: Double
    var freezesMotion: Bool = false

    static func of(_ goal: ShotGoal) -> GoalProfile {
        switch goal {
        case .portrait: GoalProfile(preferredShutter: 1.0 / 125, evBias: 0.3, isoCeiling: 1600, maxShutter: 1.0 / 15)
        case .foodProduct: GoalProfile(preferredShutter: 1.0 / 60, evBias: 0.3, isoCeiling: 1600, maxShutter: 1.0 / 8)
        case .indoorLowLight: GoalProfile(preferredShutter: 1.0 / 60, evBias: 0.3, isoCeiling: 1600, maxShutter: 1.0 / 8)
        case .motion: GoalProfile(preferredShutter: 1.0 / 500, evBias: 0.0, isoCeiling: 3200, maxShutter: 1.0 / 250, freezesMotion: true)
        case .nightCity: GoalProfile(preferredShutter: 1.0 / 30, evBias: -0.3, isoCeiling: 1600, maxShutter: 8.0)
        case .landscapeSunset: GoalProfile(preferredShutter: 1.0 / 125, evBias: -0.7, isoCeiling: 400, maxShutter: 2.0)
        }
    }
}
