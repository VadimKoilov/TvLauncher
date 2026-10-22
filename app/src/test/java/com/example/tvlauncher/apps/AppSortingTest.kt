package com.example.tvlauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSortingTest {

    @Test
    fun sortsCyrillicIgnoringCase() {
        val sorted = AppSorting.byLabel(listOf("Настройки", "браузер", "Кинопоиск")) { it }

        assertEquals(listOf("браузер", "Кинопоиск", "Настройки"), sorted)
    }

    @Test
    fun sortsLatinIgnoringCase() {
        val sorted = AppSorting.byLabel(listOf("youtube", "VLC", "Okko")) { it }

        assertEquals(listOf("Okko", "VLC", "youtube"), sorted)
    }
}
