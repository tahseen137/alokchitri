@preconcurrency import AVFoundation
import AlokEngine
import Photos
import SwiftUI
import UIKit

/// Alokchitri's own Pro camera: opens the back camera with the recommendation already applied (ISO, shutter,
/// white balance, focus, lens) so the user only has to press the shutter. The photo goes to the Photos library.
@MainActor
final class ProCamera: NSObject, ObservableObject {
    enum State: Equatable { case idle, running, denied, busy, unavailable }
    enum SaveResult { case saved, failed, photosDenied }

    @Published private(set) var state: State = .idle
    /// False when this camera refuses manual ISO and shutter: auto exposure is used and the screen says so.
    @Published private(set) var manualExposure = true
    @Published private(set) var capturing = false
    @Published private(set) var lastPhoto: UIImage?

    let session = AVCaptureSession()
    private let output = AVCapturePhotoOutput()
    private let sessionQueue = DispatchQueue(label: "alokchitri.pro-camera")
    private var device: AVCaptureDevice?
    private var configured = false
    private var delegates: [Int64: PhotoDelegate] = [:]
    private var tokens: [NSObjectProtocol] = []

    func start(_ rec: Recommendation) {
        Task {
            switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized: run(rec)
            case .notDetermined:
                if await AVCaptureDevice.requestAccess(for: .video) { run(rec) } else { state = .denied }
            default: state = .denied
            }
        }
    }

    func stop() {
        let session = session
        sessionQueue.async { if session.isRunning { session.stopRunning() } }
        tokens.forEach(NotificationCenter.default.removeObserver)
        tokens.removeAll()
        if state == .running || state == .busy { state = .idle }
    }

    private func run(_ rec: Recommendation) {
        guard let camera = Self.device(for: rec.lens) else {
            state = .unavailable
            return
        }
        device = camera
        let session = session, output = output
        let needsSetup = !configured
        configured = true
        let manual = camera.isExposureModeSupported(.custom)
        manualExposure = manual
        sessionQueue.async {
            if needsSetup {
                session.beginConfiguration()
                session.sessionPreset = .photo
                if let input = try? AVCaptureDeviceInput(device: camera), session.canAddInput(input) { session.addInput(input) }
                if session.canAddOutput(output) { session.addOutput(output) }
                session.commitConfiguration()
                if let conn = output.connection(with: .video), conn.isVideoRotationAngleSupported(90) {
                    conn.videoRotationAngle = 90
                }
            }
            Self.apply(rec, to: camera)
            session.startRunning()
        }
        let center = NotificationCenter.default
        tokens.append(center.addObserver(forName: AVCaptureSession.wasInterruptedNotification, object: session, queue: .main) { [weak self] _ in
            Task { @MainActor in self?.state = .busy }
        })
        tokens.append(center.addObserver(forName: AVCaptureSession.interruptionEndedNotification, object: session, queue: .main) { [weak self] _ in
            Task { @MainActor in self?.state = .running }
        })
        state = .running
    }

    /// The lens the recommendation asked for, or the main camera if this iPhone does not have it.
    nonisolated static func device(for lens: LensType) -> AVCaptureDevice? {
        let type: AVCaptureDevice.DeviceType = switch lens {
        case .main: .builtInWideAngleCamera
        case .ultraWide: .builtInUltraWideCamera
        case .tele: .builtInTelephotoCamera
        }
        return AVCaptureDevice.default(type, for: .video, position: .back)
            ?? AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back)
    }

    /// Sets exposure, white balance and focus. Values are clamped to what the active format allows.
    nonisolated static func apply(_ rec: Recommendation, to camera: AVCaptureDevice) {
        guard (try? camera.lockForConfiguration()) != nil else { return }
        defer { camera.unlockForConfiguration() }

        if camera.isExposureModeSupported(.custom) {
            let format = camera.activeFormat
            let iso = min(max(Float(rec.iso), format.minISO), format.maxISO)
            let seconds = min(max(rec.shutterSeconds, CMTimeGetSeconds(format.minExposureDuration)), CMTimeGetSeconds(format.maxExposureDuration))
            camera.setExposureModeCustom(duration: CMTime(seconds: seconds, preferredTimescale: 1_000_000_000), iso: iso, completionHandler: nil)
        } else if camera.isExposureModeSupported(.continuousAutoExposure) {
            camera.exposureMode = .continuousAutoExposure
            camera.setExposureTargetBias(Float(rec.evBias), completionHandler: nil)
        }

        if camera.isWhiteBalanceModeSupported(.locked) {
            let values = AVCaptureDevice.WhiteBalanceTemperatureAndTintValues(temperature: Float(rec.whiteBalanceK), tint: 0)
            var gains = camera.deviceWhiteBalanceGains(for: values)
            let top = camera.maxWhiteBalanceGain
            gains.redGain = min(max(gains.redGain, 1), top)
            gains.greenGain = min(max(gains.greenGain, 1), top)
            gains.blueGain = min(max(gains.blueGain, 1), top)
            camera.setWhiteBalanceModeLocked(with: gains, completionHandler: nil)
        }

        if rec.focus == .afContinuous, camera.isFocusModeSupported(.continuousAutoFocus) {
            camera.focusMode = .continuousAutoFocus
        } else if camera.isFocusModeSupported(.autoFocus) {
            camera.focusMode = .autoFocus
        }
    }

    /// Takes one photo and saves it to Photos.
    func capture(_ onDone: @escaping (SaveResult) -> Void) {
        guard state == .running, !capturing else { return }
        capturing = true
        let settings: AVCapturePhotoSettings
        if output.availablePhotoCodecTypes.contains(.jpeg) {
            settings = AVCapturePhotoSettings(format: [AVVideoCodecKey: AVVideoCodecType.jpeg])
        } else {
            settings = AVCapturePhotoSettings()
        }
        settings.photoQualityPrioritization = .speed
        let id = settings.uniqueID
        let delegate = PhotoDelegate { [weak self] data in
            Task { @MainActor in
                guard let self else { return }
                delegates[id] = nil
                guard let data else {
                    capturing = false
                    return onDone(.failed)
                }
                let result = await Self.save(data)
                if result == .saved { lastPhoto = UIImage(data: data) }
                capturing = false
                onDone(result)
            }
        }
        delegates[id] = delegate
        output.capturePhoto(with: settings, delegate: delegate)
    }

    private static func save(_ data: Data) async -> SaveResult {
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else { return .photosDenied }
        do {
            try await PHPhotoLibrary.shared().performChanges {
                PHAssetCreationRequest.forAsset().addResource(with: .photo, data: data, options: nil)
            }
            return .saved
        } catch {
            return .failed
        }
    }
}

private final class PhotoDelegate: NSObject, AVCapturePhotoCaptureDelegate {
    private let done: (Data?) -> Void
    init(done: @escaping (Data?) -> Void) { self.done = done }

    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        done(error == nil ? photo.fileDataRepresentation() : nil)
    }
}
