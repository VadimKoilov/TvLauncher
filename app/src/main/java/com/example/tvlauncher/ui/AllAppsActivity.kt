package com.example.tvlauncher.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.tvlauncher.R
import com.example.tvlauncher.apps.AppSorting
import com.example.tvlauncher.apps.InstalledApps
import com.example.tvlauncher.apps.LaunchableApp
import com.example.tvlauncher.apps.RuPlural
import com.example.tvlauncher.grid.GridState
import com.example.tvlauncher.grid.GridStore
import com.example.tvlauncher.grid.HomeGrid

/** Все установленные приложения по алфавиту; отсюда их добавляют на главный экран и убирают с него. */
class AllAppsActivity : Activity() {

    private lateinit var apps: InstalledApps
    private lateinit var store: GridStore
    private lateinit var adapter: TileAdapter
    private lateinit var grid: RecyclerView
    private lateinit var count: TextView

    private var state = GridState.EMPTY
    private var installed: Map<String, LaunchableApp> = emptyMap()
    private var focusFirst = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(GlowBackground(getColor(R.color.bg), getColor(R.color.glow)))
        setContentView(R.layout.activity_all_apps)

        grid = findViewById(R.id.grid)
        count = findViewById(R.id.count)

        apps = InstalledApps(this)
        store = GridStore(getSharedPreferences("grid", MODE_PRIVATE))
        adapter = TileAdapter(apps, ::onTileClick, ::onTileLongClick)

        grid.layoutManager = GridLayoutManager(this, COLUMNS)
        grid.adapter = adapter
        (grid.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    override fun onResume() {
        super.onResume()
        installed = apps.query()
        state = HomeGrid.reconcile(store.load(), installed.keys)
        store.save(state)

        val sorted = AppSorting.byLabel(installed.values.toList()) { it.label }
        adapter.homeKeys = state.order.toSet()
        adapter.submit(sorted.map { Tile.App(it.key, it.label, fresh = false) })
        count.text = RuPlural.apps(sorted.size)

        if (focusFirst) {
            focusFirst = false
            grid.post { grid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
        }
    }

    private fun onTileClick(tile: Tile) {
        if (tile !is Tile.App) return
        val app = installed[tile.key] ?: return
        try {
            startActivity(app.launch)
        } catch (e: ActivityNotFoundException) {
            return
        } catch (e: SecurityException) {
            return
        }
        if (tile.key in state.fresh) update(HomeGrid.clearFresh(state, tile.key))
    }

    private fun onTileLongClick(tile: Tile, view: View) {
        if (tile !is Tile.App) return
        val item = if (tile.key in state.order) {
            "Убрать с главного экрана" to { update(HomeGrid.remove(state, tile.key)) }
        } else {
            "Добавить на главный экран" to { update(HomeGrid.add(state, tile.key)) }
        }
        TileMenu(view, listOf(item)).show()
    }

    private fun update(newState: GridState) {
        state = newState
        store.save(state)
        adapter.homeKeys = state.order.toSet()
    }

    private companion object {
        const val COLUMNS = 6
    }
}
