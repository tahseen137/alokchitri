import PhotosUI
import SwiftUI

enum FeedbackState {
    case empty
    case loading
    case ready(FeedbackResult)
    /// Daily quota used up.
    case outOfQuota
    /// Network or server failure (retry), or `unreadable` when the picked file is not a photo.
    case error(unreadable: Bool)
}

/// Screen 9: picked photo -> EXIF + downscale -> server -> feedback. A feedback is spent only when the server
/// answered, so failed calls cost nothing.
@MainActor
final class FeedbackModel: ObservableObject {
    @Published var state: FeedbackState = .empty
    @Published var preview: UIImage?
    @Published var chips: [String] = []
    @Published var hasPhoto = false
    @Published private(set) var left: Int
    @Published private(set) var leftToday: Int

    let quota: FeedbackQuota
    private var photo: PreparedPhoto?
    private var task: Task<Void, Never>?

    init(quota: FeedbackQuota = FeedbackQuota()) {
        self.quota = quota
        left = quota.available
        leftToday = quota.leftToday
    }

    func onPicked(_ item: PhotosPickerItem?, language: String, phone: String?) {
        guard let item else { return }
        task?.cancel()
        task = Task {
            let data = try? await item.loadTransferable(type: Data.self)
            let prepared = await Task.detached { data.flatMap(PhotoPrep.prepare) }.value
            guard !Task.isCancelled else { return }
            guard let prepared else {
                photo = nil; hasPhoto = false; preview = nil; chips = []
                state = .error(unreadable: true)
                return
            }
            photo = prepared
            preview = prepared.preview
            chips = prepared.exif.chips
            hasPhoto = true
            await ask(language: language, phone: phone)
        }
    }

    func retry(language: String, phone: String?) {
        guard photo != nil else { return }
        task?.cancel()
        task = Task { await ask(language: language, phone: phone) }
    }

    func refreshQuota() {
        left = quota.available
        leftToday = quota.leftToday
    }

    private func ask(language: String, phone: String?) async {
        guard let photo else { return }
        if quota.available <= 0 { state = .outOfQuota; return }
        state = .loading
        do {
            let result = try await FeedbackApi.request(photo, language: language, phone: phone)
            guard !Task.isCancelled else { return }
            quota.consume()
            refreshQuota()
            state = .ready(result)
        } catch {
            if !Task.isCancelled { state = .error(unreadable: false) }
        }
    }
}
