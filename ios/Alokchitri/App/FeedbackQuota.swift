import Foundation

/// How many photo feedbacks the user can still ask for: a daily allowance (resets at local midnight), one-off
/// extras for the day (rewarded ads), then top-up credits that never expire. Counted on the device; a feedback
/// is only spent after the server answered. Mirrors the Android FeedbackQuota.
struct FeedbackQuota {
    static let freePerDay = 3
    static let unlockedPerDay = 20

    private let defaults: UserDefaults
    private let today: () -> Date

    init(defaults: UserDefaults = .standard, today: @escaping () -> Date = Date.init) {
        self.defaults = defaults
        self.today = today
    }

    var unlocked: Bool { defaults.bool(forKey: "quota.unlocked") }
    func setUnlocked(_ value: Bool) { defaults.set(value, forKey: "quota.unlocked") }
    var dailyLimit: Int { unlocked ? Self.unlockedPerDay : Self.freePerDay }
    var topUpCredits: Int { defaults.integer(forKey: "quota.topup") }

    /// Left from today's allowance and extras, not counting top-up credits.
    var leftToday: Int {
        rollOver()
        return max(0, dailyLimit + defaults.integer(forKey: "quota.bonus") - defaults.integer(forKey: "quota.used"))
    }

    var available: Int { leftToday + topUpCredits }

    func consume() {
        if leftToday > 0 { defaults.set(defaults.integer(forKey: "quota.used") + 1, forKey: "quota.used") }
        else if topUpCredits > 0 { defaults.set(topUpCredits - 1, forKey: "quota.topup") }
    }

    func addBonus() {
        rollOver()
        defaults.set(defaults.integer(forKey: "quota.bonus") + 1, forKey: "quota.bonus")
    }

    func addTopUp(_ credits: Int) { defaults.set(topUpCredits + credits, forKey: "quota.topup") }

    private func rollOver() {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        let day = f.string(from: today())
        if defaults.string(forKey: "quota.day") != day {
            defaults.set(day, forKey: "quota.day")
            defaults.set(0, forKey: "quota.used")
            defaults.set(0, forKey: "quota.bonus")
        }
    }
}
