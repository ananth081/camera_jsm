package com.android.example.cameraxbasic.camera

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        supportActionBar?.hide()
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                packageName
            ) ==
                    AppOpsManager.MODE_ALLOWED
        } else {
            false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (status) {
                enterPIPMode()
            } else {
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Feature Not Supported!!", Toast.LENGTH_SHORT).show()

        }

    }

    @Suppress("DEPRECATION")
    fun enterPIPMode() {
        val fragment = supportFragmentManager.findFragmentById(R.id.galleryFragmentContainer)
        if (fragment is VideoViewerFragment)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && packageManager
                    .hasSystemFeature(
                        PackageManager.FEATURE_PICTURE_IN_PICTURE
                    )
            ) {
                fragment.binding.videoViewer.useController = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = PictureInPictureParams.Builder()
                    this.enterPictureInPictureMode(params.build())
                } else {
                    this.enterPictureInPictureMode()
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val fragment = supportFragmentManager.findFragmentById(R.id.galleryFragmentContainer)
//        if (fragment is VideoViewerFragment) {
//            fragment.binding.videoViewer.useController = !isInPictureInPictureMode
//            if (isInPictureInPictureMode) {
//                fragment.binding.toolbar.visibility = View.GONE
//            }
//            else
//                fragment.binding.toolbar.visibility = View.VISIBLE
//        }

    }




}