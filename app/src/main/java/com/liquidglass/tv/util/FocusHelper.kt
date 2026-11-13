package com.liquidglass.tv.util

import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * FocusHelper provides hardware-accelerated, lightweight animations
 * specifically tuned for low-memory (1 GB RAM) Android TV devices.
 */
object FocusHelper {

    private val interpolator = DecelerateInterpolator()

    /**
     * Applies smooth D-pad focus scaling and elevation without triggering layout passes.
     */
    fun applyFocusScale(
        view: View,
        hasFocus: Boolean,
        scale: Float = 1.08f,
        elevationDp: Float = 12f,
        durationMs: Long = 180L
    ) {
        val targetScale = if (hasFocus) scale else 1.0f
        val targetElevation = if (hasFocus) {
            elevationDp * view.resources.displayMetrics.density
        } else {
            0f
        }

        if (hasFocus) {
            // Bring focused view above neighbor cards to prevent clipping
            view.elevation = targetElevation
            view.translationZ = targetElevation
        } else {
            view.elevation = 0f
            view.translationZ = 0f
        }

        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(durationMs)
            .setInterpolator(interpolator)
            .start()
    }
}
