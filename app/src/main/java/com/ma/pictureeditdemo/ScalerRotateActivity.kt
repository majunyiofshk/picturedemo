package com.ma.pictureeditdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ma.pictureeditdemo.databinding.ActivityScalerBinding

class ScalerRotateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScalerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScalerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnForward.setOnClickListener {
            binding.text.forward()
        }
    
        binding.btnBack.setOnClickListener {
            binding.text.back()
        }
    }

    
}