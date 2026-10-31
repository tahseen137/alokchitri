package com.alokchitri.app.data

import android.content.Context
import com.alokchitri.app.AppConfig
import com.alokchitri.app.ui.screens.HowToStep
import com.alokchitri.app.ui.screens.PhoneBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the brand how-to guide (screen 8) and keeps the last good copy for offline use.
 * Returns null when there is neither a download nor a cached copy; the screen then shows the bundled steps.
 */
class BrandGuideRepository(context: Context) {
    private val cache = File(context.filesDir, "brand-guides.json")

    /** Steps per brand in [language] ("bn" or "en"); the network is tried first, then the cache. */
    suspend fun load(language: String): Map<PhoneBrand, List<HowToStep>>? = withContext(Dispatchers.IO) {
        val fresh = runCatching { download() }.getOrNull()?.takeIf { parseBrandGuide(it, language) != null }
        if (fresh != null) runCatching { cache.writeText(fresh) }
        val text = fresh ?: runCatching { cache.readText() }.getOrNull() ?: return@withContext null
        parseBrandGuide(text, language)
    }

    private fun download(): String {
        val conn = URL(AppConfig.BRAND_GUIDE_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 5_000
        conn.readTimeout = 5_000
        try {
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

}

private const val SUPPORTED_VERSION = 1

/** Parses guides/brand-guides.json; null if the file is unusable, so the caller falls back. */
internal fun parseBrandGuide(text: String, language: String): Map<PhoneBrand, List<HowToStep>>? = runCatching {
    val root = JSONObject(text)
    if (root.optInt("version") != SUPPORTED_VERSION) return null
    val brands = root.getJSONObject("brands")
    PhoneBrand.entries.associateWith { brand ->
        val steps = brands.optJSONArray(brand.name) ?: return@associateWith emptyList()
        (0 until steps.length()).map { i ->
            val step = steps.getJSONObject(i)
            HowToStep(
                text = step.optString(language).ifEmpty { step.getString("en") },
                screenshotUrl = if (step.isNull("screenshot")) null else step.optString("screenshot").ifEmpty { null },
            )
        }
    }.takeIf { map -> map.values.any { it.isNotEmpty() } }
}.getOrNull()
