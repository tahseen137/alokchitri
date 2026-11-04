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
    @StateObject var flow = FlowModel()

    var body: some View {
        TabView(selection: $tab) {
            HomeScreen(onNewShot: { flow.startNewShot(); tab = .meter })
                .tabItem { Label(settings.t("nav_home"), systemImage: "house") }
                .tag(AlokTab.home)
            MeterFlow(onExit: { tab = .home })
                .environmentObject(flow)
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

/// New shot: Goal, then Meter. The Recommendation step arrives in the next pull request.
private struct MeterFlow: View {
    @EnvironmentObject var flow: FlowModel
    @State var path: [Step] = []
    let onExit: () -> Void

    enum Step: Hashable { case meter, recommend }

    var body: some View {
        NavigationStack(path: $path) {
            GoalScreen(selected: $flow.goal, onBack: onExit, onNext: { path.append(.meter) })
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: Step.self) { step in
                    switch step {
                    case .meter:
                        MeterScreen(goal: flow.goal, onBack: { path.removeLast() }, onDone: { reading in
                            flow.setReading(reading)
                            path.append(.recommend)
                        })
                        .toolbar(.hidden, for: .navigationBar)
                        .toolbar(.hidden, for: .tabBar)
                    case .recommend:
                        ComingSoon()
                            .toolbar(.hidden, for: .navigationBar)
                    }
                }
        }
    }
}
