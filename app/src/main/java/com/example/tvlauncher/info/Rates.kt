package com.example.tvlauncher.info

import org.json.JSONException
import org.json.JSONObject

/** Рыночный курс: рублей за 1 доллар и 1 евро; [updatedAt] — когда сервис обновил данные (unix, секунды). */
data class MarketRates(val updatedAt: Long, val usd: Double, val eur: Double)

/** Текущий курс и курс прошлого суточного обновления — для стрелки; при первом получении прошлого нет. */
data class Rates(val current: MarketRates, val previous: MarketRates?)

/**
 * Ответ open.er-api.com (ExchangeRate-API, открытый доступ): курсы относительно доллара, обновляются раз в сутки.
 * Условия сервиса требуют подписи «Rates By Exchange Rate API» рядом с курсами.
 */
object RatesParser {

    const val URL = "https://open.er-api.com/v6/latest/USD"

    fun parse(json: String): MarketRates? = try {
        val root = JSONObject(json)
        val rates = root.getJSONObject("rates")
        val rubPerUsd = rates.getDouble("RUB")
        val eurPerUsd = rates.getDouble("EUR")
        val valid = rubPerUsd.isFinite() && eurPerUsd.isFinite() && rubPerUsd > 0 && eurPerUsd > 0
        if (root.optString("result") != "success" || !valid) {
            null
        } else {
            MarketRates(root.getLong("time_last_update_unix"), usd = rubPerUsd, eur = rubPerUsd / eurPerUsd)
        }
    } catch (e: JSONException) {
        null
    }
}

object RatesHistory {

    /** Новое суточное обновление сдвигает текущий курс в «прошлый»; повтор того же обновления прошлый не трогает. */
    fun advance(stored: Rates?, fresh: MarketRates): Rates = when {
        stored == null -> Rates(fresh, previous = null)
        fresh.updatedAt == stored.current.updatedAt -> stored.copy(current = fresh)
        else -> Rates(fresh, previous = stored.current)
    }
}
