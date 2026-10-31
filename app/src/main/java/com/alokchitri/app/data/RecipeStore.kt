package com.alokchitri.app.data

import android.content.Context
import com.alokchitri.engine.CaptureFormat
import com.alokchitri.engine.FocusMode
import com.alokchitri.engine.LensType
import com.alokchitri.engine.LightLevel
import com.alokchitri.engine.Recommendation
import com.alokchitri.engine.ShotGoal
import com.alokchitri.engine.Stability
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A recommendation the user kept, with the name they gave it. Newest first in the store. */
data class SavedRecipe(
    val id: Long,
    val name: String,
    val goal: ShotGoal,
    val recommendation: Recommendation,
)

/**
 * Saved recipes as one small JSON file in app storage: a handful of rows, read once at start, so a database
 * would be overkill. Works offline, as the spec requires. Not thread-safe: call from one thread (main).
 */
class RecipeStore(context: Context) {
    private val file = File(context.filesDir, "recipes.json")

    fun load(): List<SavedRecipe> = runCatching {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        (0 until array.length()).mapNotNull { i -> runCatching { fromJson(array.getJSONObject(i)) }.getOrNull() }
    }.getOrDefault(emptyList())

    fun save(recipes: List<SavedRecipe>) {
        val array = JSONArray()
        recipes.forEach { array.put(toJson(it)) }
        // Write then rename, so a crash mid-write never leaves a half file behind.
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(file)
    }

    private fun toJson(r: SavedRecipe) = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("goal", r.goal.name)
        val rec = r.recommendation
        put("iso", rec.iso)
        put("shutter", rec.shutterSeconds)
        put("wb", rec.whiteBalanceK)
        put("focus", rec.focus.name)
        put("ev", rec.evBias)
        put("lens", rec.lens.name)
        put("format", rec.format.name)
        put("stability", rec.stability.name)
        put("light", rec.lightLevel.name)
    }

    private fun fromJson(o: JSONObject) = SavedRecipe(
        id = o.getLong("id"),
        name = o.getString("name"),
        goal = ShotGoal.valueOf(o.getString("goal")),
        recommendation = Recommendation(
            iso = o.getInt("iso"),
            shutterSeconds = o.getDouble("shutter"),
            whiteBalanceK = o.getInt("wb"),
            focus = FocusMode.valueOf(o.getString("focus")),
            evBias = o.getDouble("ev"),
            lens = LensType.valueOf(o.getString("lens")),
            format = CaptureFormat.valueOf(o.getString("format")),
            stability = Stability.valueOf(o.getString("stability")),
            // Advice was about the light at the time; a recipe is reused later, so it is not kept.
            advice = emptySet(),
            lightLevel = LightLevel.valueOf(o.getString("light")),
        ),
    )
}
