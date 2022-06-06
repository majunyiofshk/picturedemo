package com.ma.pictureeditdemo.touch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.ma.pictureeditdemo.extensions.dp
import com.ma.pictureeditdemo.extensions.sp

/**
 * @Description:
 * @Author: junyi.ma
 * @Date: 2022/5/20 0020-16:41
 * @Email:  junyi.ma@upuphone.com
 */
class BottomView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    
    private var mTextPaint = TextPaint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        isDither = true
        color = Color.BLACK
        textSize = 18.sp()
    }
    private val mStaticLayout: StaticLayout
    
    init {
        mStaticLayout = StaticLayout("石岛红合适的话松岛枫", mTextPaint, 200, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        Log.e("TextBox", "width = ${mStaticLayout.width}, height = ${mStaticLayout.height}")
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val boolean = super.onTouchEvent(event)
        Log.e("BottomView", "action = ${event?.action}, boolean = $boolean")
        return boolean
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mStaticLayout.draw(canvas)
    }
}