import AlokEngine
import SwiftUI

/// Screen 10 · Saved recipes. "+" starts a new shot (a recipe is saved from its Recommendation screen);
/// "Shoot with these settings" opens the Pro camera with the recipe's values already applied.
struct RecipesScreen: View {
    @EnvironmentObject var settings: AppSettings
    @EnvironmentObject var store: RecipeStore
    let onNew: () -> Void
    let onUse: (SavedRecipe) -> Void

    @State var pendingDelete: SavedRecipe?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(settings.t("recipes_title")).font(AlokType.headlineSmall).foregroundStyle(AlokColors.text)
                    Spacer()
                    Button(action: onNew) {
                        Image(systemName: "plus").font(.system(size: 20, weight: .bold)).foregroundStyle(AlokColors.onAmber)
                            .frame(width: 48, height: 48).background(AlokColors.amber, in: Circle())
                    }
                    .accessibilityLabel(settings.t("recipes_new_cd"))
                }
                .frame(height: 48)
                Text(settings.t("recipes_sub")).font(AlokType.body).foregroundStyle(AlokColors.textMuted).lineSpacing(4)

                ForEach(store.recipes) { recipe in card(recipe) }

                HStack(spacing: 14) {
                    Image(systemName: "plus").font(.system(size: 18)).foregroundStyle(AlokColors.textMuted)
                        .frame(width: 44, height: 44).background(AlokColors.surface, in: Circle())
                    Text(settings.t("recipes_empty")).font(AlokType.body).foregroundStyle(AlokColors.textMuted)
                    Spacer(minLength: 0)
                }
                .padding(18)
                .overlay(
                    RoundedRectangle(cornerRadius: AlokRadius.card, style: .continuous)
                        .strokeBorder(AlokColors.outlineStrong, style: StrokeStyle(lineWidth: 1, dash: [10, 8]))
                )
            }
            .padding(.horizontal, AlokSpacing.screen)
            .padding(.top, 8)
            .padding(.bottom, 16)
        }
        .alokScreen()
        .confirmationDialog(
            pendingDelete.map { settings.tf("recipes_delete_title", $0.name) } ?? "",
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
            titleVisibility: .visible
        ) {
            Button(settings.t("recipes_delete"), role: .destructive) {
                if let recipe = pendingDelete { store.delete(recipe) }
                pendingDelete = nil
            }
            Button(settings.t("perm_cancel"), role: .cancel) { pendingDelete = nil }
        }
    }

    private func card(_ recipe: SavedRecipe) -> some View {
        AlokCard {
            HStack {
                Image(systemName: "bookmark.fill").font(.system(size: 20)).foregroundStyle(AlokColors.amber)
                VStack(alignment: .leading, spacing: 0) {
                    Text(verbatim: recipe.name).font(AlokType.titleMedium).foregroundStyle(AlokColors.text).lineLimit(2)
                    Text(settings.t(recipe.goal.titleKey)).font(AlokType.bodySmall).foregroundStyle(AlokColors.textMuted)
                }
                .padding(.leading, 10)
                Spacer()
                Button { pendingDelete = recipe } label: {
                    Image(systemName: "trash").font(.system(size: 18)).foregroundStyle(AlokColors.textMuted)
                        .frame(width: 48, height: 48).contentShape(Rectangle())
                }
                .accessibilityLabel(settings.t("recipes_delete_cd"))
            }
            FlowChips(items: recipe.recommendation.chips).padding(.top, 10)
            Button { onUse(recipe) } label: {
                Text(settings.t("rec_cta_pro")).font(AlokType.label).foregroundStyle(AlokColors.amber)
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .background(AlokColors.amberTint, in: Capsule())
                    .overlay(Capsule().strokeBorder(AlokColors.amber, lineWidth: 1))
            }
            .buttonStyle(.plain)
            .padding(.top, 10)
        }
    }
}

/// Chips that wrap onto new lines.
struct FlowChips: View {
    let items: [String]

    var body: some View {
        FlowLayout(spacing: 6) {
            ForEach(items, id: \.self) { ValueChip(text: $0) }
        }
    }
}

/// A simple wrapping row layout (iOS 16+).
struct FlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0, maxX: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width { x = 0; y += rowHeight + spacing; rowHeight = 0 }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            maxX = max(maxX, x - spacing)
        }
        return CGSize(width: maxX, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowHeight: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX { x = bounds.minX; y += rowHeight + spacing; rowHeight = 0 }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
