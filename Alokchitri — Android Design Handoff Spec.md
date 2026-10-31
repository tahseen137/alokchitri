# Alokchitri — Android Design Handoff Spec

Oct 30, 2026 · @Motu

## Overview

This spec turns the 11 Alokchitri screens into buildable Jetpack Compose UI: every color, size and state a developer needs, with the Kotlin starter project as the reference implementation.

- **Design source:** the [Alokchitri design canvas](https://claude.ai/artifact/WtCDwVGnAyPkVcP81qGghF). Each phone frame is 390 × 844 dp.
- **Platform:** native Android, Kotlin, Jetpack Compose, Material 3. Suggested minSdk 26, because most phones in the target market (Xiaomi, Samsung, Vivo, Realme, Oppo) run Android 8 or newer.
- **Language:** Bangla is the default locale, English is the toggle. All copy lives in `strings.xml`; the design's Bangla text is a first draft and needs native review.
- **Placeholder data:** the phone model, ISO and shutter numbers, recipe names and prices are sample values. Real values come from Camera2, the rules engine and Google Play Billing.
- **Out of scope for v1:** AI scene detection, Vivo/Oppo/Infinix/Tecno guides, progress tracking, sharing to Facebook (all v1.1 in the feature spec).

## Design tokens

The app is dark-only in v1: a near-black "viewfinder" ground with amber for light and action, and green for done or OK. In Compose these live in `ui/theme/Color.kt`.

| Token | Hex | Use | Contrast on ground |
| --- | --- | --- | --- |
| Ground | #0E1014 | Screen background | n/a |
| Surface | #171A21 | Cards, nav bar, sheets | n/a |
| Surface raised | #20242D | Icon tiles, inset banners, screenshot boxes | n/a |
| Outline | #2B303B | Card borders, dividers | decorative only |
| Text | #F3F1EC | Primary text | 16.9 : 1 |
| Text muted | #A3A9B5 | Secondary text, inactive icons | 8.1 : 1 |
| Amber | #FFB547 | Primary action, values, selected state | 10.8 : 1 |
| Amber tint | #3A2D12 | Selected chip, nav indicator | n/a |
| On amber | #1A1200 | Text and icons on amber fills | 10.6 : 1 on amber |
| Green | #4FD6A5 | Done, supported, free quota | 10.4 : 1 |

**Type.** One family, Hind Siliguri (Bangla and Latin), weights 400, 500, 600, 700, bundled in `res/font` so it works offline. Never go below 13 sp.

| Role | Size / line | Weight | Used for |
| --- | --- | --- | --- |
| Display | 32 / 42 sp | 700 | Welcome and upgrade headlines |
| Headline | 28–30 / 38 sp | 700 | Screen titles |
| Title | 19–20 sp | 700 | Card titles, setting values |
| Body | 16–17 / 25 sp | 400–600 | Paragraphs, rows |
| Label | 13–15 sp | 500–600 | Nav labels, chips, captions |
| Button | 18–19 sp | 700 | Primary button text |

**Shape and spacing.** Screen padding 20 dp (24 dp on welcome). Card radius 24 dp, hero card 28 dp, sheet top corners 32 dp, buttons and chips fully rounded (pill). Gaps between cards 12–16 dp. Minimum touch target 48 dp. The floating card has a 12 dp / 0.55 alpha black shadow; nothing else is elevated.

## Shared components

All live in `ui/components/Components.kt`. Build each once and reuse it; screens below refer to them by name.

| Component | Spec | Used on |
| --- | --- | --- |
| PrimaryButton | Full width, 56 dp tall, pill. Amber fill, on-amber text (button style, 700). Optional 22 dp leading icon with 10 dp gap. | Welcome, Setup, Goal, Meter, Recommend, Upgrade |
| SecondaryButton | 48 dp tall, pill, outline border, optional 18 dp icon. `accent = true` makes the border and text amber. | Feedback, Recommend, Recipes |
| AlokCard | Surface fill, 24 dp radius, 1 dp outline border, default padding 16 dp. `borderColor` and `shape` can be overridden (amber border and 28 dp hero shape on Upgrade). | Almost every screen |
| BackBar | 48 dp row: 48 dp back icon button, optional title (8 dp gap). | Setup, Goal, Meter, Recommend, How-to |
| SettingRow | 48 dp tall, 18 dp horizontal padding, label left, value right, optional divider. | Setup, Recommend |
| ValueChip | 30 dp tall pill, 12 dp horizontal padding, raised-surface fill by default. Shows ISO, shutter and EV values. | Welcome, Recommend, Feedback, Recipes |
| InfoBanner | Raised-surface fill, banner radius, 16 × 14 dp padding, 22 dp icon (amber or green) and body text. | Recommend, Upgrade, How-to |
| LightGauge | Five segments, 10 dp tall, 6 dp gaps. Filled segments are amber. Takes a level (1–5) and a spoken description. | Meter |
| GoalTile | 136 dp tall, 16 dp padding, 32 dp icon top, label bottom. Selected: amber icon and amber tint fill. | Goal |
| LogoMark | Canvas-drawn aperture mark, 30 dp default, amber. | Welcome, Home |
| AlokNavBar | Material 3 NavigationBar, 80 dp, surface fill, no tonal elevation. Four tabs: Home, Meter (starts a new shot at Goal), Feedback, Recipes. Selected indicator uses amber tint. | Home, Feedback, Recipes only |
| LanguageToggle | Two-segment pill, 52 × 40 dp segments, selected segment amber. Changing it calls `AppCompatDelegate.setApplicationLocales`. | Welcome |

Every interactive element must give a touch target of at least 48 dp. Where the visible shape is smaller (ValueChip, LanguageToggle segments), pad the clickable area rather than the visual.

## Navigation map

&#91;embedded content: navigation map · 11 screens, 1 overlay\]

Routes are strings in `Routes` (AlokApp.kt) and one `NavHost` owns them. Welcome and Setup are popped off the back stack once Setup is confirmed, so Back from Home exits the app. The "Light meter" tab in the bottom bar is not a screen of its own: it starts a new shot at Goal. Back on Goal, Meter, Recommendation and How-to returns to the previous step. Upgrade is a plain push with a close button, so it returns to whichever screen opened it. The floating card is not a destination: it is a foreground service that draws over other apps and is started from Recommendation or from the end of the How-to flow.

## Screens 1 to 6: first run and the shot flow

Each composable takes its data and callbacks as parameters and holds no business logic, so it can be previewed and tested alone. Files are in `ui/screens/`.

| Screen | Layout, top to bottom | Data in | Actions out |
| --- | --- | --- | --- |
| 1 Welcome (`WelcomeScreen`) | 24 dp padding. Header row (48 dp): logo mark and name left, language toggle right. Phone illustration 320 × 300 dp, centred in the remaining space. Display headline, body text. Three page dots (active one is a 24 × 8 dp amber pill). Primary button. | Current `AppLanguage` (BN or EN). | `onLanguageChange`, `onStart` to Setup. |
| 2 Phone setup (`SetupScreen`) | BackBar with step title. Headline and sub. Phone card with 56 dp icon tile, phone name and a green "manual controls found" line (amber if missing). Card of four SettingRows: ISO range, shutter range, lenses, manual controls. Info note. Primary button, then a 48 dp text button "not my phone". | Phone name and `DeviceLimits` from `DeviceProbe` (Camera2). | `onBack`, `onConfirm` to Home (clears Welcome and Setup from the back stack), `onNotMyPhone`. |
| 3 Home (`HomeScreen`) | Scrolling column, 20 dp padding, 16 dp gaps. Header with logo, name and a 48 dp settings button. Headline. Amber hero card (28 dp radius, whole surface is the button). Two half-width cards: free feedbacks left (green), last recipe. Tip card with 44 dp icon tile. Bottom bar from the scaffold. | `freeLeft`, `freeTotal`, last saved recipe or null. | `onNewShot` to Goal, `onSettings` (opens Upgrade for now). |
| 4 Goal (`GoalScreen`) | BackBar with step title. Headline and sub. Two columns of GoalTiles with 12 dp gaps, six goals in this order: portrait, food, indoor low light, motion, night city, landscape or sunset. Motion shows an extra subtitle. Primary button pinned at the bottom. | Selected `ShotGoal`. | `onSelect(goal)`, `onNext` to Meter, `onBack`. |
| 5 Meter (`MeterScreen`) | Top 620 dp is the live camera preview with a rule-of-thirds grid and amber spot-meter reticle. Back button on the left and an amber-tint pill with the chosen goal on the right, both over the preview. Hint banner. Bottom sheet (32 dp top corners, surface fill): "light now" label with the level name, LightGauge, dark and bright end labels, primary button. | Camera preview composable (placeholder by default), `LightLevel` from the smoothed EV100. | `onBack`, `onDone` to Recommendation. |
| 6 Recommendation (`RecommendScreen`) | Header row: BackBar left, amber-tint goal pill right. Headline. One InfoBanner per advice item from the engine (green for a friendly alternative such as Night mode, amber for a limitation). Card of SettingRows: ISO, shutter, white balance in K, focus, EV bias, lens, format, stability. Primary button with a picture-in-picture icon, then two half-width secondary buttons: save recipe, where to find it. | `RecommendationResult` from the rules engine: `Success` or `ManualUnsupported`. | `onStartCard`, `onSaveRecipe`, `onWhereToFind` to How-to, `onBack`. |

The settings table on screen 6 is read directly from the engine's `Recommendation`; do not recompute values in the UI. If the engine returns `ManualUnsupported`, show the explanation instead of the table and buttons; Back stays in the header.

## Screens 7 to 11: card, guides, feedback, recipes, upgrade

| Screen | Layout, top to bottom | Data in | Actions out |
| --- | --- | --- | --- |
| 7 Floating card (`FloatingCard`) | Drawn by a foreground service in a `TYPE_APPLICATION_OVERLAY` window, top-start gravity, not focusable so the phone's camera app keeps input. 300 dp wide, card radius, 12 dp shadow. Header: drag handle icon and title left, 48 dp minimise button right. Checklist of 44 dp rows (label left, amber value right, 24 dp checkbox with 7 dp radius). Shows five rows, then scrolls, with a "+N more" line. Minimised: a 48 dp pill with the progress text, tap to expand. | List of `CardItem(label, value)` already resolved to strings, because the service has no Activity context. Checked set. | Toggle a row, drag the header, minimise or expand, close. |
| 8 Brand how-to (`HowToScreen`) | BackBar with bold title. Brand tabs Xiaomi, Samsung, Realme, 44 dp tall, equal width. Scrolling list of step cards (14 dp padding): an 84 × 112 dp screenshot box on the left, step number and text on the right. Green InfoBanner note. Primary button. | `PhoneBrand` selection and a list of `HowToStep(text, screenshotUrl?)`. v1 ships generic steps; real menu paths come from a remote brand-guide JSON. | `onBrandChange`, `onStartCard` (starts the card and returns to Recommendation), `onBack`. |
| 9 Photo feedback (`FeedbackScreen`) | Header: title left, green "N left" pill right. Row with a 140 × 176 dp photo (20 dp corners) and, beside it, EXIF chips plus a "pick another" secondary button. AI card (amber label, "what happened", divider, green "next time"). Two secondary buttons: watch an ad (amber), buy a top-up. | `FeedbackUiState` (Empty, Loading, Ready, OutOfQuota, Error), `freeLeft`. Result text comes from the Edge Function in the user's language. | `onPickPhoto` (system photo picker, no storage permission), `onWatchAd`, `onTopUp` to Upgrade. |
| 10 Recipes (`RecipesScreen`) | Header: title left, 48 dp amber add button right. Sub text. One card per recipe: amber bookmark icon, name, wrap row of ValueChips, a 44 dp "use" button. Last, a dashed-border empty slot with a plus tile and hint text. | List of saved recipes (name and chips). | Use a recipe, add a new one (opens Upgrade for non-buyers, see open questions). |
| 11 Upgrade (`UpgradeScreen`) | 48 dp close button. Display headline, sub. Hero card (amber border, 28 dp radius): price at 40 sp in amber, optional struck-through old price with an "early user" tag, four green-tick benefits, primary button with the price, small Google Play line. Top-up card with a 48 dp outlined price pill. Gift-card banner at the bottom (green icon). | `UpgradeOffer(price, strikePrice?, topUpLabel)`. Prices must be the formatted strings from Google Play, never hard-coded. | `onClose`, `onBuy`, `onBuyTopUp`, `onGiftCardGuide`. |

The only things that talk to the outside world from these screens are the overlay service (screen 7), the photo picker and Edge Function (9), Google Play Billing (11) and the brand-guide download (8). Everything else is local.

## States and edge cases

| Screen | Case | Behaviour |
| --- | --- | --- |
| 2 Setup | Camera2 reports no manual sensor control | Phone card line turns amber ("manual controls missing"), the manual row reads "unavailable". The engine will return `ManualUnsupported` later, so say so here and still let the user continue. |
| 2 Setup | Phone name is wrong | "Not my phone" is a text button. v1: let the user pick a brand and model from a list; the probe values still come from Camera2. |
| 3 Home | No saved recipe yet | Last-recipe card shows a dash and no chips. |
| 3 Home | Free quota used | Count turns to 0 of 3; do not hide the card, so the limit is visible before the user hits it on Feedback. |
| 5 Meter | Camera permission denied | Replace the preview with a short explanation and a button to the system settings page; keep the gauge hidden. |
| 5 Meter | Light changes quickly | The engine's smoother damps EV so the gauge does not flicker; only move the gauge when the level changes. |
| 5 Meter | Camera in use by another app | Show the same explanation with a retry button. |
| 6 Recommend | `ManualUnsupported` | Explanation instead of table and buttons (see screens 1 to 6). |
| 6 Recommend | Advice present | One banner per advice item, green for a friendly alternative, amber for a limitation. More than one is normal. |
| 7 Floating card | Overlay permission missing | Before starting the service, show the permission dialog (Allow opens the system "display over other apps" page, Cancel stays on Recommendation). On return, check the permission again before starting. |
| 7 Floating card | Notification permission denied (Android 13+) | The service still runs, but the foreground notification may be hidden. Do not block the card on it. |
| 7 Floating card | System kills the service | The card disappears with it. Starting again must be safe to call twice (no duplicate windows). |
| 7 Floating card | More than five rows | List scrolls inside the card; "+N more" line shows below. |
| 8 How-to | No screenshot URL | Dashed placeholder box with the step number. |
| 8 How-to | Brand guide download fails | Show the bundled generic three steps per brand. |
| 9 Feedback | Empty | Photo box shows a prompt, no chips, no AI card. Both bottom buttons remain. |
| 9 Feedback | Loading | Skeleton card in place of the AI card (not built yet in the scaffold). |
| 9 Feedback | Out of quota | Highlight the two buttons below; the ad button gives one extra feedback, the top-up opens Upgrade. |
| 9 Feedback | Network or server error | Retry banner; do not spend a free feedback on a failed call. |
| 9 Feedback | Photo has no EXIF | Show the photo with no chips and say the settings could not be read; the AI can still comment on the picture. |
| 10 Recipes | List is empty | Only the dashed empty slot is shown. |
| 10 Recipes | User has not unlocked | See open question on saved recipes. |
| 11 Upgrade | Google Play unavailable or price not loaded | Disable the buy button and show a short "try again" line; never show a made-up price. |
| 11 Upgrade | Already purchased | Replace the hero card with a green "unlocked" state; keep the top-up card. |
| All | Offline | Meter, Recommendation, Recipes and the card work fully offline. Only Feedback, the brand guide and Billing need a connection. |

## Localization, accessibility and platform notes

**Localization.** Bangla is the default, so its strings live in `res/values/strings.xml` and English in `res/values-en/`. The in-app toggle calls `AppCompatDelegate.setApplicationLocales`, which also appears in the system per-app language list through `locales_config.xml`. Numbers are shown in Bangla digits when the locale is Bangla (`localizedNumber`). Never build a sentence by joining strings; use format strings (`home_left_fmt`, `card_progress_fmt`). Bangla runs longer than English in several places, so every label must wrap or truncate with an ellipsis rather than overflow, and buttons grow in height before they shrink text.

**Accessibility.** Contrast figures are in the token table; amber on the dark ground is the only low-risk accent. Every tappable element is at least 48 dp. Icons that carry meaning have a content description (back, close, settings, minimise, add recipe); decorative icons pass `null`. The light gauge announces its level as text, not just colour. The welcome illustration has a spoken description. Do not rely on green versus amber alone: each is paired with words or an icon. Test with TalkBack in Bangla and with font scale at 130%.

**Permissions.** Camera is requested on screen 5, not at launch. Display over other apps (`SYSTEM_ALERT_WINDOW`) is requested only when the user taps Start card, with the in-app dialog first. Notifications (Android 13+) are optional. Photo feedback uses the system photo picker, so no storage permission is needed. Billing and Internet are normal permissions.

**Foreground service.** The overlay service uses the `specialUse` foreground service type with a declared subtype. Google Play asks for a justification for this type in the app content declarations; write it before the first review.

**Brand differences.** Xiaomi, Samsung and Realme camera apps expose manual controls differently, and some hide EV when ISO and shutter are manual. The card lists what to set but cannot read what the phone's own app is set to.

**Assets to supply.** Hind Siliguri TTFs (regular, medium, semibold, bold) in `res/font` (the project has a README where they go; they could not be downloaded in the build environment). Launcher icon and adaptive icon from the logo mark. Screenshots for the how-to guide for each brand. Play Store graphics are outside this spec.

**Open questions**

- Is the EV slider still usable in each brand's Pro mode? The engine bakes the EV bias into ISO and shutter and shows it for information only, so confirm on real Xiaomi, Samsung and Realme phones.
- Should free users be able to save recipes? The scaffold lets the first one through and opens Upgrade from the add button; the feature spec does not say.
- Does the Meter screen sit under the status bar (full-bleed preview) or below it? The design is full-bleed; check cut-outs and the back button position.
- What justification does Google Play accept for the `specialUse` foreground service?
- Which image loader for how-to screenshots and the feedback photo? Coil is suggested.
- What is the schema of the remote brand-guide JSON (brand, model range, steps, screenshot URLs, language)?
- What does the "watch an ad" button grant, and does the grant survive a restart?
- The Bangla copy is a first draft and needs a native reviewer before release.
