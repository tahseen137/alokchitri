// Swift port of engine/src/test/kotlin/com/alokchitri/engine/RecommendationEngineTest.kt.
import Testing
@testable import AlokEngine

struct RecommendationEngineTests {
    /// A typical mid-range phone: f/1.8, ISO 50-6400, 1/4000 s to 30 s, three lenses, RAW, no OIS.
    let phone = DeviceLimits(
        isoMin: 50, isoMax: 6400, shutterMinSeconds: 1.0 / 4000, shutterMaxSeconds: 30.0, aperture: 1.8,
        lenses: [.main, .ultraWide, .tele], rawSupported: true, hasOis: false
    )

    private func run(_ goal: ShotGoal, ev: Double, device: DeviceLimits? = nil, colorTempK: Int? = nil, allowRaw: Bool = true) -> Recommendation {
        let result = RecommendationEngine.recommend(goal: goal, reading: LightReading(ev100: ev, colorTempK: colorTempK), device: device ?? phone, allowRaw: allowRaw)
        guard case .success(let r) = result else { fatalError("expected a recommendation for \(goal) ev=\(ev)") }
        return r
    }

    private func close(_ a: Double, _ b: Double, _ accuracy: Double) -> Bool { abs(a - b) <= accuracy }

    private func with(_ change: (inout DeviceLimits) -> Void) -> DeviceLimits {
        var d = phone
        change(&d)
        return d
    }

    // MARK: light meter

    @Test func testEv100FromAutoExposureSettings() {
        #expect(close(LightMeter.ev100(aperture: 1.8, exposureSeconds: 1.0 / 60, iso: 800), 4.6, 0.05))
        #expect(close(LightMeter.ev100(aperture: 16, exposureSeconds: 1.0 / 100, iso: 100), 14.6, 0.05))
    }

    @Test func testEv100FromNanosecondsMatchesSeconds() {
        #expect(close(LightMeter.ev100(aperture: 1.8, exposureSeconds: 1.0 / 60, iso: 800),
                      LightMeter.ev100(aperture: 1.8, exposureNanos: 16_666_667, iso: 800), 0.001))
    }

    @Test func testLightLevelsMapToTheFiveGaugeBars() {
        #expect(LightMeter.levelOf(2).gauge == 1)
        #expect(LightMeter.levelOf(5).gauge == 2)
        #expect(LightMeter.levelOf(7).gauge == 3)
        #expect(LightMeter.levelOf(11).gauge == 4)
        #expect(LightMeter.levelOf(14).gauge == 5)
    }

    @Test func testSmoother() {
        let s = LightMeter.Smoother(alpha: 0.5)
        #expect(close(s.add(10), 10, 1e-9))
        #expect(close(s.add(6), 8, 1e-9))
        s.reset()
        #expect(close(s.add(3), 3, 1e-9))
    }

    @Test func testShutterLabels() {
        #expect(formatShutter(1.0 / 60) == "1/60")
        #expect(formatShutter(1.0 / 4000) == "1/4000")
        #expect(formatShutter(0.5) == "1/2")
        #expect(formatShutter(2.0) == "2 s")
        #expect(formatShutter(1.5) == "1.5 s")
    }

    // MARK: the design's worked example

    @Test func testIndoorLowLightMatchesTheRecommendationScreen() {
        let r = run(.indoorLowLight, ev: 5)
        #expect(r.iso == 800)
        #expect(r.shutterLabel == "1/60")
        #expect(r.whiteBalanceK == 4200)
        #expect(close(r.evBias, 0.3, 1e-9))
        #expect(r.lens == .main)
        #expect(r.focus == .afSingle)
        #expect(r.format == .raw)
        #expect(r.stability == .handheldOk)
        #expect(r.advice.contains(.preferNightMode))
    }

    // MARK: landscape / sunset

    @Test func testSunsetInGoodLight() {
        let r = run(.landscapeSunset, ev: 11)
        #expect(r.iso == 100)
        #expect(r.shutterLabel == "1/1000")
        #expect(r.whiteBalanceK == 5600)
        #expect(r.lens == .ultraWide)
        #expect(r.format == .raw)
        #expect(r.advice.isEmpty)
    }

    @Test func testSunsetWhiteBalanceIgnoresMeasuredColour() {
        #expect(run(.landscapeSunset, ev: 11, colorTempK: 3200).whiteBalanceK == 5600)
    }

    @Test func testTooBrightClampsAndWarns() {
        let r = run(.landscapeSunset, ev: 15)
        #expect(r.iso == 50)
        #expect(r.shutterLabel == "1/4000")
        #expect(r.advice.contains(.overexposureRisk))
        #expect(r.advice.contains(.preferAuto))
    }

    // MARK: motion

    @Test func testMotionInDimIndoorLightKeeps1Over500() {
        let r = run(.motion, ev: 6)
        #expect(r.shutterLabel == "1/500")
        #expect(r.iso == 2500)
        #expect(r.focus == .afContinuous)
        #expect(!r.advice.contains(.preferNightMode))
    }

    @Test func testMotionInDarkLightPins1Over250() {
        let r = run(.motion, ev: 4)
        #expect(r.shutterLabel == "1/250")
        #expect(r.iso == 5000)
        #expect(r.advice.contains(.highNoise))
        #expect(!r.advice.contains(.motionBlurRisk))
    }

    @Test func testMotionInNearDarknessAdmitsItCannotFreeze() {
        let r = run(.motion, ev: 0)
        #expect(r.advice.contains(.motionBlurRisk))
        #expect(r.shutterSeconds > 1.0 / 250)
        #expect(r.iso <= phone.isoMax)
    }

    // MARK: night

    @Test func testDarkNightStreetNeedsATripod() {
        let r = run(.nightCity, ev: 0)
        #expect(r.stability == .tripodRecommended)
        #expect(r.iso <= 1600)
        #expect(r.shutterSeconds >= 1.0 / 8)
        #expect(r.format == .raw)
        #expect(r.advice.contains(.preferNightMode))
    }

    @Test func testStabilisationExtendsHandheldLimit() {
        let noOis = run(.nightCity, ev: 2.5)
        let withOis = run(.nightCity, ev: 2.5, device: with { $0.hasOis = true })
        #expect(noOis.shutterLabel == "1/30")
        #expect(noOis.stability == .brace)
        #expect(withOis.stability == .handheldOk)
    }

    // MARK: lens, format, white balance

    @Test func testPortraitUsesTeleOnlyWithLight() {
        #expect(run(.portrait, ev: 11).lens == .tele)
        #expect(run(.portrait, ev: 6).lens == .main)
        #expect(run(.portrait, ev: 11, device: with { $0.lenses = [.main] }).lens == .main)
    }

    @Test func testNoRawGetsJpeg() {
        #expect(run(.nightCity, ev: 1, device: with { $0.rawSupported = false }).format == .jpeg)
        #expect(run(.nightCity, ev: 1, allowRaw: false).format == .jpeg)
    }

    @Test func testBrightPortraitStaysJpeg() {
        #expect(run(.portrait, ev: 12).format == .jpeg)
    }

    @Test func testMeasuredColourTemperatureRoundedAndClamped() {
        #expect(run(.foodProduct, ev: 7, colorTempK: 3480).whiteBalanceK == 3500)
        #expect(run(.foodProduct, ev: 7, colorTempK: 9300).whiteBalanceK == 8000)
        #expect(run(.foodProduct, ev: 7, colorTempK: 1800).whiteBalanceK == 2500)
    }

    @Test func testBrightDaylightSaysAutoIsFine() {
        #expect(run(.portrait, ev: 14).advice.contains(.preferAuto))
    }

    // MARK: device limits

    @Test func testNoManualControlGetsAClearRefusal() {
        let result = RecommendationEngine.recommend(goal: .portrait, reading: LightReading(ev100: 8), device: with { $0.manualSupported = false })
        #expect(result == .manualUnsupported)
    }

    @Test func testNarrowShutterRangeNeverGetsAnImpossibleShutter() {
        let narrow = with { $0.shutterMinSeconds = 1.0 / 1000; $0.shutterMaxSeconds = 1.0 / 4 }
        for ev in [-2.0, 3, 8, 14] {
            for goal in ShotGoal.allCases {
                let r = run(goal, ev: ev, device: narrow)
                #expect(r.shutterSeconds >= 1.0 / 1000 * 0.999, Comment(rawValue: "\(goal) ev=\(ev)"))
                #expect(r.shutterSeconds <= 1.0 / 4 * 1.001, Comment(rawValue: "\(goal) ev=\(ev)"))
            }
        }
    }

    // MARK: invariants

    @Test func testEveryGoalAndLevelStaysInsideLimitsOnRealStops() {
        let isoStops: Set<Int> = [50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400]
        for ev in stride(from: -3.0, through: 17.0, by: 0.5) {
            for goal in ShotGoal.allCases {
                let r = run(goal, ev: ev)
                #expect((phone.isoMin...phone.isoMax).contains(r.iso), Comment(rawValue: "\(goal) ev=\(ev) iso=\(r.iso)"))
                #expect(isoStops.contains(r.iso), Comment(rawValue: "\(goal) ev=\(ev) iso=\(r.iso)"))
                #expect(r.shutterSeconds >= phone.shutterMinSeconds * 0.999)
                #expect(r.shutterSeconds <= phone.shutterMaxSeconds * 1.001)
                #expect((2500...8000).contains(r.whiteBalanceK))
            }
        }
    }

    @Test func testDarkerScenesNeverGetLessExposure() {
        for goal in [ShotGoal.portrait, .foodProduct, .indoorLowLight] {
            var previous = -Double.infinity
            for ev in stride(from: 12.0, through: 2.0, by: -1.0) {
                let r = run(goal, ev: ev)
                let amount = r.shutterSeconds * Double(r.iso)
                #expect(amount >= previous * 0.5, Comment(rawValue: "\(goal) ev=\(ev)"))
                previous = amount
            }
        }
    }
}
