import AlokEngine
import SwiftUI
import UIKit

/// The one-tap Pro setup: our own camera, opened with the recommended settings already applied.
/// The user frames the shot and presses the shutter; the photo goes to Photos.
struct ProCameraScreen: View {
    @EnvironmentObject var settings: AppSettings
    @StateObject var camera = ProCamera()
    let goal: ShotGoal
    let rec: Recommendation
    let onBack: () -> Void

    @State var toast: String?

    var body: some View {
        ZStack(alignment: .top) {
            AlokColors.viewfinderDeep.ignoresSafeArea()
            preview.frame(maxWidth: .infinity, maxHeight: .infinity).ignoresSafeArea(edges: .top)

            VStack(spacing: 0) {
                HStack {
                    BackBar(onBack: onBack).fixedSize().background(AlokColors.ground, in: Capsule())
                    Spacer()
                    Text(settings.t(goal.titleKey))
                        .font(AlokType.label).foregroundStyle(AlokColors.text)
                        .padding(.horizontal, 16).padding(.vertical, 8)
                        .background(AlokColors.amberTint, in: Capsule())
                }
                .padding(.horizontal, 12).padding(.top, 8)

                chips
                if !camera.manualExposure {
                    InfoBanner(systemImage: "info.circle", text: settings.t("pro_exposure_auto"))
                        .padding(.horizontal, 20)
                }
                Spacer(minLength: 0)
                sheet
            }

            if let toast {
                Text(toast)
                    .font(AlokType.body).foregroundStyle(AlokColors.text).multilineTextAlignment(.center)
                    .padding(.horizontal, 18).padding(.vertical, 12)
                    .background(AlokColors.surfaceRaised, in: Capsule())
                    .padding(.top, 140)
                    .transition(.opacity)
            }
        }
        .onAppear { camera.start(rec) }
        .onDisappear { camera.stop() }
    }

    @ViewBuilder private var preview: some View {
        switch camera.state {
        case .running, .busy:
            CameraPreview(session: camera.session)
        case .denied:
            VStack(spacing: 16) {
                Text(settings.t("meter_cam_denied")).font(AlokType.bodyLarge).foregroundStyle(AlokColors.text).multilineTextAlignment(.center)
                SecondaryButton(title: settings.t("meter_open_settings"), accent: true) { openSettings() }
            }
            .padding(.horizontal, 32).frame(maxWidth: .infinity, maxHeight: .infinity).background(AlokColors.viewfinder)
        case .unavailable:
            Text(settings.t("setup_no_camera")).font(AlokType.body).foregroundStyle(AlokColors.textMuted).multilineTextAlignment(.center)
                .padding(.horizontal, 32).frame(maxWidth: .infinity, maxHeight: .infinity).background(AlokColors.viewfinder)
        case .idle:
            AlokColors.viewfinder
        }
    }

    /// The values that were applied, over the viewfinder. ISO and shutter only when the camera took them.
    private var chips: some View {
        var items: [String] = []
        if camera.manualExposure {
            items.append("ISO \(rec.iso)")
            items.append(rec.shutterLabel)
        }
        items.append("\(rec.whiteBalanceK)K")
        items.append(settings.t(rec.focus.labelKey))
        items.append(settings.t(rec.lens.labelKey()))
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(items, id: \.self) { item in
                    Text(verbatim: item).font(AlokType.caption).foregroundStyle(AlokColors.amber)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(AlokColors.ground, in: Capsule())
                }
            }
            .padding(.horizontal, 20).padding(.vertical, 10)
        }
    }

    private var sheet: some View {
        let steady = rec.stability == .handheldOk
        return VStack(spacing: 16) {
            Text(settings.t(steady ? "pro_title" : "pro_hold_still"))
                .font(AlokType.body).foregroundStyle(steady ? AlokColors.green : AlokColors.amber).multilineTextAlignment(.center)
            ZStack {
                HStack {
                    if let image = camera.lastPhoto {
                        Image(uiImage: image).resizable().scaledToFill()
                            .frame(width: 56, height: 56)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .accessibilityLabel(settings.t("pro_view_cd"))
                    }
                    Spacer()
                }
                shutter
            }
        }
        .padding(.horizontal, 20).padding(.top, 16).padding(.bottom, 28)
        .frame(maxWidth: .infinity)
        .background(
            UnevenRoundedRectangle(topLeadingRadius: AlokRadius.sheet, topTrailingRadius: AlokRadius.sheet, style: .continuous)
                .fill(AlokColors.surface).ignoresSafeArea(edges: .bottom)
        )
    }

    private var shutter: some View {
        let enabled = camera.state == .running && !camera.capturing
        return Button {
            camera.capture { result in
                switch result {
                case .saved: show(settings.t("pro_saved"))
                case .failed: show(settings.t("pro_failed"))
                case .photosDenied: show(settings.t("pro_photos_denied"))
                }
            }
        } label: {
            Circle()
                .strokeBorder(enabled ? AlokColors.text : AlokColors.outlineStrong, lineWidth: 4)
                .frame(width: 76, height: 76)
                .overlay(
                    Circle().fill(enabled ? AlokColors.text : AlokColors.outlineStrong).padding(8)
                        .overlay { if camera.capturing { ProgressView().tint(AlokColors.ground) } }
                )
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(settings.t("pro_shutter_cd"))
    }

    private func show(_ message: String) {
        withAnimation { toast = message }
        Task {
            try? await Task.sleep(for: .seconds(2.5))
            withAnimation { toast = nil }
        }
    }

    private func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
    }
}
