import SwiftUI

/// Screen 1 · Welcome: header (logo, language toggle) / illustration / headline + body / dots / start button.
struct WelcomeScreen: View {
    @EnvironmentObject var settings: AppSettings
    @Binding var language: AppLanguage
    let onStart: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HStack(spacing: 10) {
                    LogoMark(size: 32)
                    Text(settings.t("app_name")).font(AlokType.headlineSmall).foregroundStyle(AlokColors.text)
                }
                Spacer()
                LanguageToggle(language: $language)
            }
            .frame(height: 48)

            PhoneIllustration(description: settings.t("welcome_illustration_cd"))
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            Text(settings.t("welcome_title"))
                .font(AlokType.display)
                .foregroundStyle(AlokColors.text)
                .fixedSize(horizontal: false, vertical: true)
            Text(settings.t("welcome_body"))
                .font(AlokType.bodyLarge)
                .foregroundStyle(AlokColors.textMuted)
                .lineSpacing(4)
                .padding(.top, 12)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 8) {
                Capsule().fill(AlokColors.amber).frame(width: 24, height: 8)
                Circle().fill(AlokColors.outline).frame(width: 8, height: 8)
                Circle().fill(AlokColors.outline).frame(width: 8, height: 8)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
            .accessibilityHidden(true)

            PrimaryButton(title: settings.t("welcome_cta"), action: onStart)
        }
        .padding(.horizontal, 24)
        .padding(.top, 8)
        .padding(.bottom, 28)
        .alokScreen()
    }
}

/// Phone outline with a lens dial and three sliders, plus three floating value chips (320 × 300).
private struct PhoneIllustration: View {
    let description: String

    var body: some View {
        ZStack(alignment: .topLeading) {
            Canvas { ctx, _ in
                let phone = Path(roundedRect: CGRect(x: 95, y: 10, width: 130, height: 270), cornerRadius: 26)
                ctx.fill(phone, with: .color(AlokColors.surface))
                ctx.stroke(phone, with: .color(AlokColors.text.opacity(0.45)), lineWidth: 2)

                let c = CGPoint(x: 160, y: 86)
                ctx.stroke(Path(ellipseIn: CGRect(x: c.x - 36, y: c.y - 36, width: 72, height: 72)), with: .color(AlokColors.amber), lineWidth: 3)
                ctx.fill(Path(ellipseIn: CGRect(x: c.x - 14, y: c.y - 14, width: 28, height: 28)), with: .color(AlokColors.amber))
                var ticks = Path()
                let tickEnds: [(CGPoint, CGPoint)] = [
                    (CGPoint(x: 160, y: 36), CGPoint(x: 160, y: 46)),
                    (CGPoint(x: 160, y: 126), CGPoint(x: 160, y: 136)),
                    (CGPoint(x: 110, y: 86), CGPoint(x: 120, y: 86)),
                    (CGPoint(x: 200, y: 86), CGPoint(x: 210, y: 86)),
                ]
                for (a, b) in tickEnds {
                    ticks.move(to: a)
                    ticks.addLine(to: b)
                }
                ctx.stroke(ticks, with: .color(AlokColors.amber), style: StrokeStyle(lineWidth: 3, lineCap: .round))

                let sliders: [(y: CGFloat, to: CGFloat, color: Color)] = [
                    (170, 169, AlokColors.amber), (205, 145, AlokColors.green), (240, 187, AlokColors.amber),
                ]
                for (y, to, color) in sliders {
                    var track = Path()
                    track.move(to: CGPoint(x: 115, y: y)); track.addLine(to: CGPoint(x: 205, y: y))
                    ctx.stroke(track, with: .color(AlokColors.outline), style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    var fill = Path()
                    fill.move(to: CGPoint(x: 115, y: y)); fill.addLine(to: CGPoint(x: to, y: y))
                    ctx.stroke(fill, with: .color(color), style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    ctx.fill(Path(ellipseIn: CGRect(x: to - 8, y: y - 8, width: 16, height: 16)), with: .color(AlokColors.text))
                }
            }
            ValueChip(text: "ISO 800", height: 40).offset(x: 14, y: 120)
            ValueChip(text: "1/60", height: 40).offset(x: 232, y: 170)
            ValueChip(text: "+0.3 EV", height: 40).offset(x: 8, y: 222)
        }
        .frame(width: 320, height: 300)
        .accessibilityElement()
        .accessibilityLabel(description)
    }
}

#Preview {
    WelcomeScreen(language: .constant(.bn), onStart: {})
        .environmentObject(AppSettings())
}
