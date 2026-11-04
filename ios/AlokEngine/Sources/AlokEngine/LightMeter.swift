// Swift port of engine/src/main/kotlin/com/alokchitri/engine/LightMeter.kt.
import Foundation

/// Turns the camera's own auto-exposure result into a scene brightness number.
///
/// Auto exposure aims for a mid-grey scene, so the settings it picks tell us how bright the scene is:
/// EV at ISO 100 = log2(N^2 / t) - log2(ISO / 100), where N is the f-number and t the exposure time in seconds.
/// On iPhone read N, t and ISO from AVCaptureDevice (lensAperture, exposureDuration, iso) while exposure bias is 0.
public enum LightMeter {
    public static func ev100(aperture: Double, exposureSeconds: Double, iso: Double) -> Double {
        precondition(aperture > 0 && exposureSeconds > 0 && iso > 0, "aperture, exposure and ISO must be positive")
        return log2(aperture * aperture / exposureSeconds) - log2(iso / 100.0)
    }

    public static func ev100(aperture: Double, exposureNanos: Int64, iso: Int) -> Double {
        ev100(aperture: aperture, exposureSeconds: Double(exposureNanos) / 1e9, iso: Double(iso))
    }

    public static func levelOf(_ ev100: Double) -> LightLevel {
        switch ev100 {
        case ..<3.0: .veryDark
        case ..<6.0: .low
        case ..<9.0: .medium
        case ..<13.0: .bright
        default: .sunny
        }
    }

    /// Smooths the live stream of readings so the gauge does not flicker. alpha 0.2 is about a 5-frame memory.
    public final class Smoother {
        private let alpha: Double
        private var value: Double?

        public init(alpha: Double = 0.2) { self.alpha = alpha }

        @discardableResult
        public func add(_ ev100: Double) -> Double {
            let next = value.map { $0 + alpha * (ev100 - $0) } ?? ev100
            value = next
            return next
        }

        public func reset() { value = nil }
    }
}

/// Kotlin's roundToInt: rounds half up (toward positive infinity), unlike Swift's default half-away-from-zero.
@inline(__always) func roundToInt(_ x: Double) -> Int { Int((x + 0.5).rounded(.down)) }

/// "1/60", "1/4000", "2 s"; 0.5 s is shown as "1/2".
public func formatShutter(_ seconds: Double) -> String {
    if seconds >= 1.0 {
        let whole = roundToInt(seconds)
        if abs(seconds - Double(whole)) < 0.05 { return "\(whole) s" }
        return "\(Double(roundToInt(seconds * 10)) / 10.0) s"
    }
    return "1/\(roundToInt(1.0 / seconds))"
}
