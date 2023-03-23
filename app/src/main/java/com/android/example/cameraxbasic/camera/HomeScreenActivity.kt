package com.android.example.cameraxbasic.camera

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.databinding.ActivityGalleryBinding
import com.android.example.cameraxbasic.databinding.ActivityHomeScreenBinding

class HomeScreenActivity : AppCompatActivity() {

    lateinit var binding:ActivityHomeScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initClickListener()

    }

    private fun initClickListener() {
        binding.imageMenuItem1.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
        binding.imageMenuItem2.setOnClickListener {
            val intent = Intent(this, JsmGalleryActivity::class.java)
            startActivity(intent)
        }
    }

}