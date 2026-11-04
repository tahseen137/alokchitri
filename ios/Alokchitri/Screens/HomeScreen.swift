import SwiftUI

/// Screen 3 · Home. The tab bar comes from MainTabs.
struct HomeScreen: View {
    @EnvironmentObject var settings: AppSettings
    /// Free photo-feedback checks left today. Wired to the real quota with the Feedback screen.
    var freeLeft = 3
    var freeTotal = 3
    /// Name and chips of the most recent saved recipe, if any. Wired with the Recipes screen.
    var lastRecipe: (name: String, chips: [String])? = nil
    let onNewShot: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    HStack(spacing: 10) {
                        LogoMark(size: 30)
                        Text(settings.t("app_name")).font(AlokType.title).foregroundStyle(AlokColors.text)
                    }
                    Spacer()
                }
                .frame(height: 48)

                Text(settings.t("home_title")).font(AlokType.headline).foregroundStyle(AlokColors.text)

                hero

                HStack(alignment: .top, spacing: 12) {
                    AlokCard {
                        Text(settings.t("home_free_feedback")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                        Text(settings.tf("home_left_fmt", settings.number(freeLeft), settings.number(freeTotal)))
                            .font(AlokType.title).foregroundStyle(AlokColors.green).padding(.top, 6)
                    }
                    AlokCard {
                        Text(settings.t("home_last_recipe")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                        Text(verbatim: lastRecipe?.name ?? "—")
                            .font(.hind(17, .bold)).foregroundStyle(AlokColors.text).padding(.top, 6)
                        if let lastRecipe {
                            Text(verbatim: lastRecipe.chips.prefix(2).joined(separator: " · "))
                                .font(AlokType.bodySmall).foregroundStyle(AlokColors.textMuted)
                        }
                    }
                }

                AlokCard {
                    HStack(alignment: .top, spacing: 14) {
                        Image(systemName: "sun.max")
                            .font(.system(size: 22))
                            .foregroundStyle(AlokColors.amber)
                            .frame(width: 44, height: 44)
                            .background(AlokColors.surfaceRaised, in: RoundedRectangle(cornerRadius: AlokRadius.tile, style: .continuous))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(settings.t("home_tip_label")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                            Text(settings.t("home_tip_body")).font(AlokType.body).foregroundStyle(AlokColors.text).lineSpacing(4)
                        }
                    }
                }
            }
            .padding(.horizontal, AlokSpacing.screen)
            .padding(.top, 8)
            .padding(.bottom, 16)
        }
        .alokScreen()
    }

    /// Amber hero card: the whole card is the button.
    private var hero: some View {
        Button(action: onNewShot) {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text(settings.t("home_cta_tag"))
                        .font(AlokType.caption)
                        .foregroundStyle(AlokColors.amber)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 3)
                        .background(AlokColors.onAmber, in: Capsule())
                    Spacer()
                    Image(systemName: "arrow.forward").font(.system(size: 24, weight: .semibold))
                }
                Text(settings.t("home_cta_title")).font(AlokType.headlineSmall).multilineTextAlignment(.leading)
                Text(settings.t("home_cta_sub")).font(AlokType.body).multilineTextAlignment(.leading)
            }
            .padding(22)
            .frame(maxWidth: .infinity, alignment: .leading)
            .foregroundStyle(AlokColors.onAmber)
            .background(AlokColors.amber, in: RoundedRectangle(cornerRadius: AlokRadius.hero, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    HomeScreen(onNewShot: {}).environmentObject(AppSettings())
}
