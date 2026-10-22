package com.example.tvlauncher.info

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Погода и курсы: загружаются только при включении телевизора и пока виден главный экран
 * ([RefreshPolicy]). Запросы идут в одном фоновом потоке, [listener] вызывается на главном.
 */
class InfoRepository(
    private val cache: InfoCache,
    private val now: () -> Long = System::currentTimeMillis,
) {

    var listener: (() -> Unit)? = null

    val weather: Weather? get() = cache.weather

    val rates: Rates? get() = cache.rates

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val weatherBusy = AtomicBoolean(false)
    private val ratesBusy = AtomicBoolean(false)

    fun onPowerOn() = refresh(Trigger.POWER_ON)

    fun onHomeVisible() = refresh(Trigger.HOME_VISIBLE)

    private fun refresh(trigger: Trigger) {
        if (RefreshPolicy.weatherDue(trigger, cache.weatherAt, now()) && weatherBusy.compareAndSet(false, true)) {
            Log.i(TAG, "weather fetch trigger=$trigger")
            executor.execute {
                try {
                    Http.get(WeatherParser.URL)?.let(WeatherParser::parse)?.let {
                        cache.weather = it
                        cache.weatherAt = now()
                        notifyChanged()
                    }
                } finally {
                    weatherBusy.set(false)
                }
            }
        }

        if (trigger == Trigger.POWER_ON) cache.ratesPending = true
        val ratesDue = RefreshPolicy.ratesDue(trigger, cache.ratesPending, cache.ratesAttemptAt, now())
        if (ratesDue && ratesBusy.compareAndSet(false, true)) {
            Log.i(TAG, "rates fetch trigger=$trigger")
            cache.ratesAttemptAt = now()
            executor.execute {
                try {
                    Http.get(RatesParser.URL)?.let(RatesParser::parse)?.let {
                        cache.rates = RatesHistory.advance(cache.rates, it)
                        cache.ratesPending = false
                        notifyChanged()
                    }
                } finally {
                    ratesBusy.set(false)
                }
            }
        }
    }

    private fun notifyChanged() {
        main.post { listener?.invoke() }
    }

    companion object {
        const val TAG = "TvLauncher"
    }
}
