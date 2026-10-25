package com.example.tvlauncher.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Пункты меню плитки по долгому нажатию OK. */
enum class TileAction(val title: String) {
    MOVE("Переместить"),
    ADD_TO_HOME("Добавить на главный экран"),
    REMOVE_FROM_HOME("Убрать с главного экрана"),
    STOP("Остановить…"),
    UNINSTALL("Удалить приложение"),
}

object TileActions {

    /**
     * «Остановить» — у настоящих приложений (не у служебной плитки «Настройки»).
     * «Удалить» — только у приложений, которые ставил пользователь: системные Android удалить не даст.
     */
    fun home(removable: Boolean, stoppable: Boolean): List<TileAction> =
        listOf(TileAction.MOVE, TileAction.REMOVE_FROM_HOME) + optional(removable, stoppable)

    fun allApps(onHome: Boolean, removable: Boolean, stoppable: Boolean): List<TileAction> =
        listOf(if (onHome) TileAction.REMOVE_FROM_HOME else TileAction.ADD_TO_HOME) + optional(removable, stoppable)

    private fun optional(removable: Boolean, stoppable: Boolean) =
        listOfNotNull(TileAction.STOP.takeIf { stoppable }, TileAction.UNINSTALL.takeIf { removable })

    /**
     * Системная страница приложения с кнопкой «Остановить». Сам лаунчер остановить чужое приложение
     * не может: прошивка TCL разрешает это только системе (killBackgroundProcesses → «Invalid packageName»).
     */
    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            return
        }
    }

    /** Системное окно удаления: подтверждение и само удаление делает Android. */
    fun requestUninstall(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            return
        }
    }
}
