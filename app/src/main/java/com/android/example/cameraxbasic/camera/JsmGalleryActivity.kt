package com.android.example.cameraxbasic.camera

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.adapter.ViewPagerAdapter
import com.android.example.cameraxbasic.databinding.ActivityJsmGalleryBinding
import com.android.example.cameraxbasic.fragments.GalleryViewDailogFragment
import com.android.example.cameraxbasic.fragments.PublishedFragment
import com.android.example.cameraxbasic.viewmodels.CaptureViewModel
import com.android.example.cameraxbasic.viewmodels.DRAFT
import com.android.example.cameraxbasic.viewmodels.GalleryViewModel
import com.android.example.cameraxbasic.viewmodels.PUBLISHED
import com.google.android.material.tabs.TabLayoutMediator

class JsmGalleryActivity : AppCompatActivity(), GalleryViewDailogFragment.selectedView {
    private lateinit var binding: ActivityJsmGalleryBinding
    private var adapter: ViewPagerAdapter? = null
    private lateinit var mViewPager: ViewPager2
    var source = false


    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityJsmGalleryBinding.inflate(layoutInflater)
        source = intent.getBooleanExtra("IS_PUBLISHED_SCREEN", true)
        binding.toolbar.title = "Media"
        binding.toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_back_arrow)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        super.onCreate(savedInstanceState)
        initialiseView(binding.viewPager)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.menu.setOnClickListener {
            var fragment = GalleryViewDailogFragment()
            fragment.show(supportFragmentManager, "GalleryViewDailogFragment")
            fragment.initialiseObject(this)

        }

    }


    private fun initialiseView(viewPager: ViewPager2) {
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        mViewPager = viewPager


        adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        mViewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Published" else "Needs Review"
        }.attach()

        if (source) {
            mViewPager.currentItem = 0
            binding.tabLayout.getTabAt(0)?.select()

        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                mViewPager.currentItem = 1
                binding.tabLayout.getTabAt(1)?.select()
                binding.tabLayout.setScrollPosition(1, 0f, true)
            }, 500L)


        }
    }

    override fun getSelectedView(selectedView: String) {
//        val fragment = adapter?.fragments?.get(0)
//        (fragment as PublishedFragment).onMediaViewSelected(selectedView)
//        adapter?.refreshFragment(0,PublishedFragment())

    }

    fun updateText(value: Int) {
        val txt = "Needs Review ($value)"
        binding.tabLayout.getTabAt(1)?.text = txt
    }
}