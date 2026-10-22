package com.example.tvlauncher.grid

import org.junit.Assert.assertEquals
import org.junit.Test

class GridCodecTest {

    @Test
    fun roundTripKeepsEverything() {
        val s = GridState(
            order = listOf("com.a", "@settings", "com.b"),
            known = setOf("com.a", "com.b", "com.c", "@settings"),
            fresh = setOf("com.b"),
            initialized = true,
        )

        assertEquals(s, GridCodec.decode(GridCodec.encode(s)))
    }

    @Test
    fun roundTripOfEmptyListsIsEmpty() {
        val s = GridState.EMPTY.copy(initialized = true)

        val decoded = GridCodec.decode(GridCodec.encode(s))

        assertEquals(emptyList<String>(), decoded.order)
        assertEquals(emptySet<String>(), decoded.known)
        assertEquals(emptySet<String>(), decoded.fresh)
        assertEquals(true, decoded.initialized)
    }

    @Test
    fun decodeOfEmptyMapIsEmptyState() {
        assertEquals(GridState.EMPTY, GridCodec.decode(emptyMap()))
    }
}
