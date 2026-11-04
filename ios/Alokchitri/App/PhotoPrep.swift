import AlokEngine
import ImageIO
import UIKit

/// Settings the camera saved in the photo; any of them may be missing (screenshots, edited or shared photos).
struct PhotoExif: Equatable {
    var iso: Int?
    var shutter: String?
    var aperture: Double?
    var focalLength: Double?
    var flash: Bool?

    /// Chips for the Feedback screen, in the order ISO, shutter, aperture.
    var chips: [String] {
        [iso.map { "ISO \($0)" }, shutter, aperture.map { String(format: "f/%.1f", $0) }].compactMap { $0 }
    }
}

struct PreparedPhoto {
    let preview: UIImage
    let base64Jpeg: String
    let exif: PhotoExif
}

/// Reads the picked photo's EXIF and shrinks it for upload: 1280 px long side, JPEG 80, upright.
enum PhotoPrep {
    static let maxSide = 1280

    static func prepare(_ data: Data) -> PreparedPhoto? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true, // upright
            kCGImageSourceThumbnailMaxPixelSize: maxSide,
        ]
        guard let cg = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else { return nil }
        let image = UIImage(cgImage: cg)
        guard let jpeg = image.jpegData(compressionQuality: 0.8) else { return nil }
        return PreparedPhoto(preview: image, base64Jpeg: jpeg.base64EncodedString(), exif: readExif(source))
    }

    private static func readExif(_ source: CGImageSource) -> PhotoExif {
        guard let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let exif = props[kCGImagePropertyExifDictionary] as? [CFString: Any] else { return PhotoExif() }
        let iso = (exif[kCGImagePropertyExifISOSpeedRatings] as? [NSNumber])?.first?.intValue
        let exposure = (exif[kCGImagePropertyExifExposureTime] as? NSNumber)?.doubleValue
        let aperture = (exif[kCGImagePropertyExifFNumber] as? NSNumber)?.doubleValue
        let focal = (exif[kCGImagePropertyExifFocalLength] as? NSNumber)?.doubleValue
        let flash = (exif[kCGImagePropertyExifFlash] as? NSNumber)?.intValue
        return PhotoExif(
            iso: iso.flatMap { $0 > 0 ? $0 : nil },
            shutter: exposure.flatMap { $0 > 0 ? formatShutter($0) : nil },
            aperture: aperture.flatMap { $0 > 0 ? $0 : nil },
            focalLength: focal.flatMap { $0 > 0 ? $0 : nil },
            flash: flash.map { $0 & 1 == 1 }
        )
    }
}
