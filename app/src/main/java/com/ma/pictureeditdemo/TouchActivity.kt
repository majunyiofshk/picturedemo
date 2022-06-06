package com.ma.pictureeditdemo

import android.animation.ValueAnimator
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.ma.pictureeditdemo.surface.CropFrame
import com.ma.pictureeditdemo.surface.ImageSurfaceView

class TouchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).hideBar((BarHide.FLAG_HIDE_STATUS_BAR)).init()
        setContentView(R.layout.activity_touch)
        // val matrix = Matrix()
        // val src = RectF(0f, 0f, 1920f, 2560f)
        // val dst = CropFrame.origin
        // matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER)
        // Log.e("TouchActivity", "dst = $dst")
        // matrix.mapRect(src)
        // Log.e("TouchActivity", "src: width = ${src.width()}, height = ${src.height()}, s = $src")
        // val rectF = CropFrame.getCropFrameRectFByScale(1f)
        // Log.e("TouchActivity", "rectF: width = ${rectF.width()}, height = ${rectF.height()}, s = $rectF")
        val cropFrame = findViewById<CropFrame>(R.id.cropFrame)
        val btnFree = findViewById<Button>(R.id.btn_free)
        val btnOrigin = findViewById<Button>(R.id.btn_origin)
        val btn1 = findViewById<Button>(R.id.btn_1_1)
        val btn16 = findViewById<Button>(R.id.btn_16_9)
        val btn9 = findViewById<Button>(R.id.btn_9_16)
        val btn3 = findViewById<Button>(R.id.btn_3_2)
        val btn2 = findViewById<Button>(R.id.btn_2_3)
        btnFree.setOnClickListener {
            cropFrame.changeCropFrameToFreedom()
        }
        btnOrigin.setOnClickListener {
            cropFrame.changeCropFrameToOrigin()
        }
        btn1.setOnClickListener {
            cropFrame.changeCropFrameByRatio(1f)
        }
        btn16.setOnClickListener {
            cropFrame.changeCropFrameByRatio(16f / 9f)
        }
        btn9.setOnClickListener {
            cropFrame.changeCropFrameByRatio(9f / 16f)
        }
        btn3.setOnClickListener {
            cropFrame.changeCropFrameByRatio(3f / 2f)
        }
        btn2.setOnClickListener {
            cropFrame.changeCropFrameByRatio(2f / 3f)
        }
        
        val image = findViewById<ImageSurfaceView>(R.id.image)
        // 验证属性动画, GlThread 和 主线程 UI 刷新是否同步
        // val textView = findViewById<TextView>(R.id.tv_test)
        // textView.setOnClickListener {
        //     val anim = ValueAnimator.ofFloat(0f, 72f)
        //     anim.addUpdateListener {
        //         val value = it.animatedValue as Float
        //         textView.translationX = value
        //         image.setTX(value)
        //     }
        //     anim.duration = 2000
        //     anim.start()
        // }
    }
}