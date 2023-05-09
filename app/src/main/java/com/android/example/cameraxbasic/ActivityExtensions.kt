package com.android.example.cameraxbasic

import androidx.appcompat.app.AppCompatActivity


fun AppCompatActivity.isTablet(): Boolean {
    return resources.getBoolean(R.bool.isTablet)
}
