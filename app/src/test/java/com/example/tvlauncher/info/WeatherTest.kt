package com.example.tvlauncher.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherTest {

    // Ответ Open-Meteo для Кудрово, полученный 22.10.2026.
    private val sample = """{"latitude":59.897118,"longitude":30.504257,"generationtime_ms":52.15585231781006,""" +
        """"utc_offset_seconds":10800,"timezone":"Europe/Moscow","timezone_abbreviation":"GMT+3","elevation":9.0,""" +
        """"current_units":{"time":"iso8601","interval":"seconds","temperature_2m":"°C",""" +
        """"apparent_temperature":"°C","weather_code":"wmo code","is_day":""},""" +
        """"current":{"time":"2026-10-22T19:00","interval":900,"temperature_2m":8.3,""" +
        """"apparent_temperature":5.3,"weather_code":1,"is_day":0}}"""

    @Test
    fun parsesOpenMeteoCurrent() {
        assertEquals(Weather(tempC = 8.3, code = 1, isDay = false), WeatherParser.parse(sample))
    }

    @Test
    fun parseRejectsGarbage() {
        assertNull(WeatherParser.parse("{}"))
        assertNull(WeatherParser.parse("<html>Bad gateway</html>"))
        assertNull(WeatherParser.parse(""))
    }

    @Test
    fun codesMapToRussian() {
        assertEquals("пасмурно", WeatherCodes.describe(3))
        assertEquals("малооблачно", WeatherCodes.describe(1))
        assertEquals("", WeatherCodes.describe(1234))
    }

    @Test
    fun codesMapToIcons() {
        assertEquals(WeatherIcon.CLEAR_NIGHT, WeatherCodes.icon(0, isDay = false))
        assertEquals(WeatherIcon.PARTLY_DAY, WeatherCodes.icon(2, isDay = true))
        assertEquals(WeatherIcon.RAIN, WeatherCodes.icon(63, isDay = true))
        assertEquals(WeatherIcon.SNOW, WeatherCodes.icon(73, isDay = true))
        assertEquals(WeatherIcon.THUNDER, WeatherCodes.icon(96, isDay = true))
    }
}
