import AVFoundation
import AlokEngine
import UIKit

/// What the Setup screen shows: the phone's name and what its back camera can do.
struct CameraInfo {
    let phoneName: String
    /// Nil when there is no back camera, e.g. in the Simulator.
    let limits: DeviceLimits?
}

/// Reads the back camera's limits from AVFoundation. Every iPhone since the 6s allows manual ISO and shutter
/// (AVCaptureDevice.ExposureMode.custom), so unlike Android there is no "Pro mode missing" case in practice.
enum DeviceProbe {
    static func probe() -> CameraInfo {
        CameraInfo(phoneName: phoneName(), limits: limits())
    }

    static func limits() -> DeviceLimits? {
        guard let main = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else { return nil }
        let format = main.activeFormat

        let others = AVCaptureDevice.DiscoverySession(
            deviceTypes: [.builtInUltraWideCamera, .builtInTelephotoCamera],
            mediaType: .video,
            position: .back
        ).devices
        var lenses: Set<LensType> = [.main]
        if others.contains(where: { $0.deviceType == .builtInUltraWideCamera }) { lenses.insert(.ultraWide) }
        if others.contains(where: { $0.deviceType == .builtInTelephotoCamera }) { lenses.insert(.tele) }

        return DeviceLimits(
            isoMin: Int(format.minISO.rounded(.up)),
            isoMax: Int(format.maxISO.rounded(.down)),
            shutterMinSeconds: CMTimeGetSeconds(format.minExposureDuration),
            shutterMaxSeconds: CMTimeGetSeconds(format.maxExposureDuration),
            aperture: Double(main.lensAperture),
            lenses: lenses,
            // RAW needs a running photo output to check; the camera screen fills this in.
            rawSupported: false,
            // Every iPhone that runs iOS 17 (XS / SE 2nd gen and later) has optical stabilisation on the main camera.
            hasOis: true,
            manualSupported: main.isExposureModeSupported(.custom)
        )
    }

    /// "iPhone 15 Pro". UIDevice.name is just "iPhone" since iOS 16, so map the model identifier.
    static func phoneName() -> String {
        let id = modelIdentifier()
        return modelNames[id] ?? UIDevice.current.model
    }

    private static func modelIdentifier() -> String {
        if let sim = ProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] { return sim }
        var info = utsname()
        uname(&info)
        return withUnsafeBytes(of: &info.machine) { raw in
            String(decoding: raw.prefix(while: { $0 != 0 }), as: UTF8.self)
        }
    }

    private static let modelNames: [String: String] = [
        "iPhone11,2": "iPhone XS", "iPhone11,4": "iPhone XS Max", "iPhone11,6": "iPhone XS Max", "iPhone11,8": "iPhone XR",
        "iPhone12,1": "iPhone 11", "iPhone12,3": "iPhone 11 Pro", "iPhone12,5": "iPhone 11 Pro Max", "iPhone12,8": "iPhone SE",
        "iPhone13,1": "iPhone 12 mini", "iPhone13,2": "iPhone 12", "iPhone13,3": "iPhone 12 Pro", "iPhone13,4": "iPhone 12 Pro Max",
        "iPhone14,4": "iPhone 13 mini", "iPhone14,5": "iPhone 13", "iPhone14,2": "iPhone 13 Pro", "iPhone14,3": "iPhone 13 Pro Max",
        "iPhone14,6": "iPhone SE", "iPhone14,7": "iPhone 14", "iPhone14,8": "iPhone 14 Plus",
        "iPhone15,2": "iPhone 14 Pro", "iPhone15,3": "iPhone 14 Pro Max", "iPhone15,4": "iPhone 15", "iPhone15,5": "iPhone 15 Plus",
        "iPhone16,1": "iPhone 15 Pro", "iPhone16,2": "iPhone 15 Pro Max",
        "iPhone17,1": "iPhone 16 Pro", "iPhone17,2": "iPhone 16 Pro Max", "iPhone17,3": "iPhone 16", "iPhone17,4": "iPhone 16 Plus",
        "iPhone17,5": "iPhone 16e",
    ]
}
