package com.example.tvlauncher.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/** Плитка с пропорциями 16:9: высота считается от ширины колонки. */
class AspectFrameLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY))
    }
}
