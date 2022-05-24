package com.ma.pictureeditdemo

import android.content.Intent
import android.os.Bundle
import android.text.method.Touch
import androidx.appcompat.app.AppCompatActivity
import com.ma.pictureeditdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var  binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRuler.setOnClickListener {
            startActivity(Intent(this, RulerActivity::class.java))
        }

        binding.btnCrop.setOnClickListener {
            startActivity(Intent(this, CropActivity::class.java))
        }
        binding.btnScaler.setOnClickListener {
            startActivity(Intent(this, ScalerRotateActivity::class.java))
        }
    
        binding.btnTouch.setOnClickListener {
            startActivity(Intent(this, TouchActivity::class.java))
        }
    }
}