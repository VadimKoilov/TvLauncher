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
}
