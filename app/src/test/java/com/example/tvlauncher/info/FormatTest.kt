package com.example.tvlauncher.info

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FormatTest {

    @Test
    fun timeIsTwoDigit24h() {
        assertEquals("09:05", Format.time(LocalTime.of(9, 5)))
        assertEquals("18:41", Format.time(LocalTime.of(18, 41)))
    }

    @Test
    fun dateIsCapitalizedRussianGenitive() {
        assertEquals("Четверг, 22 октября", Format.date(LocalDate.of(2026, 10, 22)))
        assertEquals("Воскресенье, 1 марта", Format.date(LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun temperatureHasSign() {
        assertEquals("+8°", Format.temperature(8.3))
        assertEquals("−4°", Format.temperature(-3.6))
        assertEquals("+1°", Format.temperature(0.5))
    }

    @Test
    fun temperatureNearZeroHasNoSign() {
        assertEquals("0°", Format.temperature(-0.4))
        assertEquals("0°", Format.temperature(0.4))
        assertEquals("0°", Format.temperature(-0.5))
    }
}
