package com.example.tvlauncher.apps

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPixelsTest {

    private val white = 0xFFFFFFFF.toInt()
    private val red = 0xFFFF0000.toInt()
    private val clear = 0

    @Test
    fun nearWhiteIsLightAndColorless() {
        assertTrue(IconPixels.isNearWhite(white))
        assertTrue(IconPixels.isNearWhite(0xFFDCDCDC.toInt()))
        assertFalse(IconPixels.isNearWhite(0xFFD0D0D0.toInt()))
        assertFalse(IconPixels.isNearWhite(red))
        assertFalse(IconPixels.isNearWhite(0x00FFFFFF))
    }

    // 5×5: белый фон, красный квадрат 3×3 в центре, внутри него белая точка (как треугольник в логотипе YouTube).
    private fun logoOnWhite(): IntArray = IntArray(25) { i ->
        val x = i % 5
        val y = i / 5
        when {
            x == 2 && y == 2 -> white
            x in 1..3 && y in 1..3 -> red
            else -> white
        }
    }

    @Test
    fun whiteBackgroundFromEdgesBecomesTransparent() {
        val px = logoOnWhite()

        assertTrue(IconPixels.removeWhiteBackground(px, 5, 5))

        assertEquals(clear, px[0])
        assertEquals(clear, px[24])
        assertEquals(red, px[6])
        assertEquals(white, px[12])
    }

    @Test
    fun coloredBackgroundIsKept() {
        val px = IntArray(9) { red }

        assertFalse(IconPixels.removeWhiteBackground(px, 3, 3))
        assertArrayEquals(IntArray(9) { red }, px)
    }

    @Test
    fun edgeBlendedIntoWhiteBecomesSemiTransparent() {
        assertEquals(0x7FFF0000, IconPixels.unblendWhite(0xFFFF8080.toInt()))
    }

    @Test
    fun darkGrayDetailsAreLightenedKeepingAlpha() {
        val px = intArrayOf(0xFF000000.toInt(), 0x80101010.toInt(), 0xFF8B0000.toInt(), 0xFF404040.toInt())

        IconPixels.lightenDark(px)

        assertArrayEquals(
            intArrayOf(0xFFD4D4D8.toInt(), 0x80D4D4D8.toInt(), 0xFF8B0000.toInt(), 0xFFD4D4D8.toInt()),
            px,
        )
    }

    @Test
    fun mostlyWhiteNeedsOver80PercentOfOpaquePixels() {
        assertTrue(IconPixels.isMostlyWhite(IntArray(10) { if (it == 0) red else white }))
        assertFalse(IconPixels.isMostlyWhite(IntArray(10) { if (it < 5) red else white }))
        assertFalse(IconPixels.isMostlyWhite(IntArray(10) { clear }))
    }

    @Test
    fun contentBoundsCoverVisiblePixels() {
        val px = IntArray(16) { clear }.also { it[1 * 4 + 1] = red; it[3 * 4 + 2] = red }

        assertArrayEquals(intArrayOf(1, 1, 2, 3), IconPixels.contentBounds(px, 4, 4))
        assertNull(IconPixels.contentBounds(IntArray(16) { clear }, 4, 4))
    }
}
