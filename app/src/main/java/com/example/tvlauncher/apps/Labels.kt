package com.example.tvlauncher.apps

/** Подписи плиток: системное название, а для нескольких длинных или некрасивых — короткое (spec §7.1). */
object Labels {

    private val SHORT = mapOf(
        "com.internet.tvbrowser" to "Браузер",
        "com.alphainventor.filemanager" to "Файлы",
        "to.pda.falcofemoralis.hdrezkaapp" to "HDrezka",
    )

    fun display(packageName: String, systemLabel: String): String =
        SHORT[packageName] ?: systemLabel.trim().ifEmpty { packageName }
}
