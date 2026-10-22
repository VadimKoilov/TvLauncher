package com.example.tvlauncher.info

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Trend { UP, DOWN, FLAT }

/** Изменение курса за день: текст «▲ 0,34» и направление (для цвета). */
data class Delta(val text: String, val trend: Trend)

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

    /** «81,13» — два знака, десятичная запятая. */
    fun rate(v: Double): String = twoDecimals(v)

    /** Меньше полукопейки — «0,00» без стрелки. */
    fun delta(d: Double): Delta {
        if (abs(d) < FLAT_LIMIT) return Delta("0,00", Trend.FLAT)
        val value = twoDecimals(abs(d))
        return if (d > 0) Delta("▲ $value", Trend.UP) else Delta("▼ $value", Trend.DOWN)
    }

    private const val FLAT_LIMIT = 0.005

    // Через десятичную запись числа, чтобы 81.1255 округлялось до 81,13, а не по двоичному хвосту.
    private fun twoDecimals(v: Double): String =
        BigDecimal(v.toString()).setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
}
