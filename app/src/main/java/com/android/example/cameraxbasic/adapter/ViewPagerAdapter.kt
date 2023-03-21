package com.android.example.cameraxbasic.adapter

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentManager: FragmentManager,
                       var fragments: MutableList<Fragment>,
                       lifecycle: Lifecycle
): FragmentStateAdapter(fragmentManager,lifecycle) {
    override fun getItemCount(): Int {
      return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun refreshFragment(index: Int, fragment: Fragment) {
        try {
            fragments[index] = fragment
            notifyItemChanged(index)
        } catch (ex: Exception) {
            Log.d("Crash", ex.message.toString())
        }
    }

}