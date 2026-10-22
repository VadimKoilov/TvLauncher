package com.example.tvlauncher.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.tvlauncher.R
import com.example.tvlauncher.info.Format
import com.example.tvlauncher.info.Rates
import com.example.tvlauncher.info.Trend
import com.example.tvlauncher.info.Weather
import com.example.tvlauncher.info.WeatherCodes
import com.example.tvlauncher.info.WeatherIcon

/** Заполнение карточек погоды и курсов. dim — данные не свежие (нет интернета). */
object InfoViews {

    private const val CITY = "Кудрово"
    private const val DIM_ALPHA = 0.45f

    fun bindWeather(card: View, w: Weather?, dim: Boolean) {
        val icon = card.findViewById<ImageView>(R.id.weather_icon)
        val temp = card.findViewById<TextView>(R.id.weather_temp)
        val sub = card.findViewById<TextView>(R.id.weather_sub)

        if (w == null) {
            icon.visibility = View.GONE
            temp.text = "—"
            sub.text = CITY
        } else {
            icon.visibility = View.VISIBLE
            icon.setImageResource(iconRes(WeatherCodes.icon(w.code, w.isDay)))
            temp.text = Format.temperature(w.tempC)
            val description = WeatherCodes.describe(w.code)
            sub.text = if (description.isEmpty()) CITY else "$CITY · $description"
        }
        card.alpha = if (dim) DIM_ALPHA else 1f
        card.visibility = View.VISIBLE
    }

    fun bindRates(card: View, r: Rates?, dim: Boolean) {
        bindRate(card, R.id.usd_value, R.id.usd_delta, r?.current?.usd, r?.previous?.usd)
        bindRate(card, R.id.eur_value, R.id.eur_delta, r?.current?.eur, r?.previous?.eur)
        card.alpha = if (dim) DIM_ALPHA else 1f
        card.visibility = View.VISIBLE
    }

    private fun bindRate(card: View, valueId: Int, deltaId: Int, current: Double?, previous: Double?) {
        val value = card.findViewById<TextView>(valueId)
        val delta = card.findViewById<TextView>(deltaId)
        if (current == null || previous == null) {
            value.text = "—"
            delta.text = ""
            return
        }
        value.text = Format.rate(current)
        val d = Format.delta(current - previous)
        delta.text = d.text
        delta.setTextColor(card.context.getColor(when (d.trend) {
            Trend.UP -> R.color.up
            Trend.DOWN -> R.color.down
            Trend.FLAT -> R.color.text_muted
        }))
    }

    private fun iconRes(icon: WeatherIcon): Int = when (icon) {
        WeatherIcon.CLEAR_DAY -> R.drawable.ic_wx_clear_day
        WeatherIcon.CLEAR_NIGHT -> R.drawable.ic_wx_clear_night
        WeatherIcon.PARTLY_DAY -> R.drawable.ic_wx_partly_day
        WeatherIcon.PARTLY_NIGHT -> R.drawable.ic_wx_partly_night
        WeatherIcon.CLOUDY -> R.drawable.ic_wx_cloudy
        WeatherIcon.FOG -> R.drawable.ic_wx_fog
        WeatherIcon.RAIN -> R.drawable.ic_wx_rain
        WeatherIcon.SNOW -> R.drawable.ic_wx_snow
        WeatherIcon.THUNDER -> R.drawable.ic_wx_thunder
    }
}
