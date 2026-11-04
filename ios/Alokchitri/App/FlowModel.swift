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
        guard let reading, let limits = camera.limits else { return nil }
        return RecommendationEngine.recommend(goal: goal, reading: reading, device: limits)
    }
}
