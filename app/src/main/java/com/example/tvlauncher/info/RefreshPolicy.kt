package com.example.tvlauncher.info

/** Что вызвало проверку: включение телевизора или показ главного экрана. */
enum class Trigger { POWER_ON, HOME_VISIBLE }

/** Когда обновлять погоду и курсы (spec §7.4–7.6). Фоновой работы нет — только эти два момента. */
object RefreshPolicy {

    const val WEATHER_MIN_INTERVAL_MS = 1_800_000L

    fun weatherDue(trigger: Trigger, lastSuccessMs: Long?, nowMs: Long): Boolean = when (trigger) {
        Trigger.POWER_ON -> true
        Trigger.HOME_VISIBLE -> lastSuccessMs == null || nowMs - lastSuccessMs >= WEATHER_MIN_INTERVAL_MS
    }

    /** Повтор неудачной загрузки курсов не чаще раза в 10 минут: сервис курсов ограничивает частые запросы. */
    const val RATES_RETRY_MS = 600_000L

    /** Курсы — только при включении; на главном экране догружаются, если при включении не вышло. */
    fun ratesDue(trigger: Trigger, ratesPending: Boolean, lastAttemptMs: Long?, nowMs: Long): Boolean =
        when (trigger) {
            Trigger.POWER_ON -> true
            Trigger.HOME_VISIBLE ->
                ratesPending && (lastAttemptMs == null || nowMs - lastAttemptMs >= RATES_RETRY_MS)
        }
}
