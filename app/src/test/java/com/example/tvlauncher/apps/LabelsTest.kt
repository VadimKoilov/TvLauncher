package com.example.tvlauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class LabelsTest {

    @Test
    fun shortensKnownLongNames() {
        assertEquals("Браузер", Labels.display("com.internet.tvbrowser", "BROWSER"))
        assertEquals("Файлы", Labels.display("com.alphainventor.filemanager", "Файловый менеджер +"))
        assertEquals("HDrezka", Labels.display("to.pda.falcofemoralis.hdrezkaapp", "HDrezka Mod"))
    }

    @Test
    fun keepsOtherNamesTrimmed() {
        assertEquals("Okko", Labels.display("ru.more.play", "Okko"))
        assertEquals("VLC", Labels.display("x.y", " VLC "))
    }

    @Test
    fun blankNameFallsBackToPackage() {
        assertEquals("a.b", Labels.display("a.b", ""))
    }
}
