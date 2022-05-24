package com.ma.pictureeditdemo.txt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

class TextImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    
    private var textBoxGroup: TextBoxGroup = TextBoxGroup(this)
    
    init {
        super.setScaleType(ScaleType.FIT_CENTER)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        textBoxGroup.onSizeChanged(w, h)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        textBoxGroup.draw(canvas)
    }
    
    fun forward(){
        textBoxGroup.restore(textBoxGroup.mCaretaker.getNextMemo())
    }
    
    fun back(){
        textBoxGroup.restore(textBoxGroup.mCaretaker.getPrevMemo())
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handle = textBoxGroup.onTouchEvent(event)
        return if (handle) {
            true
        }else{
            super.onTouchEvent(event)
        }
    }
}