package com.example.tvlauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class RuPluralTest {

    @Test
    fun appsUsesRussianPluralForms() {
        val cases = mapOf(
            1 to "1 приложение",
            2 to "2 приложения",
            5 to "5 приложений",
            11 to "11 приложений",
            12 to "12 приложений",
            19 to "19 приложений",
            21 to "21 приложение",
            22 to "22 приложения",
            111 to "111 приложений",
        )
        cases.forEach { (n, want) -> assertEquals("n=$n", want, RuPlural.apps(n)) }
    }
}
