package com.example.tvlauncher.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.tvlauncher.R

/** Маленькое меню у плитки по долгому нажатию OK. */
class TileMenu(private val anchor: View, private val items: List<Pair<String, () -> Unit>>) {

    private var popup: PopupWindow? = null

    fun dismiss() {
        popup?.dismiss()
        popup = null
    }

    fun show() {
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_panel)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val popup = PopupWindow(
            container,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        )
        popup.elevation = dp(16).toFloat()
        this.popup = popup

        for ((title, action) in items) {
            container.addView(TextView(context).apply {
                text = title
                textSize = 13f
                setTextColor(context.getColorStateList(R.color.menu_item_text))
                setBackgroundResource(R.drawable.bg_menu_item)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                minWidth = dp(180)
                isFocusable = true
                setOnClickListener {
                    popup.dismiss()
                    action()
                }
            })
        }

        container.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val location = IntArray(2)
        anchor.getLocationInWindow(location)
        val gap = dp(8)
        val fitsBelow = location[1] + anchor.height + gap + container.measuredHeight <= anchor.rootView.height
        val y = if (fitsBelow) location[1] + anchor.height + gap else location[1] - gap - container.measuredHeight
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], y)
        container.getChildAt(0)?.requestFocus()
    }
}
