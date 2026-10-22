package com.example.tvlauncher.info

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {

    private val now = 1_792_700_000_000L
    private val minute = 60_000L

    @Test
    fun weatherAlwaysRefreshesOnPowerOn() {
        assertTrue(RefreshPolicy.weatherDue(Trigger.POWER_ON, lastSuccessMs = now, nowMs = now))
    }

    @Test
    fun weatherOnHomeRefreshesEvery30Minutes() {
        assertTrue(RefreshPolicy.weatherDue(Trigger.HOME_VISIBLE, lastSuccessMs = null, nowMs = now))
        assertFalse(RefreshPolicy.weatherDue(Trigger.HOME_VISIBLE, lastSuccessMs = now - 29 * minute, nowMs = now))
        assertTrue(RefreshPolicy.weatherDue(Trigger.HOME_VISIBLE, lastSuccessMs = now - 30 * minute, nowMs = now))
    }

    @Test
    fun ratesRefreshOnPowerOnOrWhenPending() {
        assertTrue(RefreshPolicy.ratesDue(Trigger.POWER_ON, ratesPending = false))
        assertFalse(RefreshPolicy.ratesDue(Trigger.HOME_VISIBLE, ratesPending = false))
        assertTrue(RefreshPolicy.ratesDue(Trigger.HOME_VISIBLE, ratesPending = true))
    }
}
