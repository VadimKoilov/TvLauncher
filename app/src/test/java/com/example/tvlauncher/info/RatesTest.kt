package com.example.tvlauncher.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatesTest {

    // Сокращённый ответ open.er-api.com/v6/latest/USD от 22.10.2026.
    private val sample = """{"result":"success","provider":"https://www.exchangerate-api.com",""" +
        """"time_last_update_unix":1792627351,"time_last_update_utc":"Thu, 22 Oct 2026 00:02:31 +0000",""" +
        """"base_code":"USD","rates":{"USD":1,"EUR":0.890298,"RUB":82.003854}}"""

    @Test
    fun parsesUsdAndEurInRubles() {
        val parsed = RatesParser.parse(sample)!!

        assertEquals(1792627351L, parsed.updatedAt)
        assertEquals(82.003854, parsed.usd, 1e-9)
        assertEquals(92.1083, parsed.eur, 1e-4)
    }

    @Test
    fun parseRejectsErrorsAndGarbage() {
        assertNull(RatesParser.parse("""{"result":"error","error-type":"unsupported-code"}"""))
        assertNull(RatesParser.parse("""{"result":"success","time_last_update_unix":1,"rates":{"EUR":0.89}}"""))
        assertNull(RatesParser.parse("""{"result":"success","time_last_update_unix":1,"rates":{"RUB":82.0,"EUR":0}}"""))
        assertNull(RatesParser.parse("<html>Too Many Requests</html>"))
        assertNull(RatesParser.parse(""))
    }

    @Test
    fun parseRejectsNonFiniteRates() {
        assertNull(RatesParser.parse("""{"result":"success","time_last_update_unix":1,"rates":{"RUB":"NaN","EUR":0.89}}"""))
        assertNull(RatesParser.parse("""{"result":"success","time_last_update_unix":1,"rates":{"RUB":82.0,"EUR":"Infinity"}}"""))
    }

    private fun rates(at: Long, usd: Double) = MarketRates(updatedAt = at, usd = usd, eur = usd + 10)

    @Test
    fun firstFetchHasNoPrevious() {
        assertEquals(Rates(rates(2, 82.0), previous = null), RatesHistory.advance(null, rates(2, 82.0)))
    }

    @Test
    fun newDailyUpdateBecomesCurrentAndOldOneBecomesPrevious() {
        val stored = Rates(rates(2, 82.0), previous = rates(1, 81.0))

        assertEquals(Rates(rates(3, 83.0), previous = rates(2, 82.0)), RatesHistory.advance(stored, rates(3, 83.0)))
    }

    @Test
    fun sameDailyUpdateKeepsPrevious() {
        val stored = Rates(rates(2, 82.0), previous = rates(1, 81.0))

        assertEquals(stored, RatesHistory.advance(stored, rates(2, 82.0)))
    }
}
