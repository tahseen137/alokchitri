package com.alokchitri.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Tokens from the design canvas. Dark only in v1: it sits next to a camera viewfinder without glare. */
object AlokColors {
    val Ground = Color(0xFF0E1014)       // screen background
    val Surface = Color(0xFF171A21)      // cards, nav bar, sheets
    val SurfaceRaised = Color(0xFF20242D) // icon tiles, inset banners
    val Outline = Color(0xFF2B303B)      // borders, dividers
    val OutlineStrong = Color(0xFF3A404D) // dashed placeholders, inactive controls on the camera mock

    val Text = Color(0xFFF3F1EC)
    val TextMuted = Color(0xFFA3A9B5)

    val Amber = Color(0xFFFFB547)        // light / primary action / values
    val AmberTint = Color(0xFF3A2D12)    // selected chip, nav indicator
    val OnAmber = Color(0xFF1A1200)

    val Green = Color(0xFF4FD6A5)        // done / supported / free quota
    val Viewfinder = Color(0xFF12151B)   // camera preview placeholder
    val ViewfinderDeep = Color(0xFF07080B)
}
