package com.ma.pictureeditdemo.surface

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
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
) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer {
    
    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    
    }
    
    override fun onDrawFrame(gl: GL10?) {
    
    }
}