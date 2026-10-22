package com.example.tvlauncher.hdmi

import org.junit.Assert.assertEquals
import org.junit.Test

class HdmiPickerTest {

    private val prefix = "com.tcl.tvinput/.passthroughinput.TvPassThroughService/"

    // Входы из `dumpsys tv_input` телевизора: AV и три HDMI (порты 1, 2, 3 = HW…4128, …4384, …4640).
    private fun inputs(hdmi2Label: String? = null) = listOf(
        InputDesc(prefix + "HW1413743104", isHdmi = false, label = null),
        InputDesc(prefix + "HW1413744640", isHdmi = true, label = null),
        InputDesc(prefix + "HW1413744128", isHdmi = true, label = null),
        InputDesc(prefix + "HW1413744384", isHdmi = true, label = hdmi2Label),
    )

    @Test
    fun picksLowestHdmiWhenNoLabels() {
        assertEquals(prefix + "HW1413744128", HdmiPicker.pickHdmi1(inputs()))
    }

    @Test
    fun prefersExplicitHdmi1Label() {
        assertEquals(prefix + "HW1413744384", HdmiPicker.pickHdmi1(inputs(hdmi2Label = "HDMI 1")))
    }

    @Test
    fun fallsBackWhenNoHdmi() {
        assertEquals(HdmiPicker.FALLBACK_HDMI1, HdmiPicker.pickHdmi1(emptyList()))
    }
}
