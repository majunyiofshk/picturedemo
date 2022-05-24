package com.ma.pictureeditdemo.txt

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText

class MonitoredEditText @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : AppCompatEditText(context, attrs, defStyleAttr) {
    
    private var mListener: (() -> Unit)? = null
    
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == 1) {
            super.onKeyPreIme(keyCode, event)
            mListener?.invoke()
            return false
        }
        return super.onKeyPreIme(keyCode, event)
    }
    
    fun setonKeyBoardHideListener(listener: (() -> Unit)?){
        mListener = listener
    }
}