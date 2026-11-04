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
    @StateObject var recipes = RecipeStore()
    @StateObject var feedback = FeedbackModel()
    @StateObject var store = PurchaseStore()
    @State var showUpgrade = false

    var body: some View {
        TabView(selection: $tab) {
            HomeTab(freeLeft: feedback.leftToday, freeTotal: feedback.quota.dailyLimit, onNewShot: { flow.startNewShot(); tab = .meter }, onUpgrade: { showUpgrade = true })
                .tabItem { Label(settings.t("nav_home"), systemImage: "house") }
                .tag(AlokTab.home)
            MeterFlow(onExit: { tab = .home }, onUpgrade: { showUpgrade = true })
                .environmentObject(flow)
                .tabItem { Label(settings.t("nav_meter"), systemImage: "camera.aperture") }
                .tag(AlokTab.meter)
            FeedbackScreen(model: feedback, phone: DeviceProbe.phoneName(), onTopUp: { showUpgrade = true })
                .tabItem { Label(settings.t("nav_feedback"), systemImage: "photo") }
                .tag(AlokTab.feedback)
            RecipesTab(onNew: { flow.startNewShot(); tab = .meter })
                .tabItem { Label(settings.t("nav_recipes"), systemImage: "bookmark") }
                .tag(AlokTab.recipes)
        }
        .environmentObject(recipes)
        .environmentObject(store)
        .onChange(of: store.revision) { _, _ in feedback.refreshQuota() }
        .fullScreenCover(isPresented: $showUpgrade) {
            UpgradeScreen(store: store, onClose: { showUpgrade = false })
                .environmentObject(settings)
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

/// New shot: Goal, Meter, Recommendation, then our own Pro camera.
private struct MeterFlow: View {
    @EnvironmentObject var flow: FlowModel
    @EnvironmentObject var recipes: RecipeStore
    @EnvironmentObject var store: PurchaseStore
    @State var path: [Step] = []
    let onExit: () -> Void
    let onUpgrade: () -> Void

    enum Step: Hashable { case meter, recommend, proCamera }

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
                        if let result = flow.result {
                            RecommendScreen(
                                goal: flow.goal,
                                result: result,
                                onBack: { path.removeLast() },
                                onShoot: { path.append(.proCamera) },
                                canSave: store.unlocked || recipes.recipes.isEmpty,
                                onNeedUpgrade: onUpgrade,
                                onSave: { name in
                                    if case .success(let rec) = result { recipes.add(name: name, goal: flow.goal, recommendation: rec) }
                                },
                                onHome: { path = []; onExit() }
                            )
                            .toolbar(.hidden, for: .navigationBar)
                            .toolbar(.hidden, for: .tabBar)
                        } else {
                            // No light reading (camera off): go back and measure again.
                            ComingSoon().toolbar(.hidden, for: .navigationBar)
                                .onAppear { path.removeLast() }
                        }
                    case .proCamera:
                        if case .success(let rec) = flow.result {
                            ProCameraScreen(goal: flow.goal, rec: rec, onBack: { path.removeLast() })
                                .toolbar(.hidden, for: .navigationBar)
                                .toolbar(.hidden, for: .tabBar)
                        }
                    }
                }
        }
    }
}

/// Home, with Settings one tap away from the gear.
private struct HomeTab: View {
    @EnvironmentObject var recipes: RecipeStore
    @State var showSettings = false
    let freeLeft: Int
    let freeTotal: Int
    let onNewShot: () -> Void
    let onUpgrade: () -> Void

    var body: some View {
        NavigationStack {
            HomeScreen(
                freeLeft: freeLeft,
                freeTotal: freeTotal,
                lastRecipe: recipes.last.map { (name: $0.name, chips: $0.recommendation.chips) },
                onNewShot: onNewShot,
                onSettings: { showSettings = true }
            )
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(isPresented: $showSettings) {
                SettingsScreen(phoneName: DeviceProbe.phoneName(), onUpgrade: onUpgrade, onBack: { showSettings = false })
                    .toolbar(.hidden, for: .navigationBar)
                    .toolbar(.hidden, for: .tabBar)
            }
        }
    }
}

/// Saved recipes; "shoot" opens the Pro camera with a recipe's settings.
private struct RecipesTab: View {
    @State var shooting: SavedRecipe?
    let onNew: () -> Void

    var body: some View {
        NavigationStack {
            RecipesScreen(onNew: onNew, onUse: { shooting = $0 })
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(item: $shooting) { recipe in
                    ProCameraScreen(goal: recipe.goal, rec: recipe.recommendation, onBack: { shooting = nil })
                        .toolbar(.hidden, for: .navigationBar)
                        .toolbar(.hidden, for: .tabBar)
                }
        }
    }
}
