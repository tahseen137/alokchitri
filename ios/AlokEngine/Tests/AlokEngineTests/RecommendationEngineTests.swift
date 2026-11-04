// Swift port of engine/src/test/kotlin/com/alokchitri/engine/RecommendationEngineTest.kt.
import XCTest
@testable import AlokEngine

final class RecommendationEngineTests: XCTestCase {
    /// A typical mid-range phone: f/1.8, ISO 50-6400, 1/4000 s to 30 s, three lenses, RAW, no OIS.
    let phone = DeviceLimits(
        isoMin: 50, isoMax: 6400, shutterMinSeconds: 1.0 / 4000, shutterMaxSeconds: 30.0, aperture: 1.8,
        lenses: [.main, .ultraWide, .tele], rawSupported: true, hasOis: false
    )

    private func run(_ goal: ShotGoal, ev: Double, device: DeviceLimits? = nil, colorTempK: Int? = nil, allowRaw: Bool = true,
                     file: StaticString = #filePath, line: UInt = #line) -> Recommendation {
        let result = RecommendationEngine.recommend(goal: goal, reading: LightReading(ev100: ev, colorTempK: colorTempK), device: device ?? phone, allowRaw: allowRaw)
        guard case .success(let r) = result else {
            XCTFail("expected a recommendation", file: file, line: line)
            fatalError()
        }
        return r
    }

    private func with(_ change: (inout DeviceLimits) -> Void) -> DeviceLimits {
        var d = phone
        change(&d)
        return d
    }

    // MARK: light meter

    func testEv100FromAutoExposureSettings() {
        XCTAssertEqual(LightMeter.ev100(aperture: 1.8, exposureSeconds: 1.0 / 60, iso: 800), 4.6, accuracy: 0.05)
        XCTAssertEqual(LightMeter.ev100(aperture: 16, exposureSeconds: 1.0 / 100, iso: 100), 14.6, accuracy: 0.05)
    }

    func testEv100FromNanosecondsMatchesSeconds() {
        XCTAssertEqual(LightMeter.ev100(aperture: 1.8, exposureSeconds: 1.0 / 60, iso: 800),
                       LightMeter.ev100(aperture: 1.8, exposureNanos: 16_666_667, iso: 800), accuracy: 0.001)
    }

    func testLightLevelsMapToTheFiveGaugeBars() {
        XCTAssertEqual(LightMeter.levelOf(2).gauge, 1)
        XCTAssertEqual(LightMeter.levelOf(5).gauge, 2)
        XCTAssertEqual(LightMeter.levelOf(7).gauge, 3)
        XCTAssertEqual(LightMeter.levelOf(11).gauge, 4)
        XCTAssertEqual(LightMeter.levelOf(14).gauge, 5)
    }

    func testSmoother() {
        let s = LightMeter.Smoother(alpha: 0.5)
        XCTAssertEqual(s.add(10), 10, accuracy: 1e-9)
        XCTAssertEqual(s.add(6), 8, accuracy: 1e-9)
        s.reset()
        XCTAssertEqual(s.add(3), 3, accuracy: 1e-9)
    }

    func testShutterLabels() {
        XCTAssertEqual(formatShutter(1.0 / 60), "1/60")
        XCTAssertEqual(formatShutter(1.0 / 4000), "1/4000")
        XCTAssertEqual(formatShutter(0.5), "1/2")
        XCTAssertEqual(formatShutter(2.0), "2 s")
        XCTAssertEqual(formatShutter(1.5), "1.5 s")
    }

    // MARK: the design's worked example

    func testIndoorLowLightMatchesTheRecommendationScreen() {
        let r = run(.indoorLowLight, ev: 5)
        XCTAssertEqual(r.iso, 800)
        XCTAssertEqual(r.shutterLabel, "1/60")
        XCTAssertEqual(r.whiteBalanceK, 4200)
        XCTAssertEqual(r.evBias, 0.3, accuracy: 1e-9)
        XCTAssertEqual(r.lens, .main)
        XCTAssertEqual(r.focus, .afSingle)
        XCTAssertEqual(r.format, .raw)
        XCTAssertEqual(r.stability, .handheldOk)
        XCTAssertTrue(r.advice.contains(.preferNightMode))
    }

    // MARK: landscape / sunset

    func testSunsetInGoodLight() {
        let r = run(.landscapeSunset, ev: 11)
        XCTAssertEqual(r.iso, 100)
        XCTAssertEqual(r.shutterLabel, "1/1000")
        XCTAssertEqual(r.whiteBalanceK, 5600)
        XCTAssertEqual(r.lens, .ultraWide)
        XCTAssertEqual(r.format, .raw)
        XCTAssertTrue(r.advice.isEmpty)
    }

    func testSunsetWhiteBalanceIgnoresMeasuredColour() {
        XCTAssertEqual(run(.landscapeSunset, ev: 11, colorTempK: 3200).whiteBalanceK, 5600)
    }

    func testTooBrightClampsAndWarns() {
        let r = run(.landscapeSunset, ev: 15)
        XCTAssertEqual(r.iso, 50)
        XCTAssertEqual(r.shutterLabel, "1/4000")
        XCTAssertTrue(r.advice.contains(.overexposureRisk))
        XCTAssertTrue(r.advice.contains(.preferAuto))
    }

    // MARK: motion

    func testMotionInDimIndoorLightKeeps1Over500() {
        let r = run(.motion, ev: 6)
        XCTAssertEqual(r.shutterLabel, "1/500")
        XCTAssertEqual(r.iso, 2500)
        XCTAssertEqual(r.focus, .afContinuous)
        XCTAssertFalse(r.advice.contains(.preferNightMode))
    }

    func testMotionInDarkLightPins1Over250() {
        let r = run(.motion, ev: 4)
        XCTAssertEqual(r.shutterLabel, "1/250")
        XCTAssertEqual(r.iso, 5000)
        XCTAssertTrue(r.advice.contains(.highNoise))
        XCTAssertFalse(r.advice.contains(.motionBlurRisk))
    }

    func testMotionInNearDarknessAdmitsItCannotFreeze() {
        let r = run(.motion, ev: 0)
        XCTAssertTrue(r.advice.contains(.motionBlurRisk))
        XCTAssertGreaterThan(r.shutterSeconds, 1.0 / 250)
        XCTAssertLessThanOrEqual(r.iso, phone.isoMax)
    }

    // MARK: night

    func testDarkNightStreetNeedsATripod() {
        let r = run(.nightCity, ev: 0)
        XCTAssertEqual(r.stability, .tripodRecommended)
        XCTAssertLessThanOrEqual(r.iso, 1600)
        XCTAssertGreaterThanOrEqual(r.shutterSeconds, 1.0 / 8)
        XCTAssertEqual(r.format, .raw)
        XCTAssertTrue(r.advice.contains(.preferNightMode))
    }

    func testStabilisationExtendsHandheldLimit() {
        let noOis = run(.nightCity, ev: 2.5)
        let withOis = run(.nightCity, ev: 2.5, device: with { $0.hasOis = true })
        XCTAssertEqual(noOis.shutterLabel, "1/30")
        XCTAssertEqual(noOis.stability, .brace)
        XCTAssertEqual(withOis.stability, .handheldOk)
    }

    // MARK: lens, format, white balance

    func testPortraitUsesTeleOnlyWithLight() {
        XCTAssertEqual(run(.portrait, ev: 11).lens, .tele)
        XCTAssertEqual(run(.portrait, ev: 6).lens, .main)
        XCTAssertEqual(run(.portrait, ev: 11, device: with { $0.lenses = [.main] }).lens, .main)
    }

    func testNoRawGetsJpeg() {
        XCTAssertEqual(run(.nightCity, ev: 1, device: with { $0.rawSupported = false }).format, .jpeg)
        XCTAssertEqual(run(.nightCity, ev: 1, allowRaw: false).format, .jpeg)
    }

    func testBrightPortraitStaysJpeg() {
        XCTAssertEqual(run(.portrait, ev: 12).format, .jpeg)
    }

    func testMeasuredColourTemperatureRoundedAndClamped() {
        XCTAssertEqual(run(.foodProduct, ev: 7, colorTempK: 3480).whiteBalanceK, 3500)
        XCTAssertEqual(run(.foodProduct, ev: 7, colorTempK: 9300).whiteBalanceK, 8000)
        XCTAssertEqual(run(.foodProduct, ev: 7, colorTempK: 1800).whiteBalanceK, 2500)
    }

    func testBrightDaylightSaysAutoIsFine() {
        XCTAssertTrue(run(.portrait, ev: 14).advice.contains(.preferAuto))
    }

    // MARK: device limits

    func testNoManualControlGetsAClearRefusal() {
        let result = RecommendationEngine.recommend(goal: .portrait, reading: LightReading(ev100: 8), device: with { $0.manualSupported = false })
        XCTAssertEqual(result, .manualUnsupported)
    }

    func testNarrowShutterRangeNeverGetsAnImpossibleShutter() {
        let narrow = with { $0.shutterMinSeconds = 1.0 / 1000; $0.shutterMaxSeconds = 1.0 / 4 }
        for ev in [-2.0, 3, 8, 14] {
            for goal in ShotGoal.allCases {
                let r = run(goal, ev: ev, device: narrow)
                XCTAssertGreaterThanOrEqual(r.shutterSeconds, 1.0 / 1000 * 0.999, "\(goal) ev=\(ev)")
                XCTAssertLessThanOrEqual(r.shutterSeconds, 1.0 / 4 * 1.001, "\(goal) ev=\(ev)")
            }
        }
    }

    // MARK: invariants

    func testEveryGoalAndLevelStaysInsideLimitsOnRealStops() {
        let isoStops: Set<Int> = [50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400]
        for ev in stride(from: -3.0, through: 17.0, by: 0.5) {
            for goal in ShotGoal.allCases {
                let r = run(goal, ev: ev)
                XCTAssertTrue((phone.isoMin...phone.isoMax).contains(r.iso), "\(goal) ev=\(ev) iso=\(r.iso)")
                XCTAssertTrue(isoStops.contains(r.iso), "\(goal) ev=\(ev) iso=\(r.iso)")
                XCTAssertGreaterThanOrEqual(r.shutterSeconds, phone.shutterMinSeconds * 0.999)
                XCTAssertLessThanOrEqual(r.shutterSeconds, phone.shutterMaxSeconds * 1.001)
                XCTAssertTrue((2500...8000).contains(r.whiteBalanceK))
            }
        }
    }

    func testDarkerScenesNeverGetLessExposure() {
        for goal in [ShotGoal.portrait, .foodProduct, .indoorLowLight] {
            var previous = -Double.infinity
            for ev in stride(from: 12.0, through: 2.0, by: -1.0) {
                let r = run(goal, ev: ev)
                let amount = r.shutterSeconds * Double(r.iso)
                XCTAssertGreaterThanOrEqual(amount, previous * 0.5, "\(goal) ev=\(ev)")
                previous = amount
            }
        }
    }
}
