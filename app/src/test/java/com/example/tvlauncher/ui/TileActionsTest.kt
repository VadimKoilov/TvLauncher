package com.example.tvlauncher.ui

import com.example.tvlauncher.ui.TileAction.ADD_TO_HOME
import com.example.tvlauncher.ui.TileAction.MOVE
import com.example.tvlauncher.ui.TileAction.REMOVE_FROM_HOME
import com.example.tvlauncher.ui.TileAction.STOP
import com.example.tvlauncher.ui.TileAction.UNINSTALL
import org.junit.Assert.assertEquals
import org.junit.Test

class TileActionsTest {

    @Test
    fun homeMenuOffersStopAndUninstallWhenAllowed() {
        assertEquals(
            listOf(MOVE, REMOVE_FROM_HOME, STOP, UNINSTALL),
            TileActions.home(removable = true, stoppable = true),
        )
        assertEquals(listOf(MOVE, REMOVE_FROM_HOME), TileActions.home(removable = false, stoppable = false))
    }

    @Test
    fun allAppsMenuDependsOnHomeStoppableAndRemovable() {
        assertEquals(
            listOf(ADD_TO_HOME, STOP, UNINSTALL),
            TileActions.allApps(onHome = false, removable = true, stoppable = true),
        )
        assertEquals(
            listOf(REMOVE_FROM_HOME, STOP),
            TileActions.allApps(onHome = true, removable = false, stoppable = true),
        )
    }
}
