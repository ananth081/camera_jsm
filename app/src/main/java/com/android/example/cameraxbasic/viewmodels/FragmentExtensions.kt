package com.android.example.cameraxbasic.viewmodels

import androidx.fragment.app.Fragment
import com.android.example.cameraxbasic.R

fun Fragment.isTablet(): Boolean {
  return activity?.applicationContext?.resources?.getBoolean(R.bool.isTablet) == true
//   resources.getBoolean(R.bool.isTablet)
}
