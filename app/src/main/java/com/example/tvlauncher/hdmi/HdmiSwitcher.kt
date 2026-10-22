package com.example.tvlauncher.hdmi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.widget.Toast

/** Переключает телевизор на HDMI 1 через приложение ТВ (у TCL — com.tcl.tv/.TVActivity). */
class HdmiSwitcher(private val context: Context) {

    fun switchToHdmi1(): Boolean {
        val manager = context.getSystemService(TvInputManager::class.java)
        val inputs = manager?.tvInputList.orEmpty().map {
            InputDesc(it.id, it.type == TvInputInfo.TYPE_HDMI, it.loadLabel(context)?.toString())
        }
        val uri = TvContract.buildChannelUriForPassthroughInput(HdmiPicker.pickHdmi1(inputs))
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            fail()
        } catch (e: SecurityException) {
            fail()
        }
    }

    private fun fail(): Boolean {
        Toast.makeText(context, "Не удалось переключиться на HDMI 1", Toast.LENGTH_SHORT).show()
        return false
    }
}
