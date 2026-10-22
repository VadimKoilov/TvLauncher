package com.example.tvlauncher.info

import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CbrDaily(val date: LocalDate, val usd: Double, val eur: Double)

/** Последний опубликованный курс и предыдущий — для стрелки «за день». */
data class Rates(val current: CbrDaily, val previous: CbrDaily)

object Cbr {

    const val LATEST_URL = "https://www.cbr.ru/scripts/XML_daily.asp"

    private val REQUEST_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun urlFor(date: LocalDate): String = "$LATEST_URL?date_req=${date.format(REQUEST_DATE)}"

    /** На этот день ЦБ отдаёт курс, действовавший до последнего опубликованного. */
    fun previousRequestDate(latest: LocalDate): LocalDate = latest.minusDays(1)
}

/** Разбор XML_daily.asp (ответ уже перекодирован из windows-1251). Без XML-парсера платформы. */
object CbrParser {

    private val DATE = Regex("""<ValCurs[^>]*\bDate="(\d{2})\.(\d{2})\.(\d{4})"""")
    private val VALUTE = Regex("""<Valute\b[^>]*>(.*?)</Valute>""", RegexOption.DOT_MATCHES_ALL)

    fun parse(xml: String): CbrDaily? {
        val date = DATE.find(xml)?.destructured?.let { (day, month, year) ->
            try {
                LocalDate.of(year.toInt(), month.toInt(), day.toInt())
            } catch (e: DateTimeException) {
                null
            }
        } ?: return null

        val rates = VALUTE.findAll(xml).mapNotNull { match ->
            val body = match.groupValues[1]
            val code = tag(body, "CharCode") ?: return@mapNotNull null
            rate(body)?.let { code to it }
        }.toMap()

        return CbrDaily(date, usd = rates["USD"] ?: return null, eur = rates["EUR"] ?: return null)
    }

    /** Курс за одну единицу: VunitRate, а если его нет — Value / Nominal. */
    private fun rate(body: String): Double? {
        tag(body, "VunitRate")?.let(::number)?.let { return it }
        val value = tag(body, "Value")?.let(::number) ?: return null
        val nominal = tag(body, "Nominal")?.trim()?.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return value / nominal
    }

    private fun tag(body: String, name: String): String? =
        Regex("<$name>([^<]*)</$name>").find(body)?.groupValues?.get(1)

    private fun number(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()
}
