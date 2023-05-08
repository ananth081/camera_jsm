package com.android.example.cameraxbasic.camera

import android.app.Activity
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.DialogInterface
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.databinding.ActivityCameraBinding
import com.android.example.cameraxbasic.databinding.ActivityGalleryBinding
import com.android.example.cameraxbasic.databinding.ActivityVideoBinding
import com.android.example.cameraxbasic.fragments.GalleryFragment
import com.android.example.cameraxbasic.fragments.PdftronPhotoFragment
import com.android.example.cameraxbasic.utils.DELETED_LIST_INTENT_KEY
import com.android.example.cameraxbasic.utils.MEDIA_LIST_KEY
import com.android.example.cameraxbasic.utils.showImmersive
import com.android.example.cameraxbasic.video.VideoViewerFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.ArrayList

class VideoListActivity : AppCompatActivity() {
    private var mediaList: ArrayList<String>? = arrayListOf()
    lateinit var binding: ActivityVideoBinding
    private var deletedList: ArrayList<String> = arrayListOf()

    inner class MediaPagerAdapter(
        fm: FragmentManager,
        val uriList: ArrayList<String>
    ) :
        FragmentStateAdapter(fm, lifecycle) {
        override fun getItemCount(): Int = uriList.size
        override fun createFragment(position: Int): Fragment =
            VideoViewerFragment.newInstance(uriList[position].toUri())

        override fun getItemId(position: Int): Long {
            return uriList[position].hashCode().toLong()
        }

        fun getItemPosition(`object`: Any?): Int {
            return PagerAdapter.POSITION_NONE
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaList = intent.getStringArrayListExtra(MEDIA_LIST_KEY)
        Log.d("PRS", "list " + mediaList)
        binding = ActivityVideoBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding?.toolbar?.background =
            ColorDrawable(resources.getColor(R.color.colorPrimary))

        binding.videoViewPager.apply {
            offscreenPageLimit = 2
            adapter = mediaList?.let { MediaPagerAdapter(supportFragmentManager, it) }
        }

        TabLayoutMediator(
            binding.tabLayout,
            binding.videoViewPager
        ) { tab, position ->
            //Some implementation
            binding.toolbarText.text = "${position+1} of ${mediaList?.size}"
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.toolbarText.text =
                    "${tab?.position?.plus(1)} of ${mediaList?.size}"
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })

        binding.galleryBackButton.setOnClickListener {
            finish()
        }

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.actionRetake) {
                mediaList?.getOrNull(binding.videoViewPager.currentItem)
                    ?.let { mediaStoreFile ->
                        mediaStoreFile.let { it1 ->
                            this.contentResolver?.delete(
                                Uri.parse(it1),
                                null,
                                null
                            )
                        }
                        val intent = Intent()
                        intent.putExtra("source", "retake_picture")
                        intent.putExtra("file_uri", mediaStoreFile)
                        intent.putStringArrayListExtra(DELETED_LIST_INTENT_KEY, deletedList)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }

            } else if (it.itemId == R.id.actionDelete) {
                deleteSpecificImage()
            }
            true
        }

    }

    private fun deleteSpecificImage() {
        mediaList?.getOrNull(binding.videoViewPager.currentItem)
            ?.let { mediaStoreFile ->

                AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
                    .setTitle(getString(R.string.delete_title))
                    .setMessage(getString(R.string.delete_dialog_video))
                    .setIcon(android.R.drawable.ic_dialog_alert)

                    .setPositiveButton(android.R.string.ok,
                        object : DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                // Delete current photo
                                mediaStoreFile.let {
                                    applicationContext.contentResolver?.delete(
                                        Uri.parse(it),
                                        null,
                                        null
                                    )
                                }

                                deletedList.add(mediaStoreFile)
                                // Notify our view pager
                                mediaList?.remove(mediaStoreFile)
                                (binding.videoViewPager.adapter as MediaPagerAdapter).uriList.remove(
                                    mediaStoreFile
                                )
                                binding.videoViewPager.adapter?.notifyDataSetChanged()

                                // If all photos have been deleted, return to camera
                                if (mediaList!!.isEmpty()) {
                                    val intent = Intent()
                                    intent.putStringArrayListExtra(
                                        DELETED_LIST_INTENT_KEY,
                                        deletedList
                                    )
                                    setResult(Activity.RESULT_OK, intent)
                                    finish()
                                }
                            }

                        })

                    .setNegativeButton(android.R.string.cancel, null)
                    .create().showImmersive()
            }
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
//               // fragment.binding.toolbar.visibility = View.GONE
//            }
//            else
//              //  fragment.binding.toolbar.visibility = View.VISIBLE
//        }
        if (isInPictureInPictureMode) {
            binding.toolbar.visibility = View.GONE
            }
            else
            binding.toolbar.visibility = View.VISIBLE
        }
    }


