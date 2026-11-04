import AlokEngine
import SwiftUI

/// First run: Welcome, then Setup. After Setup is confirmed the app opens on the tabs.
struct RootView: View {
    @EnvironmentObject var settings: AppSettings

    var body: some View {
        if settings.setupDone {
            MainTabs()
        } else {
            FirstRunFlow()
        }
    }
}

private struct FirstRunFlow: View {
    @EnvironmentObject var settings: AppSettings
    @State var showSetup = false

    var body: some View {
        NavigationStack {
            WelcomeScreen(
                language: $settings.language,
                onStart: { showSetup = true }
            )
            .navigationDestination(isPresented: $showSetup) {
                SetupScreen(
                    camera: DeviceProbe.probe(),
                    onBack: { showSetup = false },
                    onConfirm: { settings.setupDone = true }
                )
                .navigationBarBackButtonHidden()
            }
        }
    }
}

enum AlokTab: Hashable { case home, meter, feedback, recipes }

/// The four tabs from the design. Meter, Feedback and Recipes arrive in the next pull requests.
struct MainTabs: View {
    @EnvironmentObject var settings: AppSettings
    @State var tab: AlokTab = .home

    var body: some View {
        TabView(selection: $tab) {
            HomeScreen(onNewShot: { tab = .meter })
                .tabItem { Label(settings.t("nav_home"), systemImage: "house") }
                .tag(AlokTab.home)
            ComingSoon()
                .tabItem { Label(settings.t("nav_meter"), systemImage: "camera.aperture") }
                .tag(AlokTab.meter)
            ComingSoon()
                .tabItem { Label(settings.t("nav_feedback"), systemImage: "photo") }
                .tag(AlokTab.feedback)
            ComingSoon()
                .tabItem { Label(settings.t("nav_recipes"), systemImage: "bookmark") }
                .tag(AlokTab.recipes)
        }
    }
}

/// Placeholder for screens still being ported. Never shipped: each one is replaced before release.
private struct ComingSoon: View {
    var body: some View {
        ZStack {
            AlokColors.ground.ignoresSafeArea()
            Text(verbatim: "…").font(AlokType.headline).foregroundStyle(AlokColors.textMuted)
        }
    }
}
