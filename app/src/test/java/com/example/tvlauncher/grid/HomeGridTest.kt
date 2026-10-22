package com.example.tvlauncher.grid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGridTest {

    private val youtube = "com.google.android.youtube.tv"
    private val kinopoisk = "ru.kinopoisk.tv"
    private val vlc = "org.videolan.vlc"
    private val rutube = "rtb.mobile.android"
    private val settings = HomeGrid.SETTINGS_KEY

    private val installed = setOf(youtube, kinopoisk, vlc, rutube, settings)

    private fun firstRun() = HomeGrid.reconcile(GridState.EMPTY, installed)

    private fun state(vararg keys: String) =
        GridState(order = keys.toList(), known = keys.toSet(), fresh = emptySet(), initialized = true)

    @Test
    fun firstRunUsesDefaultOrderFilteredByInstalled() {
        val s = firstRun()

        assertEquals(listOf(youtube, kinopoisk, vlc, settings), s.order)
        assertEquals(installed, s.known)
        assertTrue(s.fresh.isEmpty())
        assertTrue(s.initialized)
    }

    @Test
    fun appInstalledLaterIsAppendedAndFresh() {
        val s = HomeGrid.reconcile(firstRun(), installed + "com.new.app")

        assertEquals(listOf(youtube, kinopoisk, vlc, settings, "com.new.app"), s.order)
        assertEquals(setOf("com.new.app"), s.fresh)
        assertFalse(rutube in s.order)
    }

    @Test
    fun removedFromHomeDoesNotComeBack() {
        val removed = HomeGrid.remove(firstRun(), youtube)

        val s = HomeGrid.reconcile(removed, installed)

        assertEquals(listOf(kinopoisk, vlc, settings), s.order)
    }

    @Test
    fun uninstalledAppDisappearsAndReinstallIsFresh() {
        val gone = HomeGrid.reconcile(firstRun(), installed - vlc)
        assertEquals(listOf(youtube, kinopoisk, settings), gone.order)
        assertFalse(vlc in gone.known)

        val back = HomeGrid.reconcile(gone, installed)

        assertEquals(listOf(youtube, kinopoisk, settings, vlc), back.order)
        assertEquals(setOf(vlc), back.fresh)
    }

    @Test
    fun moveRelocatesKey() {
        val s = HomeGrid.move(state("a", "b", "c", "d"), 0, 2)

        assertEquals(listOf("b", "c", "a", "d"), s.order)
    }

    @Test
    fun moveClampsTargetIntoRange() {
        assertEquals(listOf("b", "c", "a"), HomeGrid.move(state("a", "b", "c"), 0, 99).order)
        assertEquals(listOf("c", "a", "b"), HomeGrid.move(state("a", "b", "c"), 2, -5).order)
    }

    @Test
    fun addAppendsOnceAndRemoveDropsFresh() {
        val added = HomeGrid.add(HomeGrid.add(state("a", "b"), "c"), "c")
        assertEquals(listOf("a", "b", "c"), added.order)

        val withFresh = state("a", "b").copy(fresh = setOf("b"))
        val removed = HomeGrid.remove(withFresh, "b")

        assertEquals(listOf("a"), removed.order)
        assertTrue(removed.fresh.isEmpty())
    }

    @Test
    fun clearFreshRemovesBadgeOnly() {
        val s = HomeGrid.clearFresh(state("a", "b").copy(fresh = setOf("a", "b")), "a")

        assertEquals(setOf("b"), s.fresh)
        assertEquals(listOf("a", "b"), s.order)
    }

    // Сетка из 10 приложений по 6 в ряд: индексы 0..5 — верхний ряд, 6..9 — нижний (неполный).
    @Test
    fun verticalMoveStopsAtGridEdge() {
        assertEquals(null, HomeGrid.moveTarget(from = 3, step = -6, size = 10, columns = 6))
        assertEquals(null, HomeGrid.moveTarget(from = 7, step = 6, size = 10, columns = 6))
    }

    @Test
    fun verticalMoveWithinGrid() {
        assertEquals(8, HomeGrid.moveTarget(from = 2, step = 6, size = 10, columns = 6))
        assertEquals(2, HomeGrid.moveTarget(from = 8, step = -6, size = 10, columns = 6))
    }

    @Test
    fun downIntoShorterLastRowGoesToLastApp() {
        assertEquals(9, HomeGrid.moveTarget(from = 5, step = 6, size = 10, columns = 6))
    }

    @Test
    fun horizontalMoveStopsAtEnds() {
        assertEquals(null, HomeGrid.moveTarget(from = 0, step = -1, size = 10, columns = 6))
        assertEquals(null, HomeGrid.moveTarget(from = 9, step = 1, size = 10, columns = 6))
        assertEquals(5, HomeGrid.moveTarget(from = 6, step = -1, size = 10, columns = 6))
    }
}
