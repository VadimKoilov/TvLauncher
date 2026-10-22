package com.example.tvlauncher.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.tvlauncher.R
import com.example.tvlauncher.apps.InstalledApps
import com.example.tvlauncher.apps.LaunchableApp
import com.example.tvlauncher.grid.GridState
import com.example.tvlauncher.grid.GridStore
import com.example.tvlauncher.grid.HomeGrid
import com.example.tvlauncher.info.Format
import java.time.LocalDateTime

class HomeActivity : Activity() {

    private lateinit var apps: InstalledApps
    private lateinit var store: GridStore
    private lateinit var adapter: TileAdapter

    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var hdmiButton: View
    private lateinit var grid: RecyclerView

    private var state = GridState.EMPTY
    private var installed: Map<String, LaunchableApp> = emptyMap()
    private var focusFirstOnResume = true

    private val timeTick = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateClock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        hdmiButton = findViewById(R.id.hdmi_button)
        grid = findViewById(R.id.grid)

        apps = InstalledApps(this)
        store = GridStore(getSharedPreferences("grid", MODE_PRIVATE))
        adapter = TileAdapter(apps, ::onTileClick, ::onTileLongClick)

        grid.layoutManager = GridLayoutManager(this, COLUMNS)
        grid.adapter = adapter
        (grid.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        FocusAnim.attach(hdmiButton, getColor(R.color.accent))
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(timeTick, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })
        updateClock()
    }

    override fun onResume() {
        super.onResume()
        refreshGrid()
        if (focusFirstOnResume) {
            focusFirstOnResume = false
            focusFirstTile()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(timeTick)
    }

    /** Кнопка «Домой», когда лаунчер уже открыт или вернулись из приложения. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        focusFirstOnResume = true
    }

    /** На главном экране «Назад» ничего не делает. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = Unit

    private fun updateClock() {
        val now = LocalDateTime.now()
        clock.text = Format.time(now.toLocalTime())
        date.text = Format.date(now.toLocalDate())
    }

    private fun refreshGrid() {
        installed = apps.query()
        state = HomeGrid.reconcile(store.load(), installed.keys)
        store.save(state)
        adapter.submit(tiles())
    }

    private fun tiles(): List<Tile> =
        state.order.mapNotNull { key ->
            installed[key]?.let { Tile.App(key, it.label, key in state.fresh) }
        } + Tile.All + Tile.Add

    private fun focusFirstTile() {
        grid.scrollToPosition(0)
        grid.post { grid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
    }

    private fun onTileClick(tile: Tile) {
        when (tile) {
            is Tile.App -> launch(tile.key)
            Tile.All, Tile.Add -> Unit // экран «Все приложения» — Task 5
        }
    }

    private fun onTileLongClick(tile: Tile, view: View) = Unit // меню плитки — Task 6

    private fun launch(key: String) {
        val app = installed[key] ?: return
        try {
            startActivity(app.launch)
        } catch (e: ActivityNotFoundException) {
            return
        }
        if (key in state.fresh) {
            state = HomeGrid.clearFresh(state, key)
            store.save(state)
        }
    }

    private companion object {
        const val COLUMNS = 6
    }
}
