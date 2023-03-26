package com.android.example.cameraxbasic.camera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.fragments.GalleryFragment

class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val galleryFragment = GalleryFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.galleryFragmentContainer, galleryFragment, "")
        transaction.commitAllowingStateLoss()
    }
}