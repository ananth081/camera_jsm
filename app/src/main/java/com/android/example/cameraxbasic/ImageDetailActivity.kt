package com.android.example.cameraxbasic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.example.cameraxbasic.databinding.ActivityImageDetailBinding
import com.android.example.cameraxbasic.fragments.PdftronPhotoFragment
import com.android.example.cameraxbasic.utils.IMAGE_URI_STRING_KEY

class ImageDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uri = intent.getStringExtra(IMAGE_URI_STRING_KEY) ?: ""
        val fragment = PdftronPhotoFragment.create(uri)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(binding.imageDetailContainer.id, fragment)
        transaction.commitAllowingStateLoss()
    }
}