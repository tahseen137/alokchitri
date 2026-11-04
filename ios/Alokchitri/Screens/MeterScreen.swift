import AlokEngine
import SwiftUI
import UIKit

/// Screen 5 · Meter the light: live camera on top, the five-bar gauge in a sheet below.
struct MeterScreen: View {
    @EnvironmentObject var settings: AppSettings
    @StateObject var camera = LightMeterCamera()
    let goal: ShotGoal
    let onBack: () -> Void
    /// Called with the light reading when the user taps Done.
    let onDone: (LightReading?) -> Void

    var body: some View {
        ZStack(alignment: .top) {
            AlokColors.viewfinderDeep.ignoresSafeArea()

            preview
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .ignoresSafeArea(edges: .top)

            VStack(spacing: 0) {
                HStack {
                    BackBar(onBack: onBack).fixedSize().background(AlokColors.ground, in: Capsule())
                    Spacer()
                    Text(settings.t(goal.titleKey))
                        .font(AlokType.label).foregroundStyle(AlokColors.text)
                        .padding(.horizontal, 16).padding(.vertical, 8)
                        .background(AlokColors.amberTint, in: Capsule())
                }
                .padding(.horizontal, 12)
                .padding(.top, 8)

                Text(settings.t("meter_hint"))
                    .font(AlokType.body).foregroundStyle(AlokColors.text).multilineTextAlignment(.center)
                    .padding(.horizontal, 16).padding(.vertical, 12)
                    .background(AlokColors.ground, in: RoundedRectangle(cornerRadius: AlokRadius.banner, style: .continuous))
                    .padding(.horizontal, 20).padding(.top, 8)

                Spacer(minLength: 0)
                sheet
            }
        }
        .onAppear { camera.start() }
        .onDisappear { camera.stop() }
    }

    @ViewBuilder private var preview: some View {
        switch camera.state {
        case .running, .busy:
            ZStack {
                CameraPreview(session: camera.session)
                MeterReticle()
            }
        case .denied:
            Unavailable(message: settings.t("meter_cam_denied"), action: settings.t("meter_open_settings")) {
                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
            }
        case .unavailable:
            ZStack {
                AlokColors.viewfinder
                MeterReticle()
                Text(settings.t("setup_no_camera")).font(AlokType.body).foregroundStyle(AlokColors.textMuted)
                    .multilineTextAlignment(.center).padding(.horizontal, 32).padding(.top, 220)
            }
        case .idle:
            AlokColors.viewfinder
        }
    }

    private var sheet: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let level = camera.level {
                HStack(alignment: .bottom) {
                    Text(settings.t("meter_light_now")).font(AlokType.body).foregroundStyle(AlokColors.textMuted)
                    Spacer()
                    Text(settings.t(level.labelKey)).font(AlokType.headlineSmall).foregroundStyle(AlokColors.amber)
                }
                LightGauge(level: level.gauge, description: settings.tf("meter_gauge_cd", settings.number(level.gauge)))
                HStack {
                    Text(settings.t("meter_dark_end")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                    Spacer()
                    Text(settings.t("meter_bright_end")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                }
            } else {
                Text(settings.t("meter_no_reading")).font(AlokType.body).foregroundStyle(AlokColors.textMuted)
            }
            PrimaryButton(title: settings.t("meter_cta")) { onDone(camera.reading) }
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 28)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            UnevenRoundedRectangle(topLeadingRadius: AlokRadius.sheet, topTrailingRadius: AlokRadius.sheet, style: .continuous)
                .fill(AlokColors.surface)
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

/// Shown instead of the preview when there is no camera feed.
private struct Unavailable: View {
    let message: String
    let action: String
    let onAction: () -> Void

    var body: some View {
        ZStack {
            AlokColors.viewfinder
            VStack(spacing: 16) {
                Text(message).font(AlokType.bodyLarge).foregroundStyle(AlokColors.text).multilineTextAlignment(.center)
                SecondaryButton(title: action, accent: true, action: onAction)
            }
            .padding(.horizontal, 32)
        }
    }
}

/// Rule-of-thirds grid and the amber spot-meter reticle, drawn over the live feed.
struct MeterReticle: View {
    var body: some View {
        Canvas { ctx, size in
            var grid = Path()
            for f in [1.0 / 3, 2.0 / 3] {
                grid.move(to: CGPoint(x: size.width * f, y: 0)); grid.addLine(to: CGPoint(x: size.width * f, y: size.height))
                grid.move(to: CGPoint(x: 0, y: size.height * f)); grid.addLine(to: CGPoint(x: size.width, y: size.height * f))
            }
            ctx.stroke(grid, with: .color(AlokColors.text.opacity(0.12)), lineWidth: 1)

            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            let r: CGFloat = 64
            ctx.stroke(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)), with: .color(AlokColors.amber), lineWidth: 2.5)
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - 5, y: c.y - 5, width: 10, height: 10)), with: .color(AlokColors.amber))
            var ticks = Path()
            ticks.move(to: CGPoint(x: c.x, y: c.y - r)); ticks.addLine(to: CGPoint(x: c.x, y: c.y - r + 14))
            ticks.move(to: CGPoint(x: c.x, y: c.y + r)); ticks.addLine(to: CGPoint(x: c.x, y: c.y + r - 14))
            ticks.move(to: CGPoint(x: c.x - r, y: c.y)); ticks.addLine(to: CGPoint(x: c.x - r + 14, y: c.y))
            ticks.move(to: CGPoint(x: c.x + r, y: c.y)); ticks.addLine(to: CGPoint(x: c.x + r - 14, y: c.y))
            ctx.stroke(ticks, with: .color(AlokColors.amber), style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
