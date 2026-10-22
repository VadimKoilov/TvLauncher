package com.example.tvlauncher.ui

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tvlauncher.R
import com.example.tvlauncher.apps.InstalledApps

class TileAdapter(
    private val apps: InstalledApps,
    private val onClick: (Tile) -> Unit,
    private val onLongClick: (Tile, View) -> Unit,
) : RecyclerView.Adapter<TileAdapter.Holder>() {

    private var tiles: List<Tile> = emptyList()

    /** Ключ плитки в режиме перемещения: она подсвечивается как «activated». */
    var movingKey: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /** Ключи плиток на главном экране: если задано, у них рисуется точка (экран «Все приложения»). */
    var homeKeys: Set<String>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    fun submit(tiles: List<Tile>) {
        this.tiles = tiles
        notifyDataSetChanged()
    }

    fun tileAt(position: Int): Tile = tiles[position]

    fun positionOf(key: String): Int = tiles.indexOfFirst { it is Tile.App && it.key == key }

    /** Перемещение без полной перерисовки: View плитки едет вместе с ней и не теряет фокус. */
    fun moveItem(from: Int, to: Int) {
        tiles = tiles.toMutableList().apply { add(to, removeAt(from)) }
        notifyItemMoved(from, to)
    }

    override fun getItemCount(): Int = tiles.size

    override fun getItemId(position: Int): Long = when (val t = tiles[position]) {
        is Tile.App -> t.key.hashCode().toLong()
        Tile.All -> -1L
        Tile.Add -> -2L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tile, parent, false)
        FocusAnim.attach(view, parent.context.getColor(R.color.accent))
        val holder = Holder(view)
        view.setOnClickListener { holder.tile?.let(onClick) }
        // Долгое OK распознаём по автоповтору клавиши, а не таймером View: так оно срабатывает один раз
        // и с пультом, и с `adb shell input keyevent --longpress`.
        view.setOnKeyListener { v, keyCode, event ->
            if (!isOk(keyCode) || event.action != KeyEvent.ACTION_DOWN || event.repeatCount == 0) {
                return@setOnKeyListener false
            }
            if (event.repeatCount == 1) {
                v.isPressed = false
                holder.tile?.let { onLongClick(it, v) }
            }
            true
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(tiles[position])
    }

    private fun isOk(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val glyph: TextView = view.findViewById(R.id.glyph)
        private val plus: TextView = view.findViewById(R.id.plus)
        private val label: TextView = view.findViewById(R.id.label)
        private val badge: View = view.findViewById(R.id.badge)
        private val homeDot: View = view.findViewById(R.id.home_dot)

        var tile: Tile? = null
            private set

        fun bind(tile: Tile) {
            this.tile = tile
            icon.visibility = View.GONE
            glyph.visibility = View.GONE
            plus.visibility = View.GONE
            label.visibility = View.VISIBLE
            badge.visibility = View.GONE
            homeDot.visibility = View.GONE
            itemView.setBackgroundResource(R.drawable.bg_tile)
            itemView.isActivated = false

            when (tile) {
                is Tile.App -> {
                    icon.visibility = View.VISIBLE
                    icon.setImageDrawable(apps.icon(tile.key))
                    label.text = tile.label
                    badge.visibility = if (tile.fresh) View.VISIBLE else View.GONE
                    itemView.isActivated = tile.key == movingKey
                    homeDot.visibility = if (homeKeys?.contains(tile.key) == true) View.VISIBLE else View.GONE
                }
                Tile.All -> {
                    glyph.visibility = View.VISIBLE
                    glyph.text = "⋯"
                    label.text = "Все"
                }
                Tile.Add -> {
                    plus.visibility = View.VISIBLE
                    label.visibility = View.GONE
                    itemView.setBackgroundResource(R.drawable.bg_tile_add)
                }
            }
        }
    }
}
