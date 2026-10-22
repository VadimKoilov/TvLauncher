package com.example.tvlauncher.info

import android.content.SharedPreferences

/** Последние полученные погода и курсы — показываются, пока нет свежих (prefs-файл "info"). */
class InfoCache(private val prefs: SharedPreferences) {

    var weather: Weather?
        get() = if (!prefs.contains(W_TEMP)) null else Weather(
            tempC = Double.fromBits(prefs.getLong(W_TEMP, 0)),
            code = prefs.getInt(W_CODE, 0),
            isDay = prefs.getBoolean(W_DAY, true),
        )
        set(value) {
            val editor = prefs.edit()
            if (value == null) {
                editor.remove(W_TEMP).remove(W_CODE).remove(W_DAY)
            } else {
                editor.putLong(W_TEMP, value.tempC.toRawBits())
                    .putInt(W_CODE, value.code)
                    .putBoolean(W_DAY, value.isDay)
            }
            editor.apply()
        }

    var weatherAt: Long?
        get() = if (prefs.contains(W_AT)) prefs.getLong(W_AT, 0) else null
        set(value) {
            prefs.edit().apply { if (value == null) remove(W_AT) else putLong(W_AT, value) }.apply()
        }

    var rates: Rates?
        get() {
            val current = market(R_CUR) ?: return null
            return Rates(current, previous = market(R_PREV))
        }
        set(value) {
            val editor = prefs.edit()
            listOf(R_CUR, R_PREV).forEach { editor.remove("$it.at").remove("$it.usd").remove("$it.eur") }
            value?.let {
                putMarket(editor, R_CUR, it.current)
                it.previous?.let { previous -> putMarket(editor, R_PREV, previous) }
            }
            editor.apply()
        }

    /** Курсы ещё не получены после последнего включения. */
    var ratesPending: Boolean
        get() = prefs.getBoolean(R_PENDING, true)
        set(value) = prefs.edit().putBoolean(R_PENDING, value).apply()

    var ratesAttemptAt: Long?
        get() = if (prefs.contains(R_ATTEMPT)) prefs.getLong(R_ATTEMPT, 0) else null
        set(value) {
            prefs.edit().apply { if (value == null) remove(R_ATTEMPT) else putLong(R_ATTEMPT, value) }.apply()
        }

    private fun market(prefix: String): MarketRates? {
        if (!prefs.contains("$prefix.at")) return null
        return MarketRates(
            updatedAt = prefs.getLong("$prefix.at", 0),
            usd = Double.fromBits(prefs.getLong("$prefix.usd", 0)),
            eur = Double.fromBits(prefs.getLong("$prefix.eur", 0)),
        )
    }

    private fun putMarket(editor: SharedPreferences.Editor, prefix: String, m: MarketRates) {
        editor.putLong("$prefix.at", m.updatedAt)
            .putLong("$prefix.usd", m.usd.toRawBits())
            .putLong("$prefix.eur", m.eur.toRawBits())
    }

    private companion object {
        const val W_TEMP = "weather.temp"
        const val W_CODE = "weather.code"
        const val W_DAY = "weather.day"
        const val W_AT = "weather.at"
        const val R_CUR = "rates.current"
        const val R_PREV = "rates.previous"
        const val R_PENDING = "rates.pending"
        const val R_ATTEMPT = "rates.attempt"
    }
}
