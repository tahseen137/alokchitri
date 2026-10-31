package com.alokchitri.app.data

import com.alokchitri.app.AppConfig
import com.alokchitri.app.ui.screens.FeedbackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Calls server/api/photo-feedback.ts. Throws on any network, HTTP or format problem. */
object FeedbackApi {
    suspend fun request(photo: PreparedPhoto, language: String, phone: String?): FeedbackResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("image", photo.base64Jpeg)
                put("language", language)
                phone?.let { put("phone", it.take(80)) }
                put("exif", JSONObject().apply {
                    photo.exif.iso?.let { put("iso", it) }
                    photo.exif.shutter?.let { put("shutter", it) }
                    photo.exif.aperture?.let { put("aperture", it) }
                    photo.exif.focalLength?.let { put("focalLength", it) }
                    photo.exif.flash?.let { put("flash", it) }
                })
            }
            val conn = URL(AppConfig.FEEDBACK_URL).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 10_000
                conn.readTimeout = 60_000 // the model looks at the photo; give it time on slow mobile data
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                FeedbackResult(json.getString("whatHappened"), json.getString("nextTime"))
            } finally {
                conn.disconnect()
            }
        }
}
