import StoreKit
import SwiftUI

/// App Store products. Create both in App Store Connect with exactly these IDs, or change them here:
/// the lifetime unlock (non-consumable) and the feedback top-up (consumable).
enum Products {
    static let unlock = "lifetime_unlock"
    static let topUp = "feedback_topup_30"
    /// Feedbacks one top-up purchase adds.
    static let topUpCredits = 30
}

/// StoreKit 2 for the two one-time products. Prices are the App Store's own strings, never ours. The unlock is
/// re-read from the App Store's entitlements on every start, so a reinstall or a second iPhone restores it
/// (and a refund removes it); top-ups are turned into feedback credits as soon as the App Store reports them,
/// including ones finished while the app was closed.
@MainActor
final class PurchaseStore: ObservableObject {
    @Published private(set) var unlocked: Bool
    @Published private(set) var unlockPrice: String?
    @Published private(set) var topUpPrice: String?
    /// A purchase is waiting for approval (Ask to Buy, or payment still processing).
    @Published private(set) var pending = false
    @Published private(set) var restoring = false
    /// Bumped whenever the unlock or the top-up credits changed, so screens re-read the quota.
    @Published private(set) var revision = 0

    private let quota: FeedbackQuota
    private var products: [String: Product] = [:]
    private var updates: Task<Void, Never>?

    var ready: Bool { unlockPrice != nil }

    init(quota: FeedbackQuota = FeedbackQuota()) {
        self.quota = quota
        unlocked = quota.unlocked
        updates = Task { [weak self] in
            for await result in Transaction.updates { await self?.handle(result) }
        }
        Task { await refresh() }
    }

    deinit { updates?.cancel() }

    /// Loads prices and re-reads what the user owns.
    func refresh() async {
        if let list = try? await Product.products(for: [Products.unlock, Products.topUp]) {
            products = Dictionary(uniqueKeysWithValues: list.map { ($0.id, $0) })
            unlockPrice = products[Products.unlock]?.displayPrice
            topUpPrice = products[Products.topUp]?.displayPrice
        }
        var owned = false
        for await result in Transaction.currentEntitlements {
            if case .verified(let t) = result, t.productID == Products.unlock, t.revocationDate == nil { owned = true }
        }
        setUnlocked(owned)
        // Consumables that were bought but not yet finished come through here too.
        for await result in Transaction.unfinished { await handle(result) }
    }

    func buy(_ id: String) async {
        guard let product = products[id] else { return }
        do {
            switch try await product.purchase() {
            case .success(let result): await handle(result)
            case .pending: pending = true
            case .userCancelled: break
            @unknown default: break
            }
        } catch {}
    }

    /// "Restore purchases": asks the App Store to sync, then re-reads the entitlements.
    func restore() async {
        restoring = true
        try? await AppStore.sync()
        await refresh()
        restoring = false
    }

    private func handle(_ result: VerificationResult<Transaction>) async {
        guard case .verified(let t) = result else { return }
        switch t.productID {
        case Products.unlock:
            setUnlocked(t.revocationDate == nil)
        case Products.topUp:
            if t.revocationDate == nil { quota.addTopUp(Products.topUpCredits) }
        default: break
        }
        pending = false
        await t.finish()
        revision += 1
    }

    private func setUnlocked(_ value: Bool) {
        guard value != unlocked else { return }
        unlocked = value
        quota.setUnlocked(value)
        revision += 1
    }
}
