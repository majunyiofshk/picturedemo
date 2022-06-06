package com.ma.pictureeditdemo.surface

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.Uri
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @Description:
 * @Author: JunYi.Ma
 * @Date: 2022/5/23 15:29
 * @Email:  junyi.ma@upuphone.com
 */
class ImageSurfaceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {
    
    private val mTouchController = TouchController(this)
    private val mTextureRenderer = TextureRenderer(this)
    
    init {
        setEGLContextClientVersion(2)
        setRenderer(mTextureRenderer)
        // holder.setFormat(PixelFormat.TRANSLUCENT)
        renderMode = RENDERMODE_WHEN_DIRTY
        requestRender()
    }
    
    /**
    * 添加手势变换监听
    * */
    fun addOnTransformationListener(listener: TouchController.OnTransformationListener) {
        mTouchController.addOnTransformationListener(listener)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mTouchController.onTouchEvent(event)
    }
    
    fun setTX(value: Float) {
        mTextureRenderer.translateX = value / width * 2
        requestRender()
    }
}