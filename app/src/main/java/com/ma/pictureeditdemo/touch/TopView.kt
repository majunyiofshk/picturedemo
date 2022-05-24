package com.ma.pictureeditdemo.touch

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

/**
 * @Description:
 * @Author: junyi.ma
 * @Date: 2022/5/20 0020-16:41
 * @Email:  junyi.ma@upuphone.com
 */
class TopView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    
    init {
        isClickable = true
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val boolean = super.onTouchEvent(event)
        Log.e("TopView", "action = ${event?.action}, boolean = $boolean")
        return boolean
    }
}