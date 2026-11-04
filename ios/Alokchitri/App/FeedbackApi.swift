import Foundation

struct FeedbackResult: Equatable {
    let whatHappened: String
    let nextTime: String
}

/// Calls server/api/photo-feedback.ts (the same server as the Android app). Throws on any network, HTTP or format problem.
enum FeedbackApi {
    static let url = URL(string: "https://alokchitri-server.vercel.app/api/photo-feedback")!

    static func request(_ photo: PreparedPhoto, language: String, phone: String?) async throws -> FeedbackResult {
        var exif: [String: Any] = [:]
        if let v = photo.exif.iso { exif["iso"] = v }
        if let v = photo.exif.shutter { exif["shutter"] = v }
        if let v = photo.exif.aperture { exif["aperture"] = v }
        if let v = photo.exif.focalLength { exif["focalLength"] = v }
        if let v = photo.exif.flash { exif["flash"] = v }
        var body: [String: Any] = ["image": photo.base64Jpeg, "language": language, "exif": exif]
        if let phone { body["phone"] = String(phone.prefix(80)) }

        var req = URLRequest(url: url, timeoutInterval: 60) // the model looks at the photo; give it time on slow data
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, response) = try await URLSession.shared.data(for: req)
        guard (response as? HTTPURLResponse)?.statusCode == 200,
              let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let what = json["whatHappened"] as? String, let next = json["nextTime"] as? String
        else { throw URLError(.badServerResponse) }
        return FeedbackResult(whatHappened: what, nextTime: next)
    }
}
