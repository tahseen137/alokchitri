import SwiftUI

/// Screen 11 · Lifetime unlock. Opened from Settings, Feedback (out of quota) and Recipes (second recipe).
struct UpgradeScreen: View {
    @EnvironmentObject var settings: AppSettings
    @ObservedObject var store: PurchaseStore
    let onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onClose) {
                Image(systemName: "xmark").font(.system(size: 18, weight: .semibold)).foregroundStyle(AlokColors.text)
                    .frame(width: 48, height: 48).contentShape(Rectangle())
            }
            .accessibilityLabel(settings.t("cd_close"))

            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(settings.t("upgrade_title")).font(AlokType.headline).foregroundStyle(AlokColors.text).padding(.top, 4)
                    Text(settings.t("upgrade_sub")).font(AlokType.body).foregroundStyle(AlokColors.textMuted)
                    if store.unlocked { unlockedCard } else { offerCard }
                    topUpCard
                }
            }
        }
        .padding(.horizontal, AlokSpacing.screen)
        .padding(.top, 8)
        .padding(.bottom, 24)
        .alokScreen()
    }

    private var offerCard: some View {
        AlokCard(border: AlokColors.amber, radius: AlokRadius.hero, padding: 20) {
            Text(verbatim: store.unlockPrice ?? "—").font(.hind(40, .bold)).foregroundStyle(AlokColors.amber)
            VStack(alignment: .leading, spacing: 10) {
                ForEach(["upgrade_b1", "upgrade_b2", "upgrade_b3"], id: \.self) { key in
                    HStack(spacing: 12) {
                        Image(systemName: "checkmark").font(.system(size: 16, weight: .semibold)).foregroundStyle(AlokColors.green)
                        Text(settings.t(key)).font(AlokType.bodyLarge).foregroundStyle(AlokColors.text)
                    }
                }
            }
            .padding(.vertical, 14)
            if let price = store.unlockPrice {
                PrimaryButton(title: settings.tf("upgrade_cta_fmt", price), enabled: !store.pending) {
                    Task { await store.buy(Products.unlock) }
                }
            } else {
                PrimaryButton(title: settings.t("upgrade_cta"), enabled: false) {}
            }
            Text(settings.t(store.pending ? "upgrade_pending" : store.ready ? "upgrade_play" : "upgrade_unavailable"))
                .font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                .frame(maxWidth: .infinity).multilineTextAlignment(.center).padding(.top, 8)
            if !store.ready && !store.pending {
                Button(settings.t("meter_retry")) { Task { await store.refresh() } }
                    .font(AlokType.label).foregroundStyle(AlokColors.amber)
                    .frame(maxWidth: .infinity, minHeight: 48)
            }
        }
    }

    private var unlockedCard: some View {
        AlokCard(border: AlokColors.green, radius: AlokRadius.hero, padding: 20) {
            HStack(spacing: 12) {
                Image(systemName: "checkmark").font(.system(size: 28, weight: .semibold)).foregroundStyle(AlokColors.green)
                Text(settings.t("upgrade_unlocked")).font(AlokType.headlineSmall).foregroundStyle(AlokColors.green)
            }
            Text(settings.t("upgrade_unlocked_sub")).font(AlokType.body).foregroundStyle(AlokColors.text).padding(.top, 8)
        }
    }

    private var topUpCard: some View {
        AlokCard {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(settings.t("topup_title")).font(.hind(17, .bold)).foregroundStyle(AlokColors.text)
                    Text(settings.t("topup_sub")).font(AlokType.bodySmall).foregroundStyle(AlokColors.textMuted)
                }
                Spacer(minLength: 8)
                Button {
                    Task { await store.buy(Products.topUp) }
                } label: {
                    Text(store.topUpPrice.map { settings.tf("topup_pack_fmt", $0, settings.number(Products.topUpCredits)) } ?? "—")
                        .font(AlokType.label).foregroundStyle(store.topUpPrice == nil ? AlokColors.textMuted : AlokColors.text)
                        .padding(.horizontal, 16).frame(minHeight: 48)
                        .overlay(Capsule().strokeBorder(AlokColors.outline, lineWidth: 1))
                }
                .buttonStyle(.plain)
                .disabled(store.topUpPrice == nil)
            }
        }
    }
}
