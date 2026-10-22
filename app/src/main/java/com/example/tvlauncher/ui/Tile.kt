package com.example.tvlauncher.ui

/** Плитка сетки: приложение или служебная «Все». */
sealed interface Tile {
    data class App(val key: String, val label: String, val fresh: Boolean) : Tile
    data object All : Tile
}
