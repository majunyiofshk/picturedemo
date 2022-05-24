package com.ma.pictureeditdemo.surface

import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.IntBuffer

/**
 * @Description:
 * @Author: JunYi.Ma
 * @Date: 2022/5/24 0024 9:54
 * @Email:  junyi.ma@upuphone.com
 */
object OpenGlUtils {
    private const val NO_TEXTURE = -1
    
    fun deleteTexture(textureID: Int){
        GLES20.glDeleteTextures(1, intArrayOf(textureID), 0)
    }
    
    fun loadTexture(bitmap: Bitmap, usedTexId: Int): Int {
        return loadTexture(bitmap, usedTexId, true)
    }
    
    fun loadTexture(bitmap: Bitmap, usedTexId: Int, recycle: Boolean): Int {
        val textures = intArrayOf(-1)
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
            //纹理也有坐标系，称UV坐标，或者ST坐标
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT.toFloat()) // S轴的拉伸方式为重复，决定采样值的坐标超出图片范围时的采样方式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT.toFloat()) // T轴的拉伸方式为重复
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId)
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap)
            textures[0] = usedTexId
        }
        if (recycle) {
            bitmap.recycle()
        }
        return textures[0]
    }
    
    fun loadTexture(data: IntBuffer?, rect: Rect, usedTexId: Int): Int {
        val textures = IntArray(1)
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, rect.width(), rect.height(),
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId)
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, rect.width(),
                    rect.height(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data)
            textures[0] = usedTexId
        }
        return textures[0]
    }
}