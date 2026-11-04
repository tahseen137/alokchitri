import AlokEngine
import SwiftUI

/// Screen 4 · Shot goal: two columns of six tiles, in the order of the spec.
struct GoalScreen: View {
    @EnvironmentObject var settings: AppSettings
    @Binding var selected: ShotGoal
    let onBack: () -> Void
    let onNext: () -> Void

    private let rows: [(ShotGoal, ShotGoal)] = [
        (.portrait, .foodProduct),
        (.indoorLowLight, .motion),
        (.nightCity, .landscapeSunset),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            BackBar(title: settings.t("goal_step"), onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text(settings.t("goal_title")).font(AlokType.headline).foregroundStyle(AlokColors.text).padding(.top, 12)
                    Text(settings.t("goal_sub")).font(AlokType.body).foregroundStyle(AlokColors.textMuted).padding(.top, 8)
                    VStack(spacing: 12) {
                        ForEach(rows.indices, id: \.self) { i in
                            HStack(spacing: 12) {
                                tile(rows[i].0)
                                tile(rows[i].1)
                            }
                        }
                    }
                    .padding(.top, 20)
                }
            }
            PrimaryButton(title: settings.t("goal_cta"), action: onNext).padding(.top, 12)
        }
        .padding(.horizontal, AlokSpacing.screen)
        .padding(.top, 8)
        .padding(.bottom, 28)
        .alokScreen()
    }

    private func tile(_ goal: ShotGoal) -> some View {
        GoalTile(
            title: settings.t(goal.titleKey),
            subtitle: goal == .motion ? settings.t("goal_motion_sub") : nil,
            systemImage: goal.systemImage,
            selected: goal == selected
        ) { selected = goal }
    }
}

/// Selectable tile: 136 pt tall, amber outline and tint when selected.
struct GoalTile: View {
    let title: String
    var subtitle: String? = nil
    let systemImage: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading) {
                Image(systemName: systemImage)
                    .font(.system(size: 28))
                    .foregroundStyle(selected ? AlokColors.amber : AlokColors.textMuted)
                Spacer(minLength: 8)
                Text(title).font(.hind(17, .bold)).foregroundStyle(AlokColors.text).multilineTextAlignment(.leading)
                if let subtitle {
                    Text(subtitle).font(AlokType.bodySmall).foregroundStyle(AlokColors.textMuted)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 136, maxHeight: 136, alignment: .leading)
            .background(selected ? AlokColors.amberTint : AlokColors.surface, in: RoundedRectangle(cornerRadius: AlokRadius.card, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: AlokRadius.card, style: .continuous)
                    .strokeBorder(selected ? AlokColors.amber : AlokColors.outline, lineWidth: selected ? 2 : 1)
            )
            .contentShape(RoundedRectangle(cornerRadius: AlokRadius.card, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
