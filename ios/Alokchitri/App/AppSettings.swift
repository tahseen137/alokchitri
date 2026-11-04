import Foundation
import SwiftUI

/// Bangla is the default; English is the toggle on Welcome and in Settings.
enum AppLanguage: String, CaseIterable, Identifiable {
    case bn, en

    var id: String { rawValue }
    var locale: Locale { Locale(identifier: self == .bn ? "bn_BD" : "en_US") }
}

/// App-wide choices that survive restarts, plus the string lookup for the chosen language.
///
/// iOS normally picks an app's language from the system settings. The design has an in-app toggle, so strings
/// are read from the matching .lproj bundle directly instead of through `Text("key")`.
@MainActor
final class AppSettings: ObservableObject {
    @Published var language: AppLanguage {
        didSet {
            defaults.set(language.rawValue, forKey: Keys.language)
            bundle = Self.bundle(for: language)
        }
    }

    /// True once the user has confirmed the Setup screen: later launches open straight on Home.
    @Published var setupDone: Bool {
        didSet { defaults.set(setupDone, forKey: Keys.setupDone) }
    }

    private let defaults: UserDefaults
    private var bundle: Bundle

    private enum Keys {
        static let language = "language"
        static let setupDone = "setupDone"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        let lang = AppLanguage(rawValue: defaults.string(forKey: Keys.language) ?? "") ?? .bn
        language = lang
        setupDone = defaults.bool(forKey: Keys.setupDone)
        bundle = Self.bundle(for: lang)
    }

    /// The string named `key` (the Android string name) in the current language.
    func t(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: key, table: "Localizable")
    }

    /// A string with `%1$@`-style placeholders, filled in order.
    func tf(_ key: String, _ args: String...) -> String {
        String(format: t(key), locale: language.locale, arguments: args)
    }

    /// Digits follow the language: ২ in Bangla, 2 in English.
    func number(_ n: Int) -> String {
        let f = NumberFormatter()
        f.locale = language.locale
        return f.string(from: NSNumber(value: n)) ?? String(n)
    }

    private static func bundle(for language: AppLanguage) -> Bundle {
        guard let path = Bundle.main.path(forResource: language.rawValue, ofType: "lproj"),
              let bundle = Bundle(path: path) else { return .main }
        return bundle
    }
}
