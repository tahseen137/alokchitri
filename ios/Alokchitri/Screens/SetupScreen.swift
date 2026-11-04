import AlokEngine
import SwiftUI

/// Screen 2 · Phone setup: what AVFoundation reported about this iPhone's back camera.
struct SetupScreen: View {
    @EnvironmentObject var settings: AppSettings
    let camera: CameraInfo
    let onBack: () -> Void
    let onConfirm: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            BackBar(title: settings.t("setup_step"), onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text(settings.t("setup_title")).font(AlokType.headline).foregroundStyle(AlokColors.text)
                        .padding(.top, 12)
                    Text(settings.t("setup_sub")).font(AlokType.body).foregroundStyle(AlokColors.textMuted).lineSpacing(4)
                        .padding(.top, 8)

                    phoneCard.padding(.top, 20)

                    if let limits = camera.limits {
                        limitsCard(limits).padding(.top, 16)
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "info.circle").font(.system(size: 20)).foregroundStyle(AlokColors.textMuted).padding(.top, 3)
                            Text(settings.t("setup_note")).font(AlokType.body).foregroundStyle(AlokColors.textMuted).lineSpacing(4)
                        }
                        .padding(.horizontal, 4)
                        .padding(.top, 16)
                    } else {
                        InfoBanner(systemImage: "exclamationmark.triangle", text: settings.t("setup_no_camera"))
                            .padding(.top, 16)
                    }
                }
            }
            PrimaryButton(title: settings.t("setup_cta"), action: onConfirm)
                .padding(.top, 12)
        }
        .padding(.horizontal, AlokSpacing.screen)
        .padding(.top, 8)
        .padding(.bottom, 28)
        .alokScreen()
    }

    private var manual: Bool { camera.limits?.manualSupported ?? false }

    private var phoneCard: some View {
        AlokCard(padding: 18) {
            HStack(spacing: 16) {
                Image(systemName: "iphone")
                    .font(.system(size: 26))
                    .foregroundStyle(AlokColors.amber)
                    .frame(width: 56, height: 56)
                    .background(AlokColors.surfaceRaised, in: RoundedRectangle(cornerRadius: AlokRadius.tile, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(settings.t("setup_your_phone")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                    Text(verbatim: camera.phoneName).font(AlokType.title).foregroundStyle(AlokColors.text)
                    Text(settings.t(manual ? "setup_pro_found" : "setup_pro_missing"))
                        .font(AlokType.bodySmall)
                        .foregroundStyle(manual ? AlokColors.green : AlokColors.amber)
                }
            }
        }
    }

    private func limitsCard(_ d: DeviceLimits) -> some View {
        AlokCard(padding: 0) {
            SettingRow(label: settings.t("setup_iso_range"), value: "\(d.isoMin) – \(d.isoMax)", valueColor: AlokColors.text)
            SettingRow(
                label: settings.t("setup_shutter_range"),
                value: "\(formatShutter(d.shutterMinSeconds)) – \(formatShutter(d.shutterMaxSeconds))",
                valueColor: AlokColors.text
            )
            SettingRow(
                label: settings.t("setup_lenses"),
                value: d.lenses.sorted { $0.sortOrder < $1.sortOrder }.map { settings.t($0.labelKey(withZoom: false)) }.joined(separator: " · "),
                valueColor: AlokColors.text
            )
            SettingRow(
                label: settings.t("setup_manual"),
                value: settings.t(d.manualSupported ? "setup_available" : "setup_unavailable"),
                valueColor: d.manualSupported ? AlokColors.green : AlokColors.amber,
                divider: false
            )
        }
    }
}

#Preview {
    SetupScreen(
        camera: CameraInfo(
            phoneName: "iPhone 15 Pro",
            limits: DeviceLimits(isoMin: 32, isoMax: 3072, shutterMinSeconds: 1.0 / 24000, shutterMaxSeconds: 1.0 / 3, aperture: 1.78, lenses: [.main, .ultraWide, .tele])
        ),
        onBack: {},
        onConfirm: {}
    )
    .environmentObject(AppSettings())
}
