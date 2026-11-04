import PhotosUI
import SwiftUI

/// Screen 9 · Photo feedback. The photo comes from the system photo picker (no permission needed).
/// `onWatchAd` / `onTopUp` are nil until the Upgrade screen lands, which hides those buttons.
struct FeedbackScreen: View {
    @EnvironmentObject var settings: AppSettings
    @ObservedObject var model: FeedbackModel
    let phone: String
    /// Shows a rewarded ad; the result closure gets false when no ad was ready. Nil hides the button (lifetime unlock).
    var onWatchAd: ((_ language: String, _ phone: String, _ shown: @escaping (Bool) -> Void) -> Void)? = nil
    var onTopUp: (() -> Void)? = nil

    @State var pickerItem: PhotosPickerItem?
    @State var noAd = false

    private var outOfQuota: Bool { if case .outOfQuota = model.state { return true } else { return false } }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(settings.t("feedback_title")).font(AlokType.headlineSmall).foregroundStyle(AlokColors.text)
                    Spacer()
                    Text(settings.tf("feedback_left_fmt", settings.number(model.left)))
                        .font(AlokType.label).foregroundStyle(model.left > 0 ? AlokColors.green : AlokColors.amber)
                        .padding(.horizontal, 14).frame(height: 36)
                        .background(AlokColors.surface, in: Capsule())
                        .overlay(Capsule().strokeBorder(AlokColors.outline, lineWidth: 1))
                }
                .frame(height: 48)

                HStack(alignment: .center, spacing: 12) {
                    PhotosPicker(selection: $pickerItem, matching: .images) { photoBox }
                        .buttonStyle(.plain)
                        .accessibilityLabel(settings.t("feedback_pick"))
                    VStack(alignment: .leading, spacing: 8) {
                        Text(settings.t("feedback_exif")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                        if model.hasPhoto && !model.chips.isEmpty {
                            FlowChips(items: model.chips, background: AlokColors.surface)
                        } else if model.hasPhoto {
                            Text(settings.t("feedback_no_exif")).font(AlokType.bodySmall).foregroundStyle(AlokColors.textMuted)
                        }
                        pickButton(hasPhoto: model.hasPhoto, title: settings.t(model.hasPhoto ? "feedback_pick_other" : "feedback_pick"))
                    }
                }

                switch model.state {
                case .ready(let result): resultCard(result)
                case .loading: skeleton
                case .error(let unreadable):
                    InfoBanner(systemImage: "exclamationmark.circle", text: settings.t(unreadable ? "feedback_unreadable" : "feedback_error"))
                    if !unreadable {
                        SecondaryButton(title: settings.t("meter_retry"), systemImage: "arrow.clockwise", accent: true) {
                            model.retry(language: settings.language.rawValue, phone: phone)
                        }
                    }
                case .outOfQuota:
                    InfoBanner(systemImage: "hourglass", text: settings.t("feedback_out"))
                case .empty: EmptyView()
                }

                if noAd {
                    InfoBanner(systemImage: "play.slash", text: settings.t("feedback_no_ad"))
                }
                if onWatchAd != nil || onTopUp != nil {
                    HStack(spacing: 10) {
                        if let onWatchAd {
                            SecondaryButton(title: settings.t("feedback_watch_ad"), systemImage: "play.circle", accent: true) {
                                noAd = false
                                onWatchAd(settings.language.rawValue, phone) { noAd = !$0 }
                            }
                        }
                        if let onTopUp {
                            SecondaryButton(title: settings.t("feedback_topup"), accent: outOfQuota, action: onTopUp)
                        }
                    }
                    .padding(outOfQuota ? 8 : 0)
                    .overlay {
                        if outOfQuota {
                            RoundedRectangle(cornerRadius: AlokRadius.card, style: .continuous).strokeBorder(AlokColors.amber, lineWidth: 2)
                        }
                    }
                }
            }
            .padding(.horizontal, AlokSpacing.screen)
            .padding(.top, 8)
            .padding(.bottom, 16)
        }
        .alokScreen()
        .onChange(of: pickerItem) { _, item in
            model.onPicked(item, language: settings.language.rawValue, phone: phone)
        }
    }

    private func pickButton(hasPhoto: Bool, title: String) -> some View {
        PhotosPicker(selection: $pickerItem, matching: .images) {
            Text(title)
                .font(AlokType.label).multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, minHeight: 48)
                .foregroundStyle(hasPhoto ? AlokColors.text : AlokColors.amber)
                .overlay(Capsule().strokeBorder(hasPhoto ? AlokColors.outline : AlokColors.amber, lineWidth: 1))
                .contentShape(Capsule())
        }
    }

    private var photoBox: some View {
        ZStack {
            RoundedRectangle(cornerRadius: AlokRadius.tile, style: .continuous).fill(AlokColors.surfaceRaised)
            if let image = model.preview {
                Image(uiImage: image).resizable().scaledToFill()
                    .frame(width: 140, height: 176).clipped()
                    .accessibilityLabel(settings.t("feedback_photo_cd"))
            } else {
                Text(settings.t("feedback_photo")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted).multilineTextAlignment(.center)
            }
        }
        .frame(width: 140, height: 176)
        .clipShape(RoundedRectangle(cornerRadius: AlokRadius.tile, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: AlokRadius.tile, style: .continuous).strokeBorder(AlokColors.outlineStrong, lineWidth: 1))
    }

    private func resultCard(_ result: FeedbackResult) -> some View {
        AlokCard {
            HStack(spacing: 8) {
                Image(systemName: "sparkles").font(.system(size: 18)).foregroundStyle(AlokColors.amber)
                Text(settings.t("feedback_ai")).font(AlokType.label).foregroundStyle(AlokColors.amber)
            }
            Text(settings.t("feedback_what")).font(AlokType.bodySmall).foregroundStyle(AlokColors.textMuted).padding(.top, 12)
            Text(verbatim: result.whatHappened).font(AlokType.body).foregroundStyle(AlokColors.text).lineSpacing(4)
            Divider().overlay(AlokColors.outline).padding(.vertical, 12)
            Text(settings.t("feedback_next")).font(.hind(14, .semibold)).foregroundStyle(AlokColors.green)
            Text(verbatim: result.nextTime).font(AlokType.body).foregroundStyle(AlokColors.text).lineSpacing(4)
        }
    }

    /// Grey bars in the AI card's place while the server answers. Pulses so it reads as working.
    private var skeleton: some View {
        PulsingSkeleton().accessibilityLabel(settings.t("feedback_loading"))
    }
}

private struct PulsingSkeleton: View {
    @State private var dim = false
    var body: some View {
        AlokCard {
            ForEach(Array([0.4, 1, 0.85, 0, 0.3, 1, 0.7].enumerated()), id: \.offset) { _, w in
                if w == 0 { Spacer().frame(height: 12) }
                else {
                    GeometryReader { geo in
                        Capsule().fill(AlokColors.surfaceRaised).frame(width: geo.size.width * w, height: 12)
                    }
                    .frame(height: 12).padding(.vertical, 4)
                }
            }
        }
        .opacity(dim ? 0.5 : 1)
        .onAppear { withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true)) { dim = true } }
    }
}
