package com.example.tvlauncher.settings

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Кнопка ⚙ на пульте в любом приложении открывает настройки. Раньше её обрабатывал Google TV Launcher,
 * но он отключён. Служба спец. возможностей видит кнопки раньше приложений; включает её tools/deploy.sh.
 * Остальные кнопки проходят дальше без изменений.
 */
class SettingsKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!SettingsKey.matches(event.keyCode)) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) SystemSettings.open(this)
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit
}
