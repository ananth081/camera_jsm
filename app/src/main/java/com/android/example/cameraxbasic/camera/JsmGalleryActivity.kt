package com.android.example.cameraxbasic.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.android.example.cameraxbasic.adapter.ViewPagerAdapter
import com.android.example.cameraxbasic.databinding.ActivityJsmGalleryBinding
import com.android.example.cameraxbasic.fragments.NeedReviewFragment
import com.android.example.cameraxbasic.fragments.PublishedFragment
import com.google.android.material.tabs.TabLayoutMediator

class JsmGalleryActivity : AppCompatActivity() {
    private lateinit var binding:ActivityJsmGalleryBinding
    private var adapter: ViewPagerAdapter? = null
    private lateinit var mViewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityJsmGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        initialiseView(binding.viewPager)
    }

    private fun initialiseView(viewPager: ViewPager2) {
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        mViewPager = viewPager
        val fragments =  mutableListOf<Fragment>(
            PublishedFragment.newInstance(),
            NeedReviewFragment.newInstance()
        )

        adapter = ViewPagerAdapter(supportFragmentManager, fragments, lifecycle)
        mViewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Published" else "Needs Review"
        }.attach()

    }

}