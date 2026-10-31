package com.alokchitri.app.data

import android.content.Context

/** Small on-device settings: first-run state and the phone the user picked when the probe got the name wrong. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("alok_prefs", Context.MODE_PRIVATE)

    var setupDone: Boolean
        get() = sp.getBoolean(KEY_SETUP_DONE, false)
        set(value) = sp.edit().putBoolean(KEY_SETUP_DONE, value).apply()

    /** "Brand Model" typed or picked on Setup; null means use what Build reports. */
    var phoneNameOverride: String?
        get() = sp.getString(KEY_PHONE_NAME, null)
        set(value) = sp.edit().putString(KEY_PHONE_NAME, value).apply()

    /** Last known state of the lifetime unlock, so it works offline; Play re-confirms on every launch. */
    var unlocked: Boolean
        get() = sp.getBoolean(KEY_UNLOCKED, false)
        set(value) = sp.edit().putBoolean(KEY_UNLOCKED, value).apply()

    /** Debug builds only: treat the camera as manual-capable, so the full flow can be tried on an emulator. */
    var simulateProMode: Boolean
        get() = sp.getBoolean(KEY_SIMULATE_PRO, false)
        set(value) = sp.edit().putBoolean(KEY_SIMULATE_PRO, value).apply()

    private companion object {
        const val KEY_SETUP_DONE = "setup_done"
        const val KEY_PHONE_NAME = "phone_name"
        const val KEY_UNLOCKED = "unlocked"
        const val KEY_SIMULATE_PRO = "debug_simulate_pro"
    }
}
