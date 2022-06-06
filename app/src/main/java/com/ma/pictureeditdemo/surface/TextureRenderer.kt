package com.ma.pictureeditdemo.surface

import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.ma.pictureeditdemo.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @Description:
 * @Author: JunYi.Ma
 * @Date: 2022/5/30 0030 9:13
 * @Email:  junyi.ma@upuphone.com
 */
class TextureRenderer constructor(val imageSurfaceView: ImageSurfaceView) : GLSurfaceView.Renderer {
    private val mTexture = IntArray(1)
    private var mProgramId = 0
    private var mProjectionMatrix = FloatArray(16)
    private var mViewMatrix  = FloatArray(16)
    private var mModelMatrix  = FloatArray(16)
    private var mMvpMatrix = FloatArray(16)
    
    private var mVerticesHandle = 0
    private var mTextureCoordinateHandle = 0
    private var mTextureHandle = 0
    private var mBaseMatrixHandle = 0
    
    @Volatile
    var translateX = 0f
    private var baseTranslateX = 0f
    private var translateY = 0f
    private var baseTranslateY = 226.5f / 2340f * 2
    private var scaleX = 1f
    private var baseScaleX = 0.8666667f
    private var scaleY = 1f
    private var baseScaleY = 0.53333336f
    
    private val mVertices: FloatBuffer = ByteBuffer.allocateDirect(CUBE.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(CUBE)
                position(0)
            }
    private val mTextureBuffer: FloatBuffer = ByteBuffer.allocateDirect(TEXTURE.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(TEXTURE)
                position(0)
            }
    private val mIndices: IntBuffer = ByteBuffer.allocateDirect(INDICES.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer().apply {
                put(INDICES)
                position(0)
            }
    
    init {
        imageSurfaceView.addOnTransformationListener(object : TouchController.OnTransformationListener {
            override fun onMove(distanceX: Float, distanceY: Float) {
                translateX += distanceX
                translateY += distanceY
                Log.e("TextureRenderer", "translateX = $translateX, translateY = $translateY")
                imageSurfaceView.requestRender()
            }
    
            override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
                scaleX *= scaleFactor
                scaleY *= scaleFactor
                imageSurfaceView.requestRender()
            }
    
            override fun onClick() {
                translateX = 72f / imageSurfaceView.width * 2
                imageSurfaceView.requestRender()
                // ValueAnimator.ofFloat()
            }
        })
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        loadProgram()
        findHandleByString() // 初始化 shader 变量
        
        // 输入
        GLES20.glVertexAttribPointer(mVerticesHandle, 3, GLES20.GL_FLOAT, false, 12, mVertices)
        GLES20.glEnableVertexAttribArray(mVerticesHandle)
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 8, mTextureBuffer)
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)
        loadTexture()
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        // val bWidth = 936f // 通过 matrix.setRectToRect 后的图片宽
        // val bHeight = 1248f // 通过 matrix.setRectToRect 后的图片高
        // val xScale = bWidth / width
        // val yScale = bHeight / height
        // Log.e("onSurfaceChanged", "xScale = $xScale, yScale = $yScale, width = $width, height = $height")
        val ratio = width.toFloat() / height
        // TODO:如何设置 mViewMatrix 和 mProjectionMatrix ,让图片落在裁剪框内
        Matrix.setIdentityM(mViewMatrix, 0)
        Matrix.setIdentityM(mProjectionMatrix, 0)
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.frustumM(mProjectionMatrix, 0, -1248f / 936f * ratio, 1248f / 936 * ratio, -1f, 1f, 1f, 10f)
        // Matrix.orthoM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        Log.e("onDrawFrame", "onDrawFrame")
        // Matrix.setIdentityM(mBaseMatrix, 0)
        // Matrix.translateM(mBaseMatrix, 0, 0.0f, 226.5f / 2340f * 2, 0f)
        // Matrix.scaleM(mBaseMatrix, 0, 0.8666667f, 0.53333336f, 1f)
        //
        // Matrix.setIdentityM(mMvpMatrix, 0)
        // Matrix.translateM(mMvpMatrix, 0, translateX, translateY, 0f)
        // Matrix.scaleM(mMvpMatrix, 0, scaleX, scaleY, 1f)
        // Matrix.rotateM(mMvpMatrix, 0, 90f, 0f, 0f, -1f)
        // Matrix.multiplyMM(mMvpMatrix, 0, mMvpMatrix, 0, mBaseMatrix, 0)
        // Matrix.setIdentityM(mMvpMatrix, 0)
        // Matrix.translateM(mMvpMatrix, 0, baseTranslateX + translateX, baseTranslateY + translateY, 0f)
        // Matrix.rotateM(mMvpMatrix, 0, 45f, 0f, 0f, 1f)
        // Matrix.scaleM(mMvpMatrix, 0, baseScaleX * scaleX, baseScaleY * scaleY, 1f)
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.setIdentityM(mMvpMatrix, 0)
        // Matrix.translateM(mModelMatrix, 0, 0.5f, 0.5f, 0f)
        // Matrix.scaleM(mModelMatrix, 0, 0.5f, 0.5f, 1.0f)
        // Matrix.rotateM(mModelMatrix, 0, 45f, 0f, 0f, 1f)
        Matrix.multiplyMM(mMvpMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMvpMatrix, 0, mProjectionMatrix, 0, mMvpMatrix, 0)
        
        val matrixHandle = GLES20.glGetUniformLocation(mProgramId, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mMvpMatrix, 0)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, INDICES.size, GLES20.GL_UNSIGNED_INT, mIndices)
    }
    
    private  fun getIdentity(): FloatArray {
        return floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
        )
    }
    
    /**
     * 编译 shader 程序并使用
     * */
    private fun loadProgram() {
        val vertexShader = ShaderHelper.compileVertexShader(VERTEX_SHADER)
        val fragmentShader = ShaderHelper.compileFragmentShader(FRAGMENT_SHADER)
        mProgramId = ShaderHelper.linkProgram(vertexShader, fragmentShader)
        GLES20.glUseProgram(mProgramId)
    }
    
    /**
     * 找出 shader 程序中的变量句柄
     * */
    private fun findHandleByString() {
        mVerticesHandle = GLES20.glGetAttribLocation(mProgramId, "position")
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramId, "inTextureCoordinate")
        mTextureHandle = GLES20.glGetUniformLocation(mProgramId, "inputImageTexture")
        mBaseMatrixHandle = GLES20.glGetUniformLocation(mProgramId, "uMVPMatrix")
    }
    
    /**
     * 创建并加载纹理
     * */
    private fun loadTexture() {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        
        val options = BitmapFactory.Options()
        options.inScaled = false
        val bitmap = BitmapFactory.decodeResource(imageSurfaceView.context.resources, R.drawable.tt, options)
        Log.e("onSurfaceCreated", "width = ${bitmap.width}, height = ${bitmap.height}")
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glUniform1i(mTextureHandle, 0)
        
        bitmap.recycle()
    }
    
    companion object {
        private const val BYTES_PER_FLOAT = 4
        
        // 顶点: 按照右上,右下,左下,左上
        private val CUBE = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f)
        
        // 纹理
        private val TEXTURE = floatArrayOf(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
        
        //
        private val INDICES = intArrayOf(0, 1, 3, 1, 2, 3)
        
        /**
         * 顶点着色器
         */
        private const val VERTEX_SHADER = "attribute vec3 position;\n" +
                "attribute vec2 inTextureCoordinate;\n" + // 传入的纹理坐标
                "varying vec2 textureCoordinate;\n" + // 输出的纹理坐标
                "uniform mat4 uMVPMatrix;\n" +
                "void main()\n" +
                "{\n" +
                // gl_Position：GL中默认定义的输出变量，决定了当前顶点的最终位置
                "    gl_Position = uMVPMatrix * vec4(position.x, position.y, position.z, 1.0);\n" +
                "    textureCoordinate = vec2(inTextureCoordinate.x, 1.0 - inTextureCoordinate.y);\n" +
                "}";
        
        /**
         * 片段着色器
         */
        private const val FRAGMENT_SHADER = "precision mediump float;\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "void main()\n" +
                "{\n" +
                // gl_FragColor：GL中默认定义的输出变量，决定了当前片段的最终颜色
                "    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "}";
    }
}