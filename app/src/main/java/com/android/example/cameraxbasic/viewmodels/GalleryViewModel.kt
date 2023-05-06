package com.android.example.cameraxbasic.viewmodels

import android.content.Context
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.android.example.cameraxbasic.utils.MediaStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GalleryViewModel : ViewModel() {
    val communicator: MutableLiveData<MutableList<MediaStoreFile>> by lazy {
        MutableLiveData<MutableList<MediaStoreFile>>()
    }
    val list: MutableList<MediaStoreFile> = mutableListOf()


    fun loadImages(context: Context, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            list.clear()
            list.addAll(
                MediaStoreUtils(context).getImages(
                    "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/$type"
                )
            )
            communicator.postValue(list)
        }
    }
}