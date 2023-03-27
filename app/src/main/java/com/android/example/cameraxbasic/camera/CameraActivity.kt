package com.android.example.cameraxbasic.camera

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.ui.AppBarConfiguration
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.databinding.ActivityCameraBinding
import com.android.example.cameraxbasic.fragments.CameraFragment
import com.android.example.cameraxbasic.video.CaptureFragment
import com.google.android.material.tabs.TabLayout
import kotlin.math.roundToInt

class CameraActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityCameraBinding
    var permission = arrayOf("android.permission.READ_EXTERNAL_STORAGE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestPermissions(permission, 80)
        }
       // setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_camera)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        showPhotoFragment(CameraFragment())
        // val (videoFragment, transaction) = showVideoFragment(fragment)

        val tabLayout = binding.contentCamera.tabLayout
        tabLayout.addTab(tabLayout.newTab().setText("Photo"))
        tabLayout.addTab(tabLayout.newTab().setText("Video"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (0 == tab?.position) {
                    showPhotoFragment(CameraFragment())
                } else {
                    showVideoFragment(CaptureFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })

        binding.contentCamera.cameraZoomText0.setOnClickListener {
           // val fragment = supportFragmentManager.fragments
            val cameraFragment = supportFragmentManager.findFragmentByTag("CameraFragment")
            if (cameraFragment != null && cameraFragment is CameraFragment) {
                cameraFragment.setCameraZoomLevels(0.0f)
            }
            val videoFragment = supportFragmentManager.findFragmentByTag("CaptureFragment")
            if (videoFragment != null && videoFragment is CaptureFragment) {
                videoFragment.setLinearZoom(0.0f)
            }
        }
        binding.contentCamera.cameraZoomText05.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentByTag("CameraFragment")
            if (fragment != null && fragment is CameraFragment) {
                fragment.setCameraZoomLevels(0.7f)
            }
            val videoFragment = supportFragmentManager.findFragmentByTag("CaptureFragment")
            if (videoFragment != null && videoFragment is CaptureFragment) {
                videoFragment.setLinearZoom(0.7f)
            }
        }
        binding.contentCamera.cameraZoomText1.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentByTag("CameraFragment")
            if (fragment != null && fragment is CameraFragment) {
                fragment.setCameraZoomLevels(1.0f)
            }
            val videoFragment = supportFragmentManager.findFragmentByTag("CaptureFragment")
            if (videoFragment != null && videoFragment is CaptureFragment) {
                videoFragment.setLinearZoom(1.0f)
            }
        }

    }

    private fun showVideoFragment(fragment: CaptureFragment): Pair<CaptureFragment, FragmentTransaction> {
        val videoFragment = CaptureFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment, "CaptureFragment")
        transaction.commitAllowingStateLoss()
        return Pair(videoFragment, transaction)
    }

    private fun showPhotoFragment(fragment: CameraFragment): Pair<CameraFragment, FragmentTransaction> {
        val videoFragment = CameraFragment()
        val transaction1 = supportFragmentManager.beginTransaction()
        transaction1.replace(R.id.fragment_container, fragment, "CameraFragment")
        transaction1.commitAllowingStateLoss()
        return Pair(videoFragment, transaction1)
    }

    fun updateZoomText(zoomRatio: Float) {
        Log.d(CameraActivity::class.java.simpleName, "onScale: zoomRatio==$zoomRatio")
        val format = String.format("%.1f", zoomRatio)
        if ("0.9" == format || "1.0" == format) {
            binding.contentCamera.cameraZoomText05.visibility = View.VISIBLE
            binding.contentCamera.cameraZoomText1.visibility = View.VISIBLE
            binding.contentCamera.cameraZoomText0.text = "0x"
            binding.contentCamera.cameraZoomText05.text =  "1x"
            binding.contentCamera.cameraZoomText1.text =  "2x"
        }else {
            binding.contentCamera.cameraZoomText05.visibility = View.GONE
            binding.contentCamera.cameraZoomText1.visibility = View.GONE
            binding.contentCamera.cameraZoomText0.text = "${format}x"
        }

    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_camera)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}