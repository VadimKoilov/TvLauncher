package com.example.tvlauncher.settings

import android.view.KeyEvent

/** Кнопка ⚙ на пульте. Пульт TCL RC902V шлёт не стандартную KEYCODE_SETTINGS, а свою KEYCODE_TCL_SET. */
object SettingsKey {

    /** KEYCODE_TCL_SET из прошивки TCL: в SDK её нет (скан-код 233 в Generic.kl). */
    const val KEYCODE_TCL_SET = 4077

    fun matches(keyCode: Int): Boolean =
        keyCode == KEYCODE_TCL_SET || keyCode == KeyEvent.KEYCODE_SETTINGS
}
