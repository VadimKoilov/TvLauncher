package com.example.tvlauncher.apps

object RuPlural {

    /** «1 приложение», «3 приложения», «11 приложений». */
    fun apps(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        val word = when {
            mod100 in 11..14 -> "приложений"
            mod10 == 1 -> "приложение"
            mod10 in 2..4 -> "приложения"
            else -> "приложений"
        }
        return "$n $word"
    }
}
