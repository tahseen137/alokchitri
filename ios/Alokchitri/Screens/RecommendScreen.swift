import AlokEngine
import SwiftUI

/// Screen 6 · Recommended settings. Handles both outcomes of the engine.
struct RecommendScreen: View {
    @EnvironmentObject var settings: AppSettings
    let goal: ShotGoal
    let result: RecommendationResult
    let onBack: () -> Void
    let onShoot: () -> Void
    let onSave: (String) -> Void
    let onHome: () -> Void

    @State var showSave = false
    @State var saved = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                BackBar(onBack: onBack)
                Spacer()
                Text(settings.t(goal.titleKey))
                    .font(AlokType.caption).foregroundStyle(AlokColors.text)
                    .padding(.horizontal, 14).padding(.vertical, 6)
                    .background(AlokColors.amberTint, in: Capsule())
            }

            switch result {
            case .manualUnsupported:
                ScrollView { manualUnsupported }
                PrimaryButton(title: settings.t("manual_home"), action: onHome).padding(.top, 12)
            case .success(let rec):
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(settings.t("rec_title")).font(AlokType.headlineSmall).foregroundStyle(AlokColors.text).padding(.top, 4)
                        ForEach(rec.orderedAdvice, id: \.self) { advice in
                            InfoBanner(systemImage: advice.systemImage, tint: advice.tint, text: settings.t(advice.textKey))
                        }
                        table(rec)
                    }
                }
                PrimaryButton(title: settings.t("rec_cta_pro"), systemImage: "camera", action: onShoot).padding(.top, 12)
                SecondaryButton(title: settings.t(saved ? "recipes_saved" : "rec_save"), systemImage: saved ? "checkmark" : "bookmark", accent: saved) {
                    showSave = true
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 10)
                .disabled(saved)
            }
        }
        .padding(.horizontal, AlokSpacing.screen)
        .padding(.top, 8)
        .padding(.bottom, 24)
        .alokScreen()
        .sheet(isPresented: $showSave) {
            SaveRecipeSheet(
                name: settings.t(goal.titleKey),
                onCancel: { showSave = false },
                onSave: { name in onSave(name); saved = true; showSave = false }
            )
            .environmentObject(settings)
        }
    }

    private func table(_ rec: Recommendation) -> some View {
        AlokCard(padding: 0) {
            SettingRow(label: settings.t("rec_iso"), value: "\(rec.iso)")
            SettingRow(label: settings.t("rec_shutter"), value: rec.shutterLabel)
            SettingRow(label: settings.t("rec_wb"), value: "\(rec.whiteBalanceK)K")
            SettingRow(label: settings.t("rec_focus"), value: settings.t(rec.focus.labelKey))
            SettingRow(label: settings.t("rec_ev"), value: formatEv(rec.evBias))
            SettingRow(label: settings.t("rec_lens"), value: settings.t(rec.lens.labelKey()))
            SettingRow(label: settings.t("rec_format"), value: settings.t(rec.format.labelKey))
            SettingRow(
                label: settings.t("rec_stability"),
                value: settings.t(rec.stability.labelKey),
                valueColor: rec.stability == .handheldOk ? AlokColors.green : AlokColors.amber,
                divider: false
            )
        }
    }

    /// No manual control on this camera: say so, then what still helps in Auto.
    private var manualUnsupported: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(settings.t("manual_title")).font(AlokType.headlineSmall).foregroundStyle(AlokColors.text).padding(.top, 24)
            Text(settings.t("manual_body")).font(AlokType.bodyLarge).foregroundStyle(AlokColors.textMuted).lineSpacing(4)
            Text(settings.t("manual_tips_title")).font(AlokType.label).foregroundStyle(AlokColors.amber).padding(.top, 4)
            InfoBanner(systemImage: "moon.stars", tint: AlokColors.green, text: settings.t("manual_tip_night"))
            InfoBanner(systemImage: "info.circle", tint: AlokColors.green, text: settings.t("manual_tip_tap"))
            InfoBanner(systemImage: "info.circle", tint: AlokColors.green, text: settings.t("manual_tip_steady"))
        }
    }
}

private extension Advice {
    var systemImage: String {
        switch self {
        case .preferNightMode: "moon.stars"
        case .preferAuto: "info.circle"
        default: "exclamationmark.triangle"
        }
    }

    /// Green = a friendly alternative ("use Night mode"), amber = a limitation to know about.
    var tint: Color {
        switch self {
        case .preferNightMode, .preferAuto: AlokColors.green
        default: AlokColors.amber
        }
    }
}
