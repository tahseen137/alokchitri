package com.alokchitri.app

/** Everything that points outside the app, in one place, so release IDs are swapped in a single file. */
object AppConfig {
    /** Brand how-to steps; see guides/README.md. Served from the public repo so edits need no app update. */
    /**
     * The Vercel function in server/ (see server/README.md). Replace the host with the project's real
     * domain after the first deploy.
     */
    const val FEEDBACK_URL = "https://alokchitri-server.vercel.app/api/photo-feedback"

    /** Free photo feedbacks per day before the lifetime unlock. */
    const val FREE_FEEDBACKS_PER_DAY = 3

    /** "About 20 feedbacks a day" after the lifetime unlock (Upgrade screen). */
    const val UNLOCKED_FEEDBACKS_PER_DAY = 20

    /**
     * Google Play product IDs. Create both as one-time products in Play Console with exactly these IDs,
     * or change them here: the lifetime unlock (non-consumable) and the feedback top-up (consumable).
     */
    const val PRODUCT_UNLOCK = "lifetime_unlock"
    const val PRODUCT_TOPUP = "feedback_topup_30"

    /** Feedbacks one top-up purchase adds. */
    const val TOPUP_CREDITS = 30

    const val BRAND_GUIDE_URL = "https://raw.githubusercontent.com/tahseen137/alokchitri/main/guides/brand-guides.json"
}
