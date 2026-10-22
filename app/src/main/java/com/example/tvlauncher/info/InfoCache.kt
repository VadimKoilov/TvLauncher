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

    private companion object {
        const val W_TEMP = "weather.temp"
        const val W_CODE = "weather.code"
        const val W_DAY = "weather.day"
        const val W_AT = "weather.at"
    }
}
