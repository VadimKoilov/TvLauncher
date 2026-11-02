package com.example.tvlauncher.settings

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsKeyTest {

    @Test
    fun tclRemoteGearAndStandardSettingsKeyOpenSettings() {
        assertTrue(SettingsKey.matches(4077))
        assertTrue(SettingsKey.matches(KeyEvent.KEYCODE_SETTINGS))
    }

    @Test
    fun otherRemoteKeysPassThrough() {
        assertFalse(SettingsKey.matches(KeyEvent.KEYCODE_MENU))
        assertFalse(SettingsKey.matches(KeyEvent.KEYCODE_DPAD_CENTER))
        assertFalse(SettingsKey.matches(KeyEvent.KEYCODE_BACK))
    }
}
