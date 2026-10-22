package com.example.tvlauncher.apps

import java.text.Collator
import java.util.Locale

/** Алфавитный порядок для «Все приложения» с учётом русской локали, без учёта регистра. */
object AppSorting {

    fun <T> byLabel(items: List<T>, label: (T) -> String): List<T> {
        val collator = Collator.getInstance(Locale("ru")).apply { strength = Collator.SECONDARY }
        return items.sortedWith { a, b -> collator.compare(label(a), label(b)) }
    }
}
