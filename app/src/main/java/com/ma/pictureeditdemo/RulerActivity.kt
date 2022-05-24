package com.ma.pictureeditdemo

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.ma.pictureeditdemo.databinding.ActivityRulerBinding
import com.ma.pictureeditdemo.temp.Crop
import java.io.File

class RulerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRulerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRulerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.rulerView.setScaleLimit(0, 100)
        binding.rulerView.setCenterScale(50)
        binding.rulerView.setScaleChangedListener {
            binding.tvMain.text = it.toString()
        }
        binding.rulerView.setScaleUpListener {
            Log.e("RulerActivity", "抬手: it = $it")
        }

        binding.tvMain.setOnClickListener {
            // Crop.pickImage(this)
            binding.rulerView.setCenterScale(50)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(data?.data);
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
        }
    }

    private fun beginCrop(source: Uri?) {
        val destination: Uri = Uri.fromFile(File(cacheDir, "cropped"))
        Crop.of(source, destination).asSquare().start(this)
    }

    private fun handleCrop(resultCode: Int, result: Intent?) {
        if (resultCode == RESULT_OK) {
//            resultView.setImageURI(Crop.getOutput(result))
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).message, Toast.LENGTH_SHORT).show()
        }
    }
}