import SwiftUI

/// Names a recipe before saving it from the Recommendation screen. Starts with the goal's name.
struct SaveRecipeSheet: View {
    @EnvironmentObject var settings: AppSettings
    @State var name: String
    let onCancel: () -> Void
    let onSave: (String) -> Void
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(settings.t("rec_save")).font(AlokType.title).foregroundStyle(AlokColors.text)
            VStack(alignment: .leading, spacing: 6) {
                Text(settings.t("recipes_name_label")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
                TextField("", text: $name)
                    .font(AlokType.bodyLarge).foregroundStyle(AlokColors.text)
                    .focused($focused)
                    .submitLabel(.done)
                    .onChange(of: name) { if $0.count > 40 { name = String($0.prefix(40)) } }
                    .padding(.horizontal, 16).frame(height: 52)
                    .background(AlokColors.surfaceRaised, in: RoundedRectangle(cornerRadius: AlokRadius.tile, style: .continuous))
                Text(settings.t("recipes_name_hint")).font(AlokType.caption).foregroundStyle(AlokColors.textMuted)
            }
            Spacer(minLength: 0)
            PrimaryButton(title: settings.t("picker_save"), enabled: !name.trimmingCharacters(in: .whitespaces).isEmpty) { onSave(name) }
            Button(action: onCancel) {
                Text(settings.t("perm_cancel")).font(AlokType.label).foregroundStyle(AlokColors.textMuted)
                    .frame(maxWidth: .infinity, minHeight: 48)
            }
        }
        .padding(20)
        .background(AlokColors.surface.ignoresSafeArea())
        .onAppear { focused = true }
        .presentationDetents([.height(340)])
    }
}
