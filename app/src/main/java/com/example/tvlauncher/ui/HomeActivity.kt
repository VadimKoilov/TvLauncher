package com.example.tvlauncher.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
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
import com.example.tvlauncher.hdmi.HdmiSwitcher
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
    private lateinit var moveHint: View

    private var state = GridState.EMPTY
    private var installed: Map<String, LaunchableApp> = emptyMap()
    private var focusFirstOnResume = true

    /** Состояние до начала перемещения; не null — идёт режим перемещения. */
    private var moveOrigin: GridState? = null

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
        moveHint = findViewById(R.id.move_hint)

        apps = InstalledApps(this)
        store = GridStore(getSharedPreferences("grid", MODE_PRIVATE))
        adapter = TileAdapter(apps, ::onTileClick, ::onTileLongClick)

        grid.layoutManager = GridLayoutManager(this, COLUMNS)
        grid.adapter = adapter
        (grid.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        FocusAnim.attach(hdmiButton, getColor(R.color.accent))
        hdmiButton.setOnClickListener { HdmiSwitcher(this).switchToHdmi1() }
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
        if (moveOrigin != null) finishMove(commit = false)
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
            Tile.All, Tile.Add -> startActivity(Intent(this, AllAppsActivity::class.java))
        }
    }

    private fun onTileLongClick(tile: Tile, view: View) {
        if (tile !is Tile.App || moveOrigin != null) return
        TileMenu(
            view,
            listOf(
                "Переместить" to { startMove(tile.key) },
                "Убрать с главного экрана" to { removeFromHome(tile.key) },
            ),
        ).show()
    }

    /** В режиме перемещения стрелки двигают плитку, OK фиксирует, «Назад» отменяет. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (moveOrigin == null) return super.dispatchKeyEvent(event)
        val step = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> -1
            KeyEvent.KEYCODE_DPAD_RIGHT -> 1
            KeyEvent.KEYCODE_DPAD_UP -> -COLUMNS
            KeyEvent.KEYCODE_DPAD_DOWN -> COLUMNS
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BACK -> 0
            else -> return super.dispatchKeyEvent(event)
        }
        if (event.action != KeyEvent.ACTION_DOWN) return true
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK -> finishMove(commit = false)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
                finishMove(commit = true)
            else -> moveBy(step)
        }
        return true
    }

    private fun startMove(key: String) {
        moveOrigin = state
        adapter.movingKey = key
        moveHint.visibility = View.VISIBLE
        focusKey(key)
    }

    private fun moveBy(step: Int) {
        val key = adapter.movingKey ?: return
        val from = state.order.indexOf(key)
        if (from < 0) return
        // Только среди приложений: на «Все» и «+» плитка не заезжает.
        val to = (from + step).coerceIn(0, state.order.lastIndex)
        if (to == from) return
        state = HomeGrid.move(state, from, to)
        adapter.moveItem(from, to)
        grid.scrollToPosition(to)
        focusKey(key)
    }

    private fun finishMove(commit: Boolean) {
        val key = adapter.movingKey
        val origin = moveOrigin ?: return
        state = if (commit && key != null) HomeGrid.clearFresh(state, key) else origin
        if (commit) store.save(state)
        moveOrigin = null
        adapter.movingKey = null
        moveHint.visibility = View.GONE
        adapter.submit(tiles())
        key?.let(::focusKey)
    }

    private fun removeFromHome(key: String) {
        val index = state.order.indexOf(key)
        state = HomeGrid.remove(state, key)
        store.save(state)
        adapter.submit(tiles())
        // Фокус на соседнюю плитку (или на «Все», если приложений не осталось).
        val position = index.coerceAtMost(state.order.size).coerceAtLeast(0)
        grid.scrollToPosition(position)
        grid.post { grid.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
    }

    private fun focusKey(key: String) {
        val position = adapter.positionOf(key)
        if (position < 0) return
        grid.scrollToPosition(position)
        grid.post { grid.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
    }

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
