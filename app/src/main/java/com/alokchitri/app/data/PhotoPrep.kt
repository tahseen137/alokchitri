package com.alokchitri.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import com.alokchitri.engine.formatShutter
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.max

/** Settings the camera saved in the photo; any of them may be missing (screenshots, edited or shared photos). */
data class PhotoExif(
    val iso: Int? = null,
    val shutter: String? = null,
    val aperture: Double? = null,
    val focalLength: Double? = null,
    val flash: Boolean? = null,
) {
    /** Chips for the Feedback screen, in the order ISO, shutter, aperture. */
    val chips: List<String> get() = listOfNotNull(iso?.let { "ISO $it" }, shutter, aperture?.let { "f/%.1f".format(Locale.ROOT, it) })
}

data class PreparedPhoto(val uri: Uri, val base64Jpeg: String, val exif: PhotoExif)

/** Reads the picked photo's EXIF and shrinks it for upload: 1280 px long side, JPEG 80, upright. */
object PhotoPrep {
    private const val MAX_SIDE = 1280

    fun prepare(context: Context, uri: Uri): PreparedPhoto {
        val resolver = context.contentResolver
        val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "not an image" }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("could not decode")

        val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            exif?.rotationDegrees?.takeIf { it != 0 }?.let { postRotate(it.toFloat()) }
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val bytes = ByteArrayOutputStream().also { upright.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()

        return PreparedPhoto(uri, Base64.encodeToString(bytes, Base64.NO_WRAP), readExif(exif))
    }

    private fun readExif(exif: ExifInterface?): PhotoExif {
        if (exif == null) return PhotoExif()
        val iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0).takeIf { it > 0 }
        val exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0).takeIf { it > 0 }
        val aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0).takeIf { it > 0 }
        val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0 }
        val flash = exif.getAttribute(ExifInterface.TAG_FLASH)?.toIntOrNull()?.let { it and 1 == 1 }
        return PhotoExif(iso, exposure?.let { formatShutter(it) }, aperture, focal, flash)
    }
}
