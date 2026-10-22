package com.example.tvlauncher.info

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Форматы текста на главном экране. */
object Format {

    private val RU = Locale("ru")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", RU)
    private val DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", RU)

    /** «18:41» */
    fun time(t: LocalTime): String = t.format(TIME)

    /** «Четверг, 22 октября» */
    fun date(d: LocalDate): String = d.format(DATE).replaceFirstChar { it.titlecase(RU) }
}
