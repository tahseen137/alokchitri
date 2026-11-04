import Foundation
import Testing
@testable import AlokEngine

/// Checks the Swift port against engine/golden/recommendations.txt, which the Android engine's
/// GoldenVectorsTest writes. Kotlin is the source of truth: regenerate there, never here.
struct GoldenVectorsTests {
    /// Keep this list identical to GoldenVectorsTest.kt.
    static let devices: [(String, DeviceLimits)] = [
        ("phone", DeviceLimits(isoMin: 50, isoMax: 6400, shutterMinSeconds: 1.0 / 4000, shutterMaxSeconds: 30.0, aperture: 1.8, lenses: [.main, .ultraWide, .tele], rawSupported: true, hasOis: false)),
        ("narrow", DeviceLimits(isoMin: 100, isoMax: 3200, shutterMinSeconds: 1.0 / 1000, shutterMaxSeconds: 1.0 / 4, aperture: 2.0, lenses: [.main], rawSupported: false, hasOis: true)),
        ("iphone", DeviceLimits(isoMin: 32, isoMax: 3072, shutterMinSeconds: 1.0 / 24000, shutterMaxSeconds: 1.0 / 3, aperture: 1.78, lenses: [.main, .ultraWide, .tele], rawSupported: true, hasOis: true)),
    ]

    static func render() -> String {
        var out = ""
        for (name, device) in devices {
            for goal in ShotGoal.allCases {
                for step in -6...34 { // EV -3.0 to 17.0 in half stops
                    out += line(name, device, goal, Double(step) / 2.0, nil, true)
                }
                out += line(name, device, goal, 7.0, 3480, true)
                out += line(name, device, goal, 1.0, nil, false)
            }
        }
        return out
    }

    private static func line(_ name: String, _ device: DeviceLimits, _ goal: ShotGoal, _ ev: Double, _ ct: Int?, _ allowRaw: Bool) -> String {
        var s = "\(name) \(goal.rawValue) ev=\(String(format: "%.1f", ev)) ct=\(ct.map(String.init) ?? "-") raw=\(allowRaw ? 1 : 0) -> "
        switch RecommendationEngine.recommend(goal: goal, reading: LightReading(ev100: ev, colorTempK: ct), device: device, allowRaw: allowRaw) {
        case .manualUnsupported:
            s += "MANUAL_UNSUPPORTED"
        case .success(let r):
            s += "iso=\(r.iso)"
            s += " shutter=\(String(format: "%.6e", r.shutterSeconds))"
            s += " label=\(r.shutterLabel.replacingOccurrences(of: " ", with: "_"))"
            s += " wb=\(r.whiteBalanceK)"
            s += " bias=\(String(format: "%.1f", r.evBias))"
            s += " focus=\(r.focus.rawValue)"
            s += " lens=\(r.lens.rawValue)"
            s += " format=\(r.format.rawValue)"
            s += " stability=\(r.stability.rawValue)"
            s += " level=\(r.lightLevel.name)"
            let advice = r.orderedAdvice.map(\.rawValue).joined(separator: ",")
            s += " advice=\(advice.isEmpty ? "-" : advice)"
        }
        return s + "\n"
    }

    @Test func matchesTheAndroidEngine() {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent() // AlokEngineTests
            .deletingLastPathComponent() // Tests
            .deletingLastPathComponent() // AlokEngine
            .deletingLastPathComponent() // ios
            .deletingLastPathComponent() // repo root
            .appendingPathComponent("engine/golden/recommendations.txt")
        guard let expected = try? String(contentsOf: url, encoding: .utf8) else {
            Issue.record("Missing \(url.path). Run ./gradlew :engine:test from the repo root to generate it.")
            return
        }
        let actual = Self.render()
        if actual == expected { return }
        let a = actual.split(separator: "\n"), e = expected.split(separator: "\n")
        for (i, (x, y)) in zip(a, e).enumerated() where x != y {
            Issue.record("Line \(i + 1) differs\n  Android: \(y)\n  iOS:     \(x)")
            return
        }
        Issue.record("Line counts differ: Android \(e.count), iOS \(a.count)")
    }
}
