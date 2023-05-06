package com.android.example.cameraxbasic.adapter

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.example.cameraxbasic.fragments.PublishedFragment
import com.android.example.cameraxbasic.viewmodels.DRAFT
import com.android.example.cameraxbasic.viewmodels.PUBLISHED

class ViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) PublishedFragment.newInstance(PUBLISHED)
        else
            PublishedFragment.newInstance(DRAFT)
    }

//    override fun getItemViewType(position: Int): Int {
//        return super.getItemViewType(position)
//    }

//    fun refreshFragment(index: Int, fragment: Fragment) {
//        try {
//            fragments[index] = fragment
//            notifyDataSetChanged()
//        } catch (ex: Exception) {
//            Log.d("Crash", ex.message.toString())
//        }
//    }


}