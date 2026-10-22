package com.example.tvlauncher.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RatesTest {

    private fun valute(code: String, nominal: Int, value: String, vunit: String?) =
        """<Valute ID="R0"><NumCode>0</NumCode><CharCode>$code</CharCode><Nominal>$nominal</Nominal>""" +
            """<Name>x</Name><Value>$value</Value>""" + (vunit?.let { "<VunitRate>$it</VunitRate>" } ?: "") + "</Valute>"

    private fun xml(vararg valutes: String) =
        """<?xml version="1.0" encoding="windows-1251"?><ValCurs Date="23.10.2026" name="Foreign Currency Market">""" +
            valute("AUD", 1, "53,1234", "53,1234") + valutes.joinToString("") + "</ValCurs>"

    @Test
    fun parsesLatestCbr() {
        val parsed = CbrParser.parse(
            xml(valute("USD", 1, "81,1255", "81,1255"), valute("EUR", 1, "91,1364", "91,1364")),
        )

        assertEquals(CbrDaily(LocalDate.of(2026, 10, 23), usd = 81.1255, eur = 91.1364), parsed)
    }

    @Test
    fun usesNominalWhenNoVunitRate() {
        val parsed = CbrParser.parse(xml(valute("USD", 10, "811,255", null), valute("EUR", 1, "91,1364", null)))

        assertEquals(81.1255, parsed!!.usd, 1e-9)
    }

    @Test
    fun parseRejectsHtmlAndPartial() {
        assertNull(CbrParser.parse("<html>Доступ запрещён</html>"))
        assertNull(CbrParser.parse(xml(valute("USD", 1, "81,1255", "81,1255"))))
        assertNull(CbrParser.parse(""))
    }

    @Test
    fun urlsAndPreviousDate() {
        assertEquals(
            "https://www.cbr.ru/scripts/XML_daily.asp?date_req=22/10/2026",
            Cbr.urlFor(LocalDate.of(2026, 10, 22)),
        )
        assertEquals(LocalDate.of(2026, 10, 22), Cbr.previousRequestDate(LocalDate.of(2026, 10, 23)))
    }
}
