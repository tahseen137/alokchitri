import SwiftUI

@main
struct AlokchitriApp: App {
    @StateObject var settings = AppSettings()

    init() {
        AlokFonts.register()
        AlokAppearance.apply()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(settings)
                .environment(\.locale, settings.language.locale)
                .preferredColorScheme(.dark)
                .tint(AlokColors.amber)
        }
    }
}
