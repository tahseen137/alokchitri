package com.alokchitri.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alokchitri.app.R

/**
 * Hind Siliguri covers Bangla and Latin. Download the four TTFs from Google Fonts (OFL licence) and put them in
 * app/src/main/res/font/ with these exact names:
 *   hind_siliguri_regular.ttf, hind_siliguri_medium.ttf, hind_siliguri_semibold.ttf, hind_siliguri_bold.ttf
 */
val HindSiliguri = FontFamily(
    Font(R.font.hind_siliguri_regular, FontWeight.Normal),
    Font(R.font.hind_siliguri_medium, FontWeight.Medium),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight) =
    TextStyle(fontFamily = HindSiliguri, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/** Mapping from the spec's type scale to Material roles. Bangla needs generous line height: do not tighten it. */
val AlokTypography = Typography(
    displayMedium = style(32, 42, FontWeight.Bold),    // Display: welcome, upgrade
    headlineMedium = style(30, 39, FontWeight.Bold),   // Headline: screen titles
    headlineSmall = style(26, 34, FontWeight.Bold),    // tab-screen titles
    titleLarge = style(20, 28, FontWeight.Bold),       // card titles
    titleMedium = style(19, 26, FontWeight.Bold),      // setting values
    bodyLarge = style(17, 26, FontWeight.Normal),
    bodyMedium = style(16, 25, FontWeight.Normal),
    bodySmall = style(14, 21, FontWeight.Normal),
    labelLarge = style(18, 24, FontWeight.Bold),       // button text
    labelMedium = style(15, 20, FontWeight.SemiBold),  // chips
    labelSmall = style(13, 18, FontWeight.Medium),     // nav labels, captions (never smaller than 13 sp)
)
