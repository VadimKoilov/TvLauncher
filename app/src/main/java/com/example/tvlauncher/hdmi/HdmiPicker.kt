package com.example.tvlauncher.hdmi

data class InputDesc(val id: String, val isHdmi: Boolean, val label: String?)

/** Какой из входов телевизора — HDMI 1. */
object HdmiPicker {

    /** HDMI 1 этого телевизора по `dumpsys tv_input` — на случай, если список входов недоступен. */
    const val FALLBACK_HDMI1 = "com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744128"

    private val HW_NUMBER = Regex("HW(\\d+)$")

    fun pickHdmi1(inputs: List<InputDesc>): String {
        val hdmi = inputs.filter { it.isHdmi }
        hdmi.firstOrNull { it.label?.replace(" ", "")?.equals("hdmi1", ignoreCase = true) == true }
            ?.let { return it.id }
        // У TCL номера аппаратных входов растут вместе с номером порта.
        return hdmi.minByOrNull { HW_NUMBER.find(it.id)?.groupValues?.get(1)?.toLongOrNull() ?: Long.MAX_VALUE }
            ?.id
            ?: FALLBACK_HDMI1
    }
}
