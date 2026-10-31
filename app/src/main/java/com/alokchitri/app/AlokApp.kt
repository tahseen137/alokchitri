package com.alokchitri.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.alokchitri.app.ads.RewardedAds
import com.alokchitri.app.camera.Camera2PreviewHost
import com.alokchitri.app.data.BrandGuideRepository
import com.alokchitri.app.overlay.FloatingCardService
import com.alokchitri.app.ui.FeedbackViewModel
import com.alokchitri.app.ui.FlowViewModel
import com.alokchitri.app.ui.components.AlokNavBar
import com.alokchitri.app.ui.components.AlokTab
import com.alokchitri.app.ui.formatEv
import com.alokchitri.app.ui.label
import com.alokchitri.app.ui.screens.AppLanguage
import com.alokchitri.app.ui.screens.CardItem
import com.alokchitri.app.ui.screens.FeedbackScreen
import com.alokchitri.app.ui.screens.GoalScreen
import com.alokchitri.app.ui.screens.HomeScreen
import com.alokchitri.app.ui.screens.HowToScreen
import com.alokchitri.app.ui.screens.HowToStep
import com.alokchitri.app.ui.screens.MeterScreen
import com.alokchitri.app.ui.screens.PhoneBrand
import com.alokchitri.app.ui.screens.PhonePickerDialog
import com.alokchitri.app.ui.screens.RecipesScreen
import com.alokchitri.app.ui.screens.SaveRecipeDialog
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
    feedback: FeedbackViewModel = viewModel(),
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
    val recipes by flow.recipes.collectAsStateWithLifecycle()
    val feedbackState by feedback.state.collectAsStateWithLifecycle()
    val feedbackLeft by feedback.left.collectAsStateWithLifecycle()
    val feedbackLeftToday by feedback.leftToday.collectAsStateWithLifecycle()
    val billing by feedback.billing.state.collectAsStateWithLifecycle()
    // Lifetime unlock means no ads, so the SDK isn't even started for those users.
    val rewardedAds = remember(billing.unlocked) { if (billing.unlocked) null else RewardedAds(context) }
    val noAdNow = stringResource(R.string.feedback_no_ad)
    val langCode = if (language == AppLanguage.EN) "en" else "bn"
    // System photo picker: no storage permission needed (spec).
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) feedback.onPhotoPicked(uri, langCode, state.phoneName)
    }
    var showSaveRecipe by remember { mutableStateOf(false) }
    val cardTitle = stringResource(state.goal.titleRes())
    val rec = (state.result as? RecommendationResult.Success)?.recommendation
    val cardItems = rec?.let { cardItemsFor(it) }.orEmpty()
    val cardStarted = stringResource(R.string.card_started)
    // The card waiting for the overlay permission: the current shot's, or a saved recipe's.
    var pendingCard by remember { mutableStateOf<Pair<String, List<CardItem>>?>(null) }
    val launchCard: (String, List<CardItem>) -> Unit = { title, items ->
        FloatingCardService.start(context, title, items)
        Toast.makeText(context, cardStarted, Toast.LENGTH_SHORT).show()
    }
    // Android 13+: the foreground notification needs this, but the card works without it, so never block on it.
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Re-check on return: the user may have backed out of the settings page without granting.
        val card = pendingCard
        pendingCard = null
        if (card != null && FloatingCardService.canDrawOverlays(context)) launchCard(card.first, card.second)
    }
    val startCardWith: (String, List<CardItem>) -> Unit = { title, items ->
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (items.isEmpty()) Unit
        else if (FloatingCardService.canDrawOverlays(context)) launchCard(title, items)
        else { pendingCard = title to items; showPermission = true }
    }
    val startCard: () -> Unit = { startCardWith(cardTitle, cardItems) }
    if (showSaveRecipe) {
        SaveRecipeDialog(
            initialName = cardTitle,
            onDismiss = { showSaveRecipe = false },
            onSave = { name ->
                flow.saveRecipe(name)
                showSaveRecipe = false
                Toast.makeText(context, context.getString(R.string.recipes_saved), Toast.LENGTH_SHORT).show()
            },
        )
    }
    if (showPermission) {
        AlertDialog(
            onDismissRequest = { showPermission = false; pendingCard = null },
            title = { Text(stringResource(R.string.overlay_permission_title)) },
            text = { Text(stringResource(R.string.overlay_permission_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermission = false
                    permissionLauncher.launch(FloatingCardService.permissionIntent(context))
                }) { Text(stringResource(R.string.perm_allow)) }
            },
            dismissButton = { TextButton(onClick = { showPermission = false; pendingCard = null }) { Text(stringResource(R.string.perm_cancel)) } },
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
                    freeLeft = feedbackLeftToday,
                    freeTotal = feedback.quota.dailyLimit,
                    lastRecipe = recipes.firstOrNull(),
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
                    // The first recipe is free; more are part of the lifetime unlock.
                    onSaveRecipe = { if (billing.unlocked || recipes.isEmpty()) showSaveRecipe = true else nav.navigate(Routes.UPGRADE) },
                    onWhereToFind = { nav.navigate(Routes.HOWTO) },
                )
            }

            composable(Routes.HOWTO) {
                val bundled = genericSteps()
                // Null until the download (or cached copy) answers, and if neither exists: then the bundled steps.
                val guide by produceState<Map<PhoneBrand, List<HowToStep>>?>(null, langCode) {
                    value = BrandGuideRepository(context).load(langCode)
                }
                HowToScreen(
                    initialBrand = state.brand ?: PhoneBrand.XIAOMI,
                    stepsFor = { brand -> guide?.get(brand)?.takeIf { it.isNotEmpty() } ?: bundled },
                    onBack = { nav.popBackStack() },
                    onStartCard = { startCard(); nav.popBackStack() },
                )
            }

            composable(Routes.FEEDBACK) {
                FeedbackScreen(
                    state = feedbackState,
                    freeLeft = feedbackLeft,
                    onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onWatchAd = {
                        val shown = context.findActivity()?.let { activity ->
                            rewardedAds?.show(activity) { feedback.onAdRewarded(langCode, state.phoneName) }
                        } == true
                        if (!shown) Toast.makeText(context, noAdNow, Toast.LENGTH_SHORT).show()
                    },
                    showAdButton = !billing.unlocked,
                    onTopUp = { nav.navigate(Routes.UPGRADE) },
                    onRetry = { feedback.retry(langCode, state.phoneName) },
                )
            }

            composable(Routes.RECIPES) {
                val itemsById = recipes.associate { it.id to cardItemsFor(it.recommendation) }
                RecipesScreen(
                    recipes = recipes,
                    onNew = { nav.navigate(Routes.GOAL) },
                    onUse = { recipe -> startCardWith(recipe.name, itemsById.getValue(recipe.id)) },
                    onDelete = { flow.deleteRecipe(it.id) },
                )
            }

            composable(Routes.UPGRADE) {
                LaunchedEffect(Unit) { feedback.billing.connect() } // fresh prices and purchases each time it opens
                UpgradeScreen(
                    offer = UpgradeOffer(
                        price = billing.unlockPrice.takeIf { billing.ready },
                        topUpPrice = billing.topUpPrice.takeIf { billing.ready },
                        unlocked = billing.unlocked,
                        pending = billing.pending,
                    ),
                    onClose = { nav.popBackStack() },
                    onBuy = { context.findActivity()?.let { feedback.billing.buy(it, AppConfig.PRODUCT_UNLOCK) } },
                    onBuyTopUp = { context.findActivity()?.let { feedback.billing.buy(it, AppConfig.PRODUCT_TOPUP) } },
                    onGiftCardGuide = { /* TODO: bKash gift-card guide screen */ },
                    onRetry = { feedback.billing.connect() },
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
private fun genericSteps(): List<HowToStep> = listOf(
    HowToStep(stringResource(R.string.howto_1)),
    HowToStep(stringResource(R.string.howto_2)),
    HowToStep(stringResource(R.string.howto_3)),
)
