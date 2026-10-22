package com.example.tvlauncher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import com.example.tvlauncher.grid.HomeGrid

data class LaunchableApp(val key: String, val label: String, val launch: Intent)

/** Приложения, которые можно запустить с телевизора, плюс служебная плитка «Настройки». */
class InstalledApps(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val icons = HashMap<String, Drawable>()

    fun query(): Map<String, LaunchableApp> {
        val result = LinkedHashMap<String, LaunchableApp>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(intent, 0)) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName || pkg in result) continue
                // Intent собираем из найденной activity: без отдельных запросов к системе на каждый пакет.
                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(category)
                    .setClassName(pkg, info.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                val label = info.activityInfo.applicationInfo.loadLabel(pm).toString()
                result[pkg] = LaunchableApp(key = pkg, label = Labels.display(pkg, label), launch = launch)
            }
        }
        result[HomeGrid.SETTINGS_KEY] = LaunchableApp(
            key = HomeGrid.SETTINGS_KEY,
            label = "Настройки",
            launch = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return result
    }

    fun icon(key: String): Drawable = icons.getOrPut(key) {
        val pkg = if (key == HomeGrid.SETTINGS_KEY) SETTINGS_PACKAGE else key
        try {
            pm.getApplicationIcon(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            pm.defaultActivityIcon
        }
    }

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.tv.settings"
    }
}
