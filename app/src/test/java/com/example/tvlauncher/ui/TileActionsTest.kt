package com.example.tvlauncher.ui

import com.example.tvlauncher.ui.TileAction.ADD_TO_HOME
import com.example.tvlauncher.ui.TileAction.MOVE
import com.example.tvlauncher.ui.TileAction.REMOVE_FROM_HOME
import com.example.tvlauncher.ui.TileAction.UNINSTALL
import org.junit.Assert.assertEquals
import org.junit.Test

class TileActionsTest {

    @Test
    fun homeMenuOffersUninstallOnlyForUserApps() {
        assertEquals(listOf(MOVE, REMOVE_FROM_HOME, UNINSTALL), TileActions.home(removable = true))
        assertEquals(listOf(MOVE, REMOVE_FROM_HOME), TileActions.home(removable = false))
    }

    @Test
    fun allAppsMenuDependsOnHomeAndRemovable() {
        assertEquals(listOf(ADD_TO_HOME, UNINSTALL), TileActions.allApps(onHome = false, removable = true))
        assertEquals(listOf(REMOVE_FROM_HOME), TileActions.allApps(onHome = true, removable = false))
    }
}
