package com.example.tvlauncher.ui

import android.view.View

/** Выбор пультом: плитка плавно увеличивается и светится акцентным цветом (тень окрашена в акцент). */
object FocusAnim {

    private const val SCALE = 1.1f
    private const val DURATION_MS = 150L
    private const val GLOW_DP = 14f

    fun attach(view: View, glowColor: Int) {
        view.outlineSpotShadowColor = glowColor
        view.outlineAmbientShadowColor = glowColor
        val glow = GLOW_DP * view.resources.displayMetrics.density
        view.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) SCALE else 1f
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .translationZ(if (hasFocus) glow else 0f)
                .setDuration(DURATION_MS)
                .start()
        }
    }
}
