package com.android.example.cameraxbasic

import android.app.Activity
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity


fun AppCompatActivity.isTablet(): Boolean {
    return resources.getBoolean(R.bool.isTablet)
}

fun Activity.getWidthInPixel(percentage: Int): Int {
    val widthInPercentage = percentage / 100.0
    return (getScreenWidth() * widthInPercentage).toInt()
}

fun Activity.getHeightInPixel(percentage: Int): Int {
    val heightInPercentage = percentage / 100.0
    return (getScreenHeight() * heightInPercentage).toInt()
}

fun Activity.getScreenWidth(): Int {
    val displayMetrics = DisplayMetrics()
    windowManager?.defaultDisplay?.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

fun Activity.getScreenHeight(): Int {
    val displayMetrics = DisplayMetrics()
    windowManager?.defaultDisplay?.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
}