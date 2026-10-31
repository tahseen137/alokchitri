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

    const val BRAND_GUIDE_URL = "https://raw.githubusercontent.com/tahseen137/alokchitri/main/guides/brand-guides.json"
}
