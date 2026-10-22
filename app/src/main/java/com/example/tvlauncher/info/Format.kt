package com.example.tvlauncher.info

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Форматы текста на главном экране. */
object Format {

    private val RU = Locale("ru")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", RU)
    private val DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", RU)

    /** «18:41» */
    fun time(t: LocalTime): String = t.format(TIME)

    /** «Четверг, 22 октября» */
    fun date(d: LocalDate): String = d.format(DATE).replaceFirstChar { it.titlecase(RU) }

    /** «+8°», «0°», «−4°» (типографский минус). Около нуля знака нет. */
    fun temperature(c: Double): String {
        val t = c.roundToInt()
        return when {
            t > 0 -> "+$t°"
            t < 0 -> "−${abs(t)}°"
            else -> "0°"
        }
    }
}
