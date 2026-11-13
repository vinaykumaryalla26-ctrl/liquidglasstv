package com.liquidglass.tv.model

import android.content.Intent
import android.graphics.drawable.Drawable

/**
 * Lightweight data model representing an installed Android TV application.
 */
data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Drawable,
    val launchIntent: Intent,
    val isDocked: Boolean = false
)
