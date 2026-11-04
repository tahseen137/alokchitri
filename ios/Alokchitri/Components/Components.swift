import SwiftUI

/// Amber capsule, at least 56 pt tall: long Bangla labels wrap and the button grows. One per screen.
struct PrimaryButton: View {
    let title: String
    var systemImage: String? = nil
    var enabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if let systemImage {
                    Image(systemName: systemImage).font(.system(size: 20, weight: .semibold))
                }
                Text(title).font(AlokType.button).multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity, minHeight: 56)
            .padding(.horizontal, 20)
            .foregroundStyle(enabled ? AlokColors.onAmber : AlokColors.textMuted)
            .background(enabled ? AlokColors.amber : AlokColors.surfaceRaised, in: Capsule())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

/// Outlined capsule, at least 48 pt tall. `accent` makes the outline and text amber.
struct SecondaryButton: View {
    let title: String
    var systemImage: String? = nil
    var accent: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let systemImage { Image(systemName: systemImage).font(.system(size: 16)) }
                Text(title).font(AlokType.label).multilineTextAlignment(.center)
            }
            .frame(minHeight: 48)
            .padding(.horizontal, 18)
            .foregroundStyle(accent ? AlokColors.amber : AlokColors.text)
            .overlay(Capsule().strokeBorder(accent ? AlokColors.amber : AlokColors.outline, lineWidth: 1))
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

/// Surface card with a 1 pt outline.
struct AlokCard<Content: View>: View {
    var background: Color = AlokColors.surface
    var border: Color = AlokColors.outline
    var radius: CGFloat = AlokRadius.card
    var padding: CGFloat = 16
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0, content: content)
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(background, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: radius, style: .continuous).strokeBorder(border, lineWidth: 1))
    }
}

/// Back arrow (48 pt target) with an optional caption, as on Setup, Goal, Recommend and How-to.
struct BackBar: View {
    @EnvironmentObject var settings: AppSettings
    var title: String? = nil
    var titleBold = false
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Button(action: onBack) {
                Image(systemName: "chevron.backward")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(AlokColors.text)
                    .frame(width: 48, height: 48)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(settings.t("cd_back"))
            if let title {
                Text(title)
                    .font(titleBold ? AlokType.title : AlokType.label)
                    .foregroundStyle(titleBold ? AlokColors.text : AlokColors.textMuted)
            }
            Spacer(minLength: 0)
        }
        .frame(height: 48)
    }
}

/// One line of a settings table: muted label left, value right, 48 pt tall.
struct SettingRow: View {
    let label: String
    let value: String
    var valueColor: Color = AlokColors.amber
    var divider = true

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(label).font(AlokType.body).foregroundStyle(AlokColors.textMuted)
                Spacer(minLength: 12)
                Text(value).font(AlokType.titleMedium).foregroundStyle(valueColor).multilineTextAlignment(.trailing)
            }
            .frame(minHeight: 48)
            .padding(.horizontal, 18)
            if divider { Rectangle().fill(AlokColors.outline).frame(height: 1) }
        }
    }
}

/// 30 pt capsule showing a value such as "ISO 800" or "1/60".
struct ValueChip: View {
    let text: String
    var background: Color = AlokColors.surfaceRaised
    var height: CGFloat = 30

    var body: some View {
        Text(verbatim: text)
            .font(AlokType.label)
            .foregroundStyle(AlokColors.text)
            .padding(.horizontal, 12)
            .frame(height: height)
            .background(background, in: Capsule())
    }
}

/// Raised inset banner with a leading icon, for tips and honest "use Night mode" notes.
struct InfoBanner: View {
    let systemImage: String
    var tint: Color = AlokColors.amber
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: systemImage).font(.system(size: 20)).foregroundStyle(tint).padding(.top, 2)
            Text(text).font(AlokType.body).foregroundStyle(AlokColors.text).lineSpacing(4)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(AlokColors.surfaceRaised, in: RoundedRectangle(cornerRadius: AlokRadius.banner, style: .continuous))
    }
}

/// Five bars; `level` of them are lit (LightLevel.gauge).
struct LightGauge: View {
    let level: Int
    let description: String

    var body: some View {
        HStack(spacing: 6) {
            ForEach(1...5, id: \.self) { i in
                RoundedRectangle(cornerRadius: 5)
                    .fill(i <= level ? AlokColors.amber : AlokColors.outline)
                    .frame(height: 10)
            }
        }
        .accessibilityElement()
        .accessibilityLabel(description)
    }
}

/// The aperture mark in the app header: ring, centre dot and four ticks.
struct LogoMark: View {
    var size: CGFloat = 30
    var color: Color = AlokColors.amber

    var body: some View {
        Canvas { ctx, canvas in
            let w = canvas.width
            let c = CGPoint(x: w / 2, y: canvas.height / 2)
            let stroke = w * 0.08
            let r = w * 0.40
            ctx.stroke(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)), with: .color(color), lineWidth: stroke)
            let dot = w * 0.16
            ctx.fill(Path(ellipseIn: CGRect(x: c.x - dot, y: c.y - dot, width: dot * 2, height: dot * 2)), with: .color(color))
            var ticks = Path()
            ticks.move(to: CGPoint(x: c.x, y: w * 0.09)); ticks.addLine(to: CGPoint(x: c.x, y: w * 0.28))
            ticks.move(to: CGPoint(x: c.x, y: w * 0.72)); ticks.addLine(to: CGPoint(x: c.x, y: w * 0.91))
            ticks.move(to: CGPoint(x: w * 0.09, y: c.y)); ticks.addLine(to: CGPoint(x: w * 0.28, y: c.y))
            ticks.move(to: CGPoint(x: w * 0.72, y: c.y)); ticks.addLine(to: CGPoint(x: w * 0.91, y: c.y))
            ctx.stroke(ticks, with: .color(color), style: StrokeStyle(lineWidth: stroke, lineCap: .round))
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// Two-segment capsule: বাং | EN. Each segment is 52 × 40 inside a 48 pt touch area.
struct LanguageToggle: View {
    @EnvironmentObject var settings: AppSettings
    @Binding var language: AppLanguage

    var body: some View {
        HStack(spacing: 2) {
            segment(.bn, key: "lang_bn")
            segment(.en, key: "lang_en")
        }
        .padding(4)
        .overlay(Capsule().strokeBorder(AlokColors.outline, lineWidth: 1))
    }

    private func segment(_ lang: AppLanguage, key: String) -> some View {
        let selected = lang == language
        return Button { language = lang } label: {
            Text(settings.t(key))
                .font(AlokType.label)
                .foregroundStyle(selected ? AlokColors.onAmber : AlokColors.textMuted)
                .frame(width: 52, height: 40)
                .background(selected ? AlokColors.amber : .clear, in: Capsule())
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

/// A full-screen ground-coloured background behind a screen's content.
extension View {
    func alokScreen() -> some View {
        frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(AlokColors.ground.ignoresSafeArea())
    }
}
