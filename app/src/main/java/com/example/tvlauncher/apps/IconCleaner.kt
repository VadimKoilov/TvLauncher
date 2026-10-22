package com.example.tvlauncher.apps

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.min

/**
 * Иконка для тёмной плитки: у иконок с белой подложкой остаётся только логотип, увеличенный
 * до размера иконки. Остальные иконки не меняются. Пиксели обрабатывает [IconPixels].
 */
object IconCleaner {

    fun clean(icon: Drawable, sizePx: Int, resources: Resources): Drawable {
        val bitmap = when {
            icon is AdaptiveIconDrawable && IconPixels.isMostlyWhite(pixels(render(icon.background, sizePx))) ->
                logoOnly(renderForeground(icon, sizePx), removeBackground = false)
            else -> render(icon, sizePx).let { logoOnly(it, removeBackground = true) ?: it }
        }
        return BitmapDrawable(resources, bitmap)
    }

    /** null — фон не белый, иконку не трогаем. */
    private fun logoOnly(bitmap: Bitmap, removeBackground: Boolean): Bitmap? {
        val w = bitmap.width
        val h = bitmap.height
        val px = pixels(bitmap)
        if (removeBackground && !IconPixels.removeWhiteBackground(px, w, h)) return null
        IconPixels.lightenDark(px)
        bitmap.setPixels(px, 0, w, 0, 0, w, h)
        val (left, top, right, bottom) = IconPixels.contentBounds(px, w, h) ?: return bitmap
        return fit(bitmap, Rect(left, top, right + 1, bottom + 1))
    }

    private fun fit(bitmap: Bitmap, content: Rect): Bitmap {
        val size = bitmap.width
        val scale = min(size.toFloat() / content.width(), size.toFloat() / content.height())
        val dw = content.width() * scale
        val dh = content.height() * scale
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val dst = RectF((size - dw) / 2, (size - dh) / 2, (size + dw) / 2, (size + dh) / 2)
        Canvas(out).drawBitmap(bitmap, content, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun render(d: Drawable?, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        d?.let {
            it.setBounds(0, 0, size, size)
            it.draw(Canvas(bitmap))
        }
        return bitmap
    }

    /** Передний слой рассчитан на холст 108dp с безопасной зоной 72dp — рисуем его увеличенным в 1.5 раза. */
    private fun renderForeground(icon: AdaptiveIconDrawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val inset = size / 4
        icon.foreground?.let {
            it.setBounds(-inset, -inset, size + inset, size + inset)
            it.draw(Canvas(bitmap))
        }
        return bitmap
    }

    private fun pixels(bitmap: Bitmap): IntArray =
        IntArray(bitmap.width * bitmap.height).also {
            bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
}
