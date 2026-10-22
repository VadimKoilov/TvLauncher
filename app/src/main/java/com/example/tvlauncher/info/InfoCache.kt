package com.example.tvlauncher.info

import android.content.SharedPreferences
import java.time.LocalDate

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
            val current = daily(R_CUR) ?: return null
            val previous = daily(R_PREV) ?: return null
            return Rates(current, previous)
        }
        set(value) {
            val editor = prefs.edit()
            if (value == null) {
                listOf(R_CUR, R_PREV).forEach { editor.remove("$it.date").remove("$it.usd").remove("$it.eur") }
            } else {
                putDaily(editor, R_CUR, value.current)
                putDaily(editor, R_PREV, value.previous)
            }
            editor.apply()
        }

    /** Курсы ещё не получены после последнего включения. */
    var ratesPending: Boolean
        get() = prefs.getBoolean(R_PENDING, true)
        set(value) = prefs.edit().putBoolean(R_PENDING, value).apply()

    private fun daily(prefix: String): CbrDaily? {
        if (!prefs.contains("$prefix.date")) return null
        return CbrDaily(
            date = LocalDate.ofEpochDay(prefs.getLong("$prefix.date", 0)),
            usd = Double.fromBits(prefs.getLong("$prefix.usd", 0)),
            eur = Double.fromBits(prefs.getLong("$prefix.eur", 0)),
        )
    }

    private fun putDaily(editor: SharedPreferences.Editor, prefix: String, d: CbrDaily) {
        editor.putLong("$prefix.date", d.date.toEpochDay())
            .putLong("$prefix.usd", d.usd.toRawBits())
            .putLong("$prefix.eur", d.eur.toRawBits())
    }

    private companion object {
        const val W_TEMP = "weather.temp"
        const val W_CODE = "weather.code"
        const val W_DAY = "weather.day"
        const val W_AT = "weather.at"
        const val R_CUR = "rates.current"
        const val R_PREV = "rates.previous"
        const val R_PENDING = "rates.pending"
    }
}
