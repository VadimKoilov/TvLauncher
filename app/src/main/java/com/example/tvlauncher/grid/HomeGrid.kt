package com.example.tvlauncher.grid

/** Правила сетки главного экрана (spec §7.1–7.2). Ключ плитки — имя пакета или [SETTINGS_KEY]. */
object HomeGrid {

    const val SETTINGS_KEY = "@settings"

    /** Начальный порядок при первом запуске; дальше порядок меняет только пользователь. */
    val DEFAULT_ORDER: List<String> = listOf(
        "com.google.android.youtube.tv",
        "ru.kinopoisk.tv",
        "to.pda.falcofemoralis.hdrezkaapp",
        "ru.more.play",
        "tv.twitch.android.app",
        "org.videolan.vlc",
        "com.internet.tvbrowser",
        "com.alphainventor.filemanager",
        "com.pigeoncast.screen.mirroring.android.tv",
        SETTINGS_KEY,
    )

    /** Сверяет сетку со списком установленного: удалённое убирает, новое дописывает в конец с меткой. */
    fun reconcile(state: GridState, installed: Set<String>): GridState {
        if (!state.initialized) {
            return GridState(
                order = DEFAULT_ORDER.filter { it in installed },
                known = installed,
                fresh = emptySet(),
                initialized = true,
            )
        }
        val gone = state.known - installed
        val added = (installed - state.known).sorted()
        return state.copy(
            order = state.order.filter { it !in gone } + added,
            known = state.known - gone + added,
            fresh = state.fresh - gone + added,
        )
    }

    /** Переставляет плитку; [to] прижимается к краям сетки. */
    fun move(state: GridState, from: Int, to: Int): GridState {
        val order = state.order.toMutableList()
        val key = order.removeAt(from)
        order.add(to.coerceIn(0, order.size), key)
        return state.copy(order = order)
    }

    fun remove(state: GridState, key: String): GridState =
        state.copy(order = state.order - key, fresh = state.fresh - key)

    fun add(state: GridState, key: String): GridState =
        if (key in state.order) state else state.copy(order = state.order + key)

    fun clearFresh(state: GridState, key: String): GridState =
        state.copy(fresh = state.fresh - key)
}
