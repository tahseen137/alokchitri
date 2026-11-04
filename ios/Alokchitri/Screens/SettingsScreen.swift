import SwiftUI

/// Settings, opened from the gear on Home: language, phone, lifetime unlock and restore purchases.
struct SettingsScreen: View {
    @EnvironmentObject var settings: AppSettings
    @EnvironmentObject var store: PurchaseStore
    let phoneName: String
    let onUpgrade: () -> Void
    let onBack: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                BackBar(title: settings.t("settings_title"), titleBold: true, onBack: onBack)
                AlokCard(padding: 0) {
                    HStack {
                        Text(settings.t("settings_language")).font(AlokType.bodyLarge).foregroundStyle(AlokColors.text)
                        Spacer()
                        LanguageToggle(language: $settings.language)
                    }
                    .padding(.horizontal, 18).padding(.vertical, 8).frame(minHeight: 56)
                    Rectangle().fill(AlokColors.outline).frame(height: 1)
                    HStack {
                        Text(settings.t("settings_phone")).font(AlokType.bodyLarge).foregroundStyle(AlokColors.text)
                        Spacer(minLength: 12)
                        Text(verbatim: phoneName).font(AlokType.body).foregroundStyle(AlokColors.textMuted).lineLimit(1)
                    }
                    .padding(.horizontal, 18).padding(.vertical, 8).frame(minHeight: 56)
                    Rectangle().fill(AlokColors.outline).frame(height: 1)
                    Button(action: onUpgrade) {
                        HStack {
                            Text(settings.t("settings_unlock")).font(AlokType.bodyLarge).foregroundStyle(AlokColors.text)
                            Spacer(minLength: 12)
                            Text(settings.t(store.unlocked ? "upgrade_unlocked" : "settings_not_unlocked"))
                                .font(AlokType.body).foregroundStyle(store.unlocked ? AlokColors.green : AlokColors.amber)
                            Image(systemName: "chevron.right").font(.system(size: 13, weight: .semibold)).foregroundStyle(AlokColors.textMuted)
                        }
                        .padding(.horizontal, 18).padding(.vertical, 8).frame(minHeight: 56).contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    Rectangle().fill(AlokColors.outline).frame(height: 1)
                    Button { Task { await store.restore() } } label: {
                        HStack {
                            Text(settings.t(store.restoring ? "settings_restoring" : "settings_restore")).font(AlokType.bodyLarge).foregroundStyle(AlokColors.text)
                            Spacer(minLength: 0)
                        }
                        .padding(.horizontal, 18).padding(.vertical, 8).frame(minHeight: 56).contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .disabled(store.restoring)
                }
                Text(settings.tf("settings_version_fmt", Self.version))
                    .font(AlokType.caption).foregroundStyle(AlokColors.textMuted).padding(.horizontal, 4)
            }
            .padding(.horizontal, AlokSpacing.screen)
            .padding(.top, 8)
            .padding(.bottom, 24)
        }
        .alokScreen()
    }

    static var version: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "0.1.0"
    }
}
