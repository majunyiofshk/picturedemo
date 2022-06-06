package com.ma.pictureeditdemo.surface

import android.content.Context
import com.ma.pictureeditdemo.surface.ShaderHelper.compileVertexShader
import com.ma.pictureeditdemo.surface.ShaderHelper.compileFragmentShader
import com.ma.pictureeditdemo.surface.ShaderHelper.linkProgram
import android.opengl.GLSurfaceView
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig

/**
 * 点
 */
class PointRender : GLSurfaceView.Renderer {
    private val mVertices: FloatBuffer = ByteBuffer.allocateDirect(POINT_DATA.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(POINT_DATA)
                position(0)
            }
    
    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        //1.编译顶点着色器
        val vertexShader = compileVertexShader(VERTEX_SHADER)
        
        //2.编译片段着色器
        val fragmentShader = compileFragmentShader(FRAGMENT_SHADER)
        
        //3.将顶点着色器、片段着色器进行链接，组装成一个OpenGL程序
        val program = linkProgram(vertexShader, fragmentShader)
        
        //4.通知OpenGL开始使用该程序
        GLES20.glUseProgram(program)
        
        //5.关联顶点坐标属性和缓存数据(告诉OpenGL该如何解析顶点数据)
        GLES20.glVertexAttribPointer(0, POINT_DATA.size, GLES20.GL_FLOAT, false, 0, mVertices)
        
        //6.通知GL程序使用指定的顶点属性索引
        GLES20.glEnableVertexAttribArray(0)
    }
    
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
    
    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
    }
    
    companion object {
        private const val BYTES_PER_FLOAT = 4
        
        private val POINT_DATA = floatArrayOf(0.0f, 0.0f, 0.0f)
        
        /**
         * 顶点着色器
         */
        private const val VERTEX_SHADER = "attribute vec3 position;\n" +
                "void main()\n" +
                "{\n" +
                // gl_Position：GL中默认定义的输出变量，决定了当前顶点的最终位置
                "    gl_Position = vec4(position.x, position.y, position.z, 1.0);\n" +
                // gl_PointSize：GL中默认定义的输出变量，决定了当前顶点的大小
                "    gl_PointSize = 40.0;\n" +
                "}";
        
        /**
         * 片段着色器
         */
        private const val FRAGMENT_SHADER = "precision mediump float;\n" +
                "uniform mediump vec4 u_Color;\n" +
                "void main()\n" +
                "{\n" +
                // gl_FragColor：GL中默认定义的输出变量，决定了当前片段的最终颜色
                "    gl_FragColor = vec4(1.0, 0.5, 0.2, 1.0);\n" +
                "}";
    }
}