package com.alokchitri.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alokchitri.app.camera.Camera2PreviewHost
import com.alokchitri.app.data.SampleData
import com.alokchitri.app.overlay.FloatingCardService
import com.alokchitri.app.ui.FlowViewModel
import com.alokchitri.app.ui.components.AlokNavBar
import com.alokchitri.app.ui.components.AlokTab
import com.alokchitri.app.ui.formatEv
import com.alokchitri.app.ui.label
import com.alokchitri.app.ui.screens.AppLanguage
import com.alokchitri.app.ui.screens.CardItem
import com.alokchitri.app.ui.screens.FeedbackScreen
import com.alokchitri.app.ui.screens.FeedbackUiState
import com.alokchitri.app.ui.screens.GoalScreen
import com.alokchitri.app.ui.screens.HomeScreen
import com.alokchitri.app.ui.screens.HowToScreen
import com.alokchitri.app.ui.screens.HowToStep
import com.alokchitri.app.ui.screens.MeterScreen
import com.alokchitri.app.ui.screens.PhoneBrand
import com.alokchitri.app.ui.screens.PhonePickerDialog
import com.alokchitri.app.ui.screens.PhotoMeta
import com.alokchitri.app.ui.screens.RecipesScreen
import com.alokchitri.app.ui.screens.RecommendScreen
import com.alokchitri.app.ui.screens.SetupScreen
import com.alokchitri.app.ui.screens.UpgradeOffer
import com.alokchitri.app.ui.screens.UpgradeScreen
import com.alokchitri.app.ui.screens.WelcomeScreen
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.titleRes
import com.alokchitri.engine.LightMeter
import com.alokchitri.engine.Recommendation
import com.alokchitri.engine.RecommendationResult

object Routes {
    const val WELCOME = "welcome"
    const val SETUP = "setup"
    const val HOME = "home"
    const val GOAL = "goal"
    const val METER = "meter"
    const val RECOMMEND = "recommend"
    const val HOWTO = "howto"
    const val FEEDBACK = "feedback"
    const val RECIPES = "recipes"
    const val UPGRADE = "upgrade"
}

private val TAB_ROUTES = mapOf(
    AlokTab.HOME to Routes.HOME,
    AlokTab.METER to Routes.GOAL, // the Meter tab starts a new shot, which begins at the goal picker
    AlokTab.FEEDBACK to Routes.FEEDBACK,
    AlokTab.RECIPES to Routes.RECIPES,
)

/** App shell: navigation graph + the 4-tab bar on Home, Feedback and Recipes. Screen 7 is the overlay service. */
@Composable
fun AlokApp(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    flow: FlowViewModel = viewModel(),
) {
    val nav = rememberNavController()
    val state by flow.state.collectAsStateWithLifecycle()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current

    // Floating card start: needs "display over other apps" once.
    var showPermission by remember { mutableStateOf(false) }
    var showPhonePicker by remember { mutableStateOf(false) }
    // Read once: after Setup is confirmed the next launch opens straight on Home.
    val startRoute = remember { if (flow.setupDone) Routes.HOME else Routes.WELCOME }
    val cardTitle = stringResource(state.goal.titleRes())
    val rec = (state.result as? RecommendationResult.Success)?.recommendation
    val cardItems = rec?.let { cardItemsFor(it) }.orEmpty()
    val cardStarted = stringResource(R.string.card_started)
    val launchCard: () -> Unit = {
        FloatingCardService.start(context, cardTitle, cardItems)
        Toast.makeText(context, cardStarted, Toast.LENGTH_SHORT).show()
    }
    // Android 13+: the foreground notification needs this, but the card works without it, so never block on it.
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Re-check on return: the user may have backed out of the settings page without granting.
        if (FloatingCardService.canDrawOverlays(context) && cardItems.isNotEmpty()) launchCard()
    }
    val startCard: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (cardItems.isEmpty()) Unit
        else if (FloatingCardService.canDrawOverlays(context)) launchCard()
        else showPermission = true
    }
    if (showPermission) {
        AlertDialog(
            onDismissRequest = { showPermission = false },
            title = { Text(stringResource(R.string.overlay_permission_title)) },
            text = { Text(stringResource(R.string.overlay_permission_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermission = false
                    permissionLauncher.launch(FloatingCardService.permissionIntent(context))
                }) { Text(stringResource(R.string.perm_allow)) }
            },
            dismissButton = { TextButton(onClick = { showPermission = false }) { Text(stringResource(R.string.perm_cancel)) } },
        )
    }

    if (showPhonePicker) {
        PhonePickerDialog(
            initialBrand = state.brand,
            onDismiss = { showPhonePicker = false },
            onSave = { name -> flow.setPhoneName(name); showPhonePicker = false },
        )
    }

    val currentTab = when (route) {
        Routes.HOME -> AlokTab.HOME
        Routes.FEEDBACK -> AlokTab.FEEDBACK
        Routes.RECIPES -> AlokTab.RECIPES
        else -> null
    }

    Scaffold(
        containerColor = AlokColors.Ground,
        bottomBar = {
            if (currentTab != null) AlokNavBar(currentTab) { tab -> nav.navigateTab(TAB_ROUTES.getValue(tab)) }
        },
    ) { padding ->
        NavHost(nav, startDestination = startRoute, modifier = Modifier.padding(padding)) {

            composable(Routes.WELCOME) {
                WelcomeScreen(language, onLanguageChange, onStart = { nav.navigate(Routes.SETUP) })
            }

            composable(Routes.SETUP) {
                SetupScreen(
                    phoneName = state.phoneName ?: stringResource(R.string.setup_detecting),
                    device = state.device,
                    onBack = { nav.popBackStack() },
                    onConfirm = {
                        flow.confirmSetup()
                        nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                    },
                    onNotMyPhone = { showPhonePicker = true },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    freeLeft = SampleData.FREE_FEEDBACKS_LEFT,
                    freeTotal = SampleData.FREE_FEEDBACKS_PER_DAY,
                    lastRecipe = SampleData.recipes.firstOrNull(),
                    onNewShot = { nav.navigate(Routes.GOAL) },
                    onSettings = { nav.navigate(Routes.UPGRADE) }, // TODO: real settings screen
                )
            }

            composable(Routes.GOAL) {
                GoalScreen(
                    selected = state.goal,
                    onSelect = flow::selectGoal,
                    onBack = { nav.popBackStack() },
                    onNext = { nav.navigate(Routes.METER) },
                )
            }

            composable(Routes.METER) {
                var cameraLive by remember { mutableStateOf(false) }
                MeterScreen(
                    goal = state.goal,
                    level = LightMeter.levelOf(state.ev100),
                    onBack = { nav.popBackStack() },
                    onDone = { nav.navigate(Routes.RECOMMEND) },
                    showGauge = cameraLive,
                    preview = { Camera2PreviewHost(onFrameEv = { flow.onLightReading(it) }, onCameraReady = { cameraLive = it }) },
                )
            }

            composable(Routes.RECOMMEND) {
                RecommendScreen(
                    goal = state.goal,
                    result = state.result,
                    onBack = { nav.popBackStack() },
                    onStartCard = startCard,
                    onSaveRecipe = { Toast.makeText(context, "TODO: save recipe (Room)", Toast.LENGTH_SHORT).show() },
                    onWhereToFind = { nav.navigate(Routes.HOWTO) },
                )
            }

            composable(Routes.HOWTO) {
                val stepsByBrand = PhoneBrand.entries.associateWith { genericSteps(it) }
                HowToScreen(
                    initialBrand = state.brand ?: PhoneBrand.XIAOMI,
                    stepsFor = { brand -> stepsByBrand.getValue(brand) }, // TODO: from the remote brand-guide JSON
                    onBack = { nav.popBackStack() },
                    onStartCard = { startCard(); nav.popBackStack() },
                )
            }

            composable(Routes.FEEDBACK) {
                FeedbackScreen(
                    state = FeedbackUiState.Ready(
                        PhotoMeta(listOf("ISO 3200", "1/30", stringResource(R.string.lens_main))),
                        com.alokchitri.app.ui.screens.FeedbackResult(
                            "ISO অনেক বেশি ছিল, তাই ছবিতে দানা দানা নয়েজ এসেছে। শাটারও ধীর, হাত কাঁপলে ঝাপসা হয়।",
                            "ISO 800 রাখুন, শাটার 1/60। কনুই শরীরে চেপে ধরুন বা দেয়ালে ঠেস দিন।",
                        ),
                    ), // TODO: replace with the photo-picker + Edge Function flow
                    freeLeft = SampleData.FREE_FEEDBACKS_LEFT,
                    onPickPhoto = { /* TODO: PickVisualMedia */ },
                    onWatchAd = { /* TODO: AdMob rewarded */ },
                    onTopUp = { nav.navigate(Routes.UPGRADE) },
                )
            }

            composable(Routes.RECIPES) {
                RecipesScreen(
                    recipes = SampleData.recipes,
                    onNew = { nav.navigate(Routes.UPGRADE) },
                    onUse = { /* TODO: load the recipe into the card */ },
                )
            }

            composable(Routes.UPGRADE) {
                UpgradeScreen(
                    offer = UpgradeOffer(price = "৳149", strikePrice = "৳199", topUpLabel = stringResource(R.string.topup_pack)), // TODO: Play Billing
                    onClose = { nav.popBackStack() },
                    onBuy = { /* TODO: BillingClient.launchBillingFlow */ },
                    onBuyTopUp = { /* TODO: consumable product */ },
                    onGiftCardGuide = { /* TODO: bKash gift-card guide screen */ },
                )
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** The eight rows of the floating card, in the order shown in the design. */
@Composable
private fun cardItemsFor(rec: Recommendation): List<CardItem> = listOf(
    CardItem(stringResource(R.string.rec_iso), rec.iso.toString()),
    CardItem(stringResource(R.string.rec_shutter), rec.shutterLabel),
    CardItem(stringResource(R.string.rec_wb), "${rec.whiteBalanceK}K"),
    CardItem(stringResource(R.string.rec_ev), formatEv(rec.evBias)),
    CardItem(stringResource(R.string.rec_focus), rec.focus.label()),
    CardItem(stringResource(R.string.rec_lens), rec.lens.label()),
    CardItem(stringResource(R.string.rec_format), rec.format.label()),
    CardItem(stringResource(R.string.rec_stability), rec.stability.label()),
)

@Composable
private fun genericSteps(brand: PhoneBrand): List<HowToStep> = listOf(
    HowToStep(stringResource(R.string.howto_1)),
    HowToStep(stringResource(R.string.howto_2)),
    HowToStep(stringResource(R.string.howto_3)),
)
