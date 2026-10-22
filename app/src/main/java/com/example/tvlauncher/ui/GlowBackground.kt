package com.example.tvlauncher.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Фон экрана: заливка и эллиптическое свечение сверху по центру, как в наброске —
 * radial-gradient(70% × 30% ширины, центр на 8 % высоты выше экрана, затухание к 70 %).
 * Рисуется напрямую, без растрового кэша во весь экран, который держал бы VectorDrawable (~8 МБ).
 */
class GlowBackground(private val baseColor: Int, private val glowColor: Int) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cx = 0f
    private var cy = 0f
    private var rx = 1f
    private var ry = 1f

    override fun onBoundsChange(bounds: Rect) {
        cx = bounds.exactCenterX()
        cy = bounds.top - 0.08f * bounds.height()
        rx = 0.70f * bounds.width()
        ry = 0.30f * bounds.width()
        paint.shader = RadialGradient(
            cx, cy, rx,
            intArrayOf(glowColor, glowColor and 0x00FFFFFF),
            floatArrayOf(0f, 0.7f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(baseColor)
        canvas.save()
        canvas.scale(1f, ry / rx, cx, cy)
        canvas.drawCircle(cx, cy, rx * 0.7f, paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
