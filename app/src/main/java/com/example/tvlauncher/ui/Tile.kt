package com.example.tvlauncher.ui

/** Плитка сетки: приложение или служебные «Все» и «+». */
sealed interface Tile {
    data class App(val key: String, val label: String, val fresh: Boolean) : Tile
    data object All : Tile
    data object Add : Tile
}
