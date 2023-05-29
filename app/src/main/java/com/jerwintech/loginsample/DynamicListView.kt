package com.jerwintech.loginsample

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

class DynamicListView(context: Context, attrs: AttributeSet) : ListView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSpec = if (adapter != null && adapter.count > 0) {
            MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE shr 2, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}