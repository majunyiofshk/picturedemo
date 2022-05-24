package com.ma.pictureeditdemo

import android.graphics.Matrix
import android.graphics.RectF
import android.opengl.GLUtils
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.ma.pictureeditdemo.box.PhotoViewAttach
import com.ma.pictureeditdemo.databinding.ActivityCropBinding
import java.util.*

class CropActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCropBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnFree.setOnClickListener{
            binding.cropView.adjustToFreedom()
        }
        binding.btnOrigin.setOnClickListener{
            binding.cropView.adjustToOrigin()
        }
        binding.btn1.setOnClickListener{
            binding.cropView.adjustByScale(1f)
        }
        binding.btn16.setOnClickListener{
            binding.cropView.adjustByScale(16f / 9f)
        }
        binding.btn9.setOnClickListener{
            binding.cropView.mirror()
        }
        binding.btnRotate.setOnClickListener{
            binding.cropView.rotateByVertical()
        }
        binding.rulerView.setScaleChangedListener {
            binding.cropView.correctVertical(it)
        }
        binding.btnRotation.setOnClickListener{
            val rotation = binding.cropView.obtainRotation()
            Log.e("CropActivity", "rotation = $rotation");
        }
//        binding.cropView.setRotateType(PhotoViewAttach.RotateType.HORIZONTAL_POLY)
    
    }
}