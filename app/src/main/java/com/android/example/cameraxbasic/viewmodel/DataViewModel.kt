package com.android.example.cameraxbasic.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

const val DIRECTORY_NAME = "/Pictures/JSM Analysis"

class DataViewModel : ViewModel() {

    var dataList: List<Any> = emptyList()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val filePath =
                File(Environment.getExternalStorageDirectory().path + File.separator + DIRECTORY_NAME)
            val dataList = filePath.listFiles()?.toList()!!

        }

    }
}