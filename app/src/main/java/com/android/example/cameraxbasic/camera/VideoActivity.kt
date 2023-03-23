package com.android.example.cameraxbasic.camera

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.video.VideoViewerFragment

class VideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val uri: Uri = intent.getParcelableExtra<Uri>("video_uri") as Uri
        val videoViewerFragment = VideoViewerFragment.newInstance(uri)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.galleryFragmentContainer, videoViewerFragment, "")
        transaction.commitAllowingStateLoss()

    }

}