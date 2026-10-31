package com.alokchitri.app.data

import android.content.Context
import com.alokchitri.app.AppConfig
import java.time.LocalDate

/**
 * How many photo feedbacks the user can still ask for. Daily allowance (resets at local midnight) plus
 * one-off extras for the day (rewarded ads), then top-up credits that never expire.
 * Counted on the device; a feedback is only spent after the server answered.
 */
class FeedbackQuota(context: Context, private val today: () -> LocalDate = LocalDate::now) {
    private val sp = context.getSharedPreferences("alok_quota", Context.MODE_PRIVATE)
    private val prefs = Prefs(context)

    /** 3 a day free, about 20 after the lifetime unlock. */
    val dailyLimit: Int
        get() = if (prefs.unlocked) AppConfig.UNLOCKED_FEEDBACKS_PER_DAY else AppConfig.FREE_FEEDBACKS_PER_DAY

    val topUpCredits: Int get() = sp.getInt(KEY_TOPUP, 0)

    /** Left from today's allowance and extras, not counting top-up credits. */
    val leftToday: Int get() {
        rollOver()
        return (dailyLimit + sp.getInt(KEY_BONUS, 0) - sp.getInt(KEY_USED, 0)).coerceAtLeast(0)
    }

    val available: Int get() = leftToday + topUpCredits

    fun consume() {
        if (leftToday > 0) sp.edit().putInt(KEY_USED, sp.getInt(KEY_USED, 0) + 1).apply()
        else if (topUpCredits > 0) sp.edit().putInt(KEY_TOPUP, topUpCredits - 1).apply()
    }

    /** One extra for today, e.g. after a rewarded ad. */
    fun addBonus() {
        rollOver()
        sp.edit().putInt(KEY_BONUS, sp.getInt(KEY_BONUS, 0) + 1).apply()
    }

    fun addTopUp(credits: Int) = sp.edit().putInt(KEY_TOPUP, topUpCredits + credits).apply()

    private fun rollOver() {
        val day = today().toString()
        if (sp.getString(KEY_DAY, null) != day) {
            sp.edit().putString(KEY_DAY, day).putInt(KEY_USED, 0).putInt(KEY_BONUS, 0).apply()
        }
    }

    private companion object {
        const val KEY_DAY = "day"
        const val KEY_USED = "used"
        const val KEY_BONUS = "bonus"
        const val KEY_TOPUP = "topup"
    }
}
