package com.example.tvlauncher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.tvlauncher.info.InfoCache
import com.example.tvlauncher.info.InfoRepository

/**
 * Запуск процесса лаунчера = включение телевизора (после загрузки HOME стартует первым),
 * а SCREEN_ON — выход из режима ожидания. В оба момента обновляем погоду и курсы.
 */
class LauncherApp : Application() {

    lateinit var info: InfoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        info = InfoRepository(InfoCache(getSharedPreferences("info", MODE_PRIVATE)))
        info.onPowerOn()
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = info.onPowerOn()
        }, IntentFilter(Intent.ACTION_SCREEN_ON))
    }
}
