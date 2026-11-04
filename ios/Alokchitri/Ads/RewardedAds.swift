import GoogleMobileAds
import UIKit

/// AdMob IDs. These are Google's sample IDs, which always fill with a test ad. Replace the unit here and the app
/// ID in Config/Info-Ads.plist with the real ones before release.
enum AdConfig {
    static let rewardedUnit = "ca-app-pub-3940256099942544/1712485313"
}

/// One rewarded ad at a time for "watch an ad for one more feedback". Loads ahead, so the ad shows instantly
/// on tap, and loads the next one after it closes.
@MainActor
final class RewardedAds: NSObject, ObservableObject, FullScreenContentDelegate {
    private var ad: RewardedAd?
    private var loading = false
    private var started = false

    /// Starts the SDK and loads the first ad. Safe to call more than once.
    func start() {
        guard !started else { return }
        started = true
        MobileAds.shared.start { [weak self] _ in
            Task { @MainActor in self?.load() }
        }
    }

    func load() {
        guard started, ad == nil, !loading else { return }
        loading = true
        Task {
            let loaded = try? await RewardedAd.load(with: AdConfig.rewardedUnit, request: Request())
            loading = false
            ad = loaded
            ad?.fullScreenContentDelegate = self
        }
    }

    /// Shows the ad; `onReward` runs only if the user watched it to the end. False if no ad was ready.
    @discardableResult
    func show(onReward: @escaping () -> Void) -> Bool {
        guard let current = ad, let top = Self.topViewController() else { load(); return false }
        ad = nil
        current.present(from: top) { onReward() }
        return true
    }

    nonisolated func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        Task { @MainActor in self.load() }
    }

    nonisolated func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        Task { @MainActor in self.load() }
    }

    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top
    }
}
