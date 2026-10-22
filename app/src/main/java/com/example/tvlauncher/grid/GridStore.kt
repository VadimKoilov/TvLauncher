package com.example.tvlauncher.grid

import android.content.SharedPreferences

/** Хранит сетку в prefs-файле "grid". */
class GridStore(private val prefs: SharedPreferences) {

    fun load(): GridState =
        GridCodec.decode(prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap())

    fun save(s: GridState) {
        val editor = prefs.edit()
        GridCodec.encode(s).forEach { (k, v) -> editor.putString(k, v) }
        editor.apply()
    }
}
