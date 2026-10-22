package com.example.tvlauncher.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import com.example.tvlauncher.grid.HomeGrid

/** [removable] — приложение ставил пользователь, его можно удалить. */
data class LaunchableApp(val key: String, val label: String, val launch: Intent, val removable: Boolean = false)

/** Приложения, которые можно запустить с телевизора, плюс служебная плитка «Настройки». */
class InstalledApps(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val icons = HashMap<String, Drawable>()
    private val iconSizePx = context.resources.getDimensionPixelSize(com.example.tvlauncher.R.dimen.tile_icon) * 3 / 2

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
                val appInfo = info.activityInfo.applicationInfo
                val system = appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                result[pkg] = LaunchableApp(
                    key = pkg,
                    label = Labels.display(pkg, appInfo.loadLabel(pm).toString()),
                    launch = launch,
                    removable = !system,
                )
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
        val raw = try {
            pm.getApplicationIcon(pkg)
        } catch (e: PackageManager.NameNotFoundException) {
            pm.defaultActivityIcon
        }
        IconCleaner.clean(raw, iconSizePx, context.resources)
    }

    private companion object {
        const val SETTINGS_PACKAGE = "com.android.tv.settings"
    }
}
