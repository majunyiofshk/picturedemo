package com.ma.pictureeditdemo.surface

import android.opengl.GLES20
import android.util.Log

/**
 * @Description: 着色器帮助类
 * @Author: JunYi.Ma
 * @Date: 2022/5/27 27 15:41
 * @Email:  junyi.ma@upuphone.com
 */
object ShaderHelper {
    private const val TAG = "ShaderHelper"
    
    /**
     * 编译顶点着色器
     *
     * @param shaderCode 编译代码
     * @return 着色器对象ID, 在OpenGL中，都是通过整型值去作为OpenGL对象的引用
     */
    fun compileVertexShader(shaderCode: String): Int {
        return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode)
    }
    
    /**
     * 编译片段着色器
     *
     * @param shaderCode 编译代码
     * @return 着色器对象ID, 在OpenGL中，都是通过整型值去作为OpenGL对象的引用
     */
    fun compileFragmentShader(shaderCode: String): Int {
        return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode)
    }
    
    private fun compileShader(type: Int, shaderCode: String): Int {
        // 1.创建一个新的着色器对象
        val shaderId = GLES20.glCreateShader(type)
        
        // 2.获取创建状态
        if (shaderId == 0) {
            return 0
        }
        
        // 3. 将着色器代码上传到着色器对象中
        GLES20.glShaderSource(shaderId, shaderCode)
        
        // 4.编译着色器对象
        GLES20.glCompileShader(shaderId)
        
        // 5.获取编译状态：OpenGL将想要获取的值放入长度为1的数组的首位
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        Log.e(TAG, "Results of compiling source: \n $shaderCode \n: ${GLES20.glGetShaderInfoLog(shaderId)}")
        
        // 6.验证编译状态
       if (compileStatus[0] == 0) {
           GLES20.glDeleteShader(shaderId)
           Log.e(TAG, "Compilation of shader failed.")
           return 0
       }
        // 7.返回着色器对象：成功，非0
        return shaderId
    }
    
    /**
     * 创建OpenGL程序：通过链接顶点着色器、片段着色器
     *
     * @param vertexShaderId   顶点着色器ID
     * @param fragmentShaderId 片段着色器ID
     * @return OpenGL程序ID
     */
    fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        // 1.创建一个OpenGL程序对象
        val programId = GLES20.glCreateProgram()
        
        // 2.获取创建状态
        if (programId == 0) {
            Log.e(TAG, "Could not create new program")
            return 0
        }
        
        // 3.1 将顶点着色器依附到OpenGL程序对象
        GLES20.glAttachShader(programId, vertexShaderId)
        // 3.2 将片段着色器依附到OpenGL程序对象
        GLES20.glAttachShader(programId, fragmentShaderId)
        
        // 4.将两个着色器链接到OpenGL程序对象
        GLES20.glLinkProgram(programId)
        
        // 5.获取链接状态：OpenGL将想要获取的值放入长度为1的数组的首位
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        
        // 6.验证链接状态
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(programId)
            Log.e(TAG, "Linking of program failed.")
            return 0
        }
        // 7.返回程序对象：成功，非0
        return programId
    }
}