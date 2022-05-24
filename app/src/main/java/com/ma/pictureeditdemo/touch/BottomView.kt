package com.ma.pictureeditdemo.touch

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * @Description:
 * @Author: junyi.ma
 * @Date: 2022/5/20 0020-16:41
 * @Email:  junyi.ma@upuphone.com
 */
class BottomView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {
    
    init {
        // isClickable = true
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val boolean = super.onTouchEvent(event)
        Log.e("BottomView", "action = ${event?.action}, boolean = $boolean")
        return boolean
    }
    
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    
    }
}