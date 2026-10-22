package com.example.tvlauncher.info

import org.json.JSONException
import org.json.JSONObject

data class Weather(val tempC: Double, val code: Int, val isDay: Boolean)

/** Текущая погода в Кудрово из Open-Meteo (spec §7.4). */
object WeatherParser {

    const val URL = "https://api.open-meteo.com/v1/forecast?latitude=59.9006&longitude=30.5135" +
        "&current=temperature_2m,weather_code,is_day&timezone=Europe%2FMoscow"

    fun parse(json: String): Weather? = try {
        val current = JSONObject(json).getJSONObject("current")
        val temp = current.getDouble("temperature_2m")
        if (!temp.isFinite()) {
            null
        } else {
            Weather(tempC = temp, code = current.getInt("weather_code"), isDay = current.getInt("is_day") == 1)
        }
    } catch (e: JSONException) {
        null
    }
}

enum class WeatherIcon { CLEAR_DAY, CLEAR_NIGHT, PARTLY_DAY, PARTLY_NIGHT, CLOUDY, FOG, RAIN, SNOW, THUNDER }

/** Коды погоды WMO → подпись для «Кудрово · …» и иконка. */
object WeatherCodes {

    fun describe(code: Int): String = when (code) {
        0 -> "ясно"
        1 -> "малооблачно"
        2 -> "переменная облачность"
        3 -> "пасмурно"
        45, 48 -> "туман"
        51, 53, 55 -> "морось"
        56, 57 -> "ледяная морось"
        61 -> "небольшой дождь"
        63 -> "дождь"
        65 -> "сильный дождь"
        66, 67 -> "ледяной дождь"
        71 -> "небольшой снег"
        73 -> "снег"
        75 -> "сильный снег"
        77 -> "снежная крупа"
        80, 81 -> "ливень"
        82 -> "сильный ливень"
        85, 86 -> "снегопад"
        95 -> "гроза"
        96, 99 -> "гроза с градом"
        else -> ""
    }

    fun icon(code: Int, isDay: Boolean): WeatherIcon = when (code) {
        0 -> if (isDay) WeatherIcon.CLEAR_DAY else WeatherIcon.CLEAR_NIGHT
        1, 2 -> if (isDay) WeatherIcon.PARTLY_DAY else WeatherIcon.PARTLY_NIGHT
        3 -> WeatherIcon.CLOUDY
        45, 48 -> WeatherIcon.FOG
        in 51..67, in 80..82 -> WeatherIcon.RAIN
        in 71..77, 85, 86 -> WeatherIcon.SNOW
        in 95..99 -> WeatherIcon.THUNDER
        else -> WeatherIcon.CLOUDY
    }
}
