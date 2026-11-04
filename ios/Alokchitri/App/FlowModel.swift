import AlokEngine
import Foundation

/// The shot in progress: what the user wants, and the light and camera limits it will be judged against.
@MainActor
final class FlowModel: ObservableObject {
    @Published var goal: ShotGoal = .portrait
    @Published private(set) var reading: LightReading?
    @Published private(set) var camera: CameraInfo = DeviceProbe.probe()

    func setReading(_ reading: LightReading?) { self.reading = reading }

    func startNewShot() {
        goal = .portrait
        reading = nil
        camera = DeviceProbe.probe()
    }

    /// The recommendation for the current goal and reading, or nil before the light has been measured.
    var result: RecommendationResult? {
        #if targetEnvironment(simulator)
        // The Simulator has no camera: use a typical indoor reading and an iPhone's limits so every screen can be reviewed.
        let reading = reading ?? LightReading(ev100: 6.0)
        let limits = camera.limits ?? Self.sampleLimits
        #else
        guard let reading, let limits = camera.limits else { return nil }
        #endif
        return RecommendationEngine.recommend(goal: goal, reading: reading, device: limits)
    }

    #if targetEnvironment(simulator)
    private static let sampleLimits = DeviceLimits(
        isoMin: 32, isoMax: 3072, shutterMinSeconds: 1.0 / 24000, shutterMaxSeconds: 1.0 / 3,
        aperture: 1.78, lenses: [.main, .ultraWide, .tele], rawSupported: true, hasOis: true
    )
    #endif
}
