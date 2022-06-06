package com.ma.pictureeditdemo.surface

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.ma.pictureeditdemo.photoview.CustomGestureDetector
import com.ma.pictureeditdemo.photoview.OnGestureListener

/**
 * @Description: 手势控制
 * @Author: JunYi.Ma
 * @Date: 2022/6/3 12:11
 * @Email:  junyi.ma@upuphone.com
 */
class TouchController constructor(private val view: View) {
    private val mListeners = mutableListOf<OnTransformationListener>()
    
    private val mScaleGestureDetector by lazy {
        CustomGestureDetector(view.context, onScaleGestureListener)
    }
    private val mGestureDetector by lazy {
        GestureDetector(view.context, onGestureListener)
    }
    
    private val onScaleGestureListener = object : OnGestureListener {
        override fun onDrag(dx: Float, dy: Float) {
            // do nothing
        }
    
        override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
            // do nothing
        }
    
        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
            mListeners.forEach {
                it.onScale(scaleFactor, focusX, focusY)
            }
        }
    }
    
    private val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }
    
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            // 点击事件
            mListeners.forEach {
                it.onClick()
            }
            return true
        }
    
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            val x = -distanceX / view.width * 2
            val y = distanceY / view.height * 2
            mListeners.forEach {
                it.onMove(x, y)
            }
            return true
        }
    }
    
    /**
    * 触摸事件
    * */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                // 图片归位
            }
        }
        mScaleGestureDetector.onTouchEvent(event)
        return mGestureDetector.onTouchEvent(event)
    }
    
    /**
    * 添加手势变换监听
    * */
    fun addOnTransformationListener(listener: OnTransformationListener) {
        mListeners.add(listener)
    }
    
    /**
    * 手势变换监听
    * */
    interface OnTransformationListener {
        /**
        * 移动
        * */
        fun onMove(distanceX: Float, distanceY: Float)
        
        /**
        * 缩放
        * */
        fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)
        
        fun onClick()
    }
}