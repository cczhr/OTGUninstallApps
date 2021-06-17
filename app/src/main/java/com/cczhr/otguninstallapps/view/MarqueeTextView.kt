package com.cczhr.otguninstallapps.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet

/**
 * @author cczhr
 * @description
 * @since 2021/1/26 09:43
 */
class MarqueeTextView :androidx.appcompat.widget.AppCompatTextView  {
    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)


    override fun isFocused(): Boolean {
        return true
    }
    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(true, direction, previouslyFocusedRect)
    }
}