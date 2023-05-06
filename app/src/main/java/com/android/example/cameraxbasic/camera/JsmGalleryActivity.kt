package com.android.example.cameraxbasic.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.adapter.ViewPagerAdapter
import com.android.example.cameraxbasic.databinding.ActivityJsmGalleryBinding
import com.android.example.cameraxbasic.fragments.GalleryViewDailogFragment
import com.android.example.cameraxbasic.fragments.PublishedFragment
import com.android.example.cameraxbasic.viewmodels.DRAFT
import com.android.example.cameraxbasic.viewmodels.PUBLISHED
import com.google.android.material.tabs.TabLayoutMediator

class JsmGalleryActivity : AppCompatActivity(),GalleryViewDailogFragment.selectedView {
    private lateinit var binding:ActivityJsmGalleryBinding
    private var adapter: ViewPagerAdapter? = null
    private lateinit var mViewPager: ViewPager2
    var source = false

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityJsmGalleryBinding.inflate(layoutInflater)
        source = intent.getBooleanExtra("IS_PUBLISHED_SCREEN",true)
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
            fragment.show(supportFragmentManager,"GalleryViewDailogFragment")
            fragment.initialiseObject(this)

        }

    }



    private fun initialiseView(viewPager: ViewPager2) {
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        mViewPager = viewPager
        val fragments =  mutableListOf<Fragment>(
            PublishedFragment.newInstance(PUBLISHED),
            PublishedFragment.newInstance(DRAFT)
        )

        adapter = ViewPagerAdapter(supportFragmentManager,  lifecycle)
        mViewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Published" else "Needs Review (4)"
        }.attach()

        if(source){
            mViewPager.setCurrentItem(0)
        }else{
            mViewPager.setCurrentItem(1)

        }



    }

    override fun getSelectedView(selectedView: String) {
//        val fragment = adapter?.fragments?.get(0)
//        (fragment as PublishedFragment).onMediaViewSelected(selectedView)
//        adapter?.refreshFragment(0,PublishedFragment())

    }

}