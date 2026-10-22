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

    @Test
    fun rateHasTwoDecimalsWithComma() {
        assertEquals("81,13", Format.rate(81.1255))
        assertEquals("91,14", Format.rate(91.1364))
    }

    @Test
    fun deltaShowsArrow() {
        assertEquals(Delta("▼ 0,85", Trend.DOWN), Format.delta(81.1255 - 81.9739))
        assertEquals(Delta("▲ 0,34", Trend.UP), Format.delta(0.34))
    }

    @Test
    fun tinyDeltaIsFlat() {
        assertEquals(Delta("0,00", Trend.FLAT), Format.delta(0.004))
        assertEquals(Delta("0,00", Trend.FLAT), Format.delta(-0.0049))
        assertEquals(Delta("0,00", Trend.FLAT), Format.delta(0.0))
    }
}
