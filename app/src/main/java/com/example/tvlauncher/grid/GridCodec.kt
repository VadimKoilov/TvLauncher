package com.example.tvlauncher.grid

/** Перевод [GridState] в строки для SharedPreferences и обратно. Ключи — имена пакетов, перевода строки в них нет. */
object GridCodec {

    private const val ORDER = "order"
    private const val KNOWN = "known"
    private const val FRESH = "fresh"
    private const val INITIALIZED = "initialized"

    fun encode(s: GridState): Map<String, String> = mapOf(
        ORDER to s.order.joinToString("\n"),
        KNOWN to s.known.sorted().joinToString("\n"),
        FRESH to s.fresh.sorted().joinToString("\n"),
        INITIALIZED to if (s.initialized) "1" else "0",
    )

    fun decode(m: Map<String, String>): GridState = GridState(
        order = split(m[ORDER]),
        known = split(m[KNOWN]).toSet(),
        fresh = split(m[FRESH]).toSet(),
        initialized = m[INITIALIZED] == "1",
    )

    private fun split(s: String?): List<String> =
        if (s.isNullOrEmpty()) emptyList() else s.split("\n")
}
