package com.android.example.cameraxbasic.camera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.fragments.GalleryFragment
import com.android.example.cameraxbasic.utils.MEDIA_LIST_KEY

class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val list = intent.getStringArrayListExtra(MEDIA_LIST_KEY)
        val galleryFragment = GalleryFragment()
        val bundle = Bundle()
        bundle.putStringArrayList(MEDIA_LIST_KEY, list)

        galleryFragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.galleryFragmentContainer, galleryFragment, "")
        transaction.commitAllowingStateLoss()
    }
}