package com.example.tvlauncher.apps

import kotlin.math.max
import kotlin.math.min

/**
 * Обработка пикселей иконки (ARGB в Int), без Android: убрать белую подложку,
 * сохранить сглаженные края и осветлить тёмные детали, которые пропали бы на тёмной плитке.
 */
object IconPixels {

    private const val LIGHT_GRAY = 0xD4D4D8

    private fun a(c: Int) = c ushr 24
    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF
    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    private fun minChannel(c: Int) = min(r(c), min(g(c), b(c)))
    private fun maxChannel(c: Int) = max(r(c), max(g(c), b(c)))

    /** Почти белый: светлый, без оттенка, непрозрачный. */
    fun isNearWhite(c: Int): Boolean =
        a(c) > 160 && minChannel(c) >= 215 && maxChannel(c) - minChannel(c) < 30

    /** Фон составной иконки светлый: больше 80 % непрозрачных пикселей почти белые. */
    fun isMostlyWhite(px: IntArray): Boolean {
        val opaque = px.filter { a(it) > 200 }
        return opaque.isNotEmpty() && opaque.count(::isNearWhite) > opaque.size * 0.8
    }

    /**
     * Если не меньше 60 % рамки почти белые — делает прозрачным почти-белое, связанное с краями.
     * Белое внутри логотипа (окружённое цветом) остаётся. true — фон был убран.
     */
    fun removeWhiteBackground(px: IntArray, w: Int, h: Int): Boolean {
        val border = (0 until w).flatMap { listOf(it, (h - 1) * w + it) } +
            (0 until h).flatMap { listOf(it * w, it * w + w - 1) }
        if (border.count { isNearWhite(px[it]) } < border.size * 0.6) return false

        val removed = BooleanArray(px.size)
        val queue = ArrayDeque<Int>()
        for (i in border) {
            if (!removed[i] && isNearWhite(px[i])) {
                removed[i] = true
                queue.add(i)
            }
        }
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            val x = i % w
            val y = i / w
            for ((nx, ny) in listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)) {
                if (nx !in 0 until w || ny !in 0 until h) continue
                val j = ny * w + nx
                if (!removed[j] && isNearWhite(px[j])) {
                    removed[j] = true
                    queue.add(j)
                }
            }
        }
        for (i in px.indices) {
            when {
                removed[i] -> px[i] = 0
                touches(removed, i, w, h) -> px[i] = unblendWhite(px[i])
            }
        }
        return true
    }

    private fun touches(removed: BooleanArray, i: Int, w: Int, h: Int): Boolean {
        val x = i % w
        val y = i / w
        for (dy in -1..1) for (dx in -1..1) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until w && ny in 0 until h && removed[ny * w + nx]) return true
        }
        return false
    }

    /** Цвет, сглаженный в белый, → тот же цвет с прозрачностью: без светлой каймы на тёмном фоне. */
    fun unblendWhite(c: Int): Int {
        val alpha = (255 - minChannel(c)).coerceAtLeast(1)
        fun channel(v: Int) = (255 - (255 - v) * 255 / alpha).coerceIn(0, 255)
        return argb(alpha * a(c) / 255, channel(r(c)), channel(g(c)), channel(b(c)))
    }

    /** Почти-чёрное и тёмно-серое без цвета → светло-серое, прозрачность сохраняется. */
    fun lightenDark(px: IntArray) {
        for (i in px.indices) {
            val c = px[i]
            if (a(c) > 0 && maxChannel(c) < 80 && maxChannel(c) - minChannel(c) < 40) {
                px[i] = (c and 0xFF000000.toInt()) or LIGHT_GRAY
            }
        }
    }

    /** Границы видимого содержимого [left, top, right, bottom] включительно; null — пусто. */
    fun contentBounds(px: IntArray, w: Int, h: Int): IntArray? {
        var left = w
        var top = h
        var right = -1
        var bottom = -1
        for (y in 0 until h) for (x in 0 until w) {
            if (a(px[y * w + x]) > 24) {
                left = min(left, x)
                right = max(right, x)
                top = min(top, y)
                bottom = max(bottom, y)
            }
        }
        return if (right < 0) null else intArrayOf(left, top, right, bottom)
    }
}
