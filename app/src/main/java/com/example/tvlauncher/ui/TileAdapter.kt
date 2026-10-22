package com.example.tvlauncher.ui

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

    init {
        setHasStableIds(true)
    }

    fun submit(tiles: List<Tile>) {
        this.tiles = tiles
        notifyDataSetChanged()
    }

    fun tileAt(position: Int): Tile = tiles[position]

    override fun getItemCount(): Int = tiles.size

    override fun getItemId(position: Int): Long = when (val t = tiles[position]) {
        is Tile.App -> t.key.hashCode().toLong()
        Tile.All -> -1L
        Tile.Add -> -2L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tile, parent, false)
        FocusAnim.attach(view, parent.context.getColor(R.color.accent))
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val tile = tiles[position]
        holder.bind(tile)
        holder.itemView.setOnClickListener { onClick(tile) }
        holder.itemView.setOnLongClickListener {
            onLongClick(tile, it)
            true
        }
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val glyph: TextView = view.findViewById(R.id.glyph)
        private val plus: TextView = view.findViewById(R.id.plus)
        private val label: TextView = view.findViewById(R.id.label)
        private val badge: View = view.findViewById(R.id.badge)

        fun bind(tile: Tile) {
            icon.visibility = View.GONE
            glyph.visibility = View.GONE
            plus.visibility = View.GONE
            label.visibility = View.VISIBLE
            badge.visibility = View.GONE
            itemView.setBackgroundResource(R.drawable.bg_tile)
            itemView.isActivated = false

            when (tile) {
                is Tile.App -> {
                    icon.visibility = View.VISIBLE
                    icon.setImageDrawable(apps.icon(tile.key))
                    label.text = tile.label
                    badge.visibility = if (tile.fresh) View.VISIBLE else View.GONE
                    itemView.isActivated = tile.key == movingKey
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
