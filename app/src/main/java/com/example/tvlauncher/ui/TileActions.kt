package com.example.tvlauncher.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Пункты меню плитки по долгому нажатию OK. */
enum class TileAction(val title: String) {
    MOVE("Переместить"),
    ADD_TO_HOME("Добавить на главный экран"),
    REMOVE_FROM_HOME("Убрать с главного экрана"),
    UNINSTALL("Удалить приложение"),
}

object TileActions {

    /** «Удалить» — только у приложений, которые ставил пользователь: системные Android удалить не даст. */
    fun home(removable: Boolean): List<TileAction> =
        listOf(TileAction.MOVE, TileAction.REMOVE_FROM_HOME) + uninstall(removable)

    fun allApps(onHome: Boolean, removable: Boolean): List<TileAction> =
        listOf(if (onHome) TileAction.REMOVE_FROM_HOME else TileAction.ADD_TO_HOME) + uninstall(removable)

    private fun uninstall(removable: Boolean) = if (removable) listOf(TileAction.UNINSTALL) else emptyList()

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
