import AVFoundation
import AlokEngine
import SwiftUI

/// Runs the back camera on auto exposure and turns what it picks into a scene brightness.
///
/// Auto exposure aims for a mid-grey scene, so the ISO, shutter and aperture it chooses tell us how bright the
/// scene is (see `LightMeter.ev100`). Nothing is photographed or saved: the session only feeds the preview.
@MainActor
final class LightMeterCamera: NSObject, ObservableObject {
    enum State: Equatable { case idle, running, denied, busy, unavailable }

    @Published private(set) var state: State = .idle
    /// Smoothed EV at ISO 100, nil until the first frame.
    @Published private(set) var ev100: Double?
    @Published private(set) var colorTempK: Int?

    var level: LightLevel? { ev100.map(LightMeter.levelOf) }
    var reading: LightReading? { ev100.map { LightReading(ev100: $0, colorTempK: colorTempK) } }

    let session = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "alokchitri.light-meter")
    private var device: AVCaptureDevice?
    private var observations: [NSKeyValueObservation] = []
    private var notificationTokens: [NSObjectProtocol] = []
    private let smoother = LightMeter.Smoother()
    private var configured = false

    func start() {
        Task {
            switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized:
                run()
            case .notDetermined:
                if await AVCaptureDevice.requestAccess(for: .video) { run() } else { state = .denied }
            default:
                state = .denied
            }
        }
    }

    func stop() {
        let session = session
        sessionQueue.async { if session.isRunning { session.stopRunning() } }
        observations.removeAll()
        notificationTokens.forEach(NotificationCenter.default.removeObserver)
        notificationTokens.removeAll()
        smoother.reset()
        if state == .running || state == .busy { state = .idle }
    }

    private func run() {
        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
            state = .unavailable
            return
        }
        device = camera
        let session = session
        let needsSetup = !configured
        configured = true
        sessionQueue.async {
            if needsSetup {
                session.beginConfiguration()
                session.sessionPreset = .high
                if let input = try? AVCaptureDeviceInput(device: camera), session.canAddInput(input) {
                    session.addInput(input)
                }
                session.commitConfiguration()
            }
            // Meter at exposure bias 0, which is what LightMeter.ev100 assumes.
            if (try? camera.lockForConfiguration()) != nil {
                camera.setExposureTargetBias(0)
                camera.unlockForConfiguration()
            }
            session.startRunning()
        }
        observe(camera)
        state = .running
    }

    private func observe(_ camera: AVCaptureDevice) {
        observations.removeAll()
        // exposureDuration and ISO change together as auto exposure settles; either one triggers a new reading.
        observations.append(camera.observe(\.exposureDuration, options: [.new]) { [weak self] cam, _ in
            self?.sample(cam)
        })
        observations.append(camera.observe(\.iso, options: [.new]) { [weak self] cam, _ in
            self?.sample(cam)
        })

        let center = NotificationCenter.default
        notificationTokens.append(center.addObserver(forName: AVCaptureSession.wasInterruptedNotification, object: session, queue: .main) { [weak self] _ in
            Task { @MainActor in self?.state = .busy }
        })
        notificationTokens.append(center.addObserver(forName: AVCaptureSession.interruptionEndedNotification, object: session, queue: .main) { [weak self] _ in
            Task { @MainActor in self?.state = .running }
        })
    }

    /// KVO fires on a background thread: read the numbers there, then hop to the main actor.
    private nonisolated func sample(_ camera: AVCaptureDevice) {
        let seconds = CMTimeGetSeconds(camera.exposureDuration)
        let iso = Double(camera.iso)
        let aperture = Double(camera.lensAperture)
        guard seconds > 0, iso > 0, aperture > 0 else { return }
        let ev = LightMeter.ev100(aperture: aperture, exposureSeconds: seconds, iso: iso)
        let kelvin = Self.colorTemperature(camera)
        Task { @MainActor [weak self] in
            guard let self else { return }
            ev100 = smoother.add(ev)
            colorTempK = kelvin
        }
    }

    /// Light colour from auto white balance, or nil if the camera has not settled on valid gains yet.
    private nonisolated static func colorTemperature(_ camera: AVCaptureDevice) -> Int? {
        let g = camera.deviceWhiteBalanceGains
        let max = camera.maxWhiteBalanceGain
        guard g.redGain >= 1, g.greenGain >= 1, g.blueGain >= 1, g.redGain <= max, g.greenGain <= max, g.blueGain <= max else { return nil }
        let k = camera.temperatureAndTintValues(for: g).temperature
        return k.isFinite ? Int(k.rounded()) : nil
    }
}

/// The live camera image, filling its frame.
struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }
}
