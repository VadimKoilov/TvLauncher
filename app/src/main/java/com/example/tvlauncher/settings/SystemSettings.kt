package com.example.tvlauncher.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Экраны системных настроек телевизора. */
object SystemSettings {

    /**
     * «Пульты и аксессуары» в настройках TCL: Bluetooth, сопряжение, подключённые наушники.
     * Стандартный ACTION_BLUETOOTH_SETTINGS на этом телевизоре никто не принимает.
     */
    private const val ACTION_ACCESSORIES = "com.android.tv.settings.ACCESSORIES_CONNECTED_DEVICES"

    fun open(context: Context) {
        start(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openBluetooth(context: Context) {
        if (!start(context, Intent(ACTION_ACCESSORIES))) open(context)
    }

    private fun start(context: Context, intent: Intent): Boolean =
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
}
