package com.android.example.cameraxbasic.viewmodels

import android.content.Context
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.android.example.cameraxbasic.utils.MediaStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GalleryViewModel : ViewModel() {
    val communicator: MutableLiveData<MutableList<MediaStoreFile>> by lazy {
        MutableLiveData<MutableList<MediaStoreFile>>()
    }
    val list: MutableList<MediaStoreFile> = mutableListOf()


    fun loadImages(context: Context, type: String) {
        val mediaStoreUtils = MediaStoreUtils(context)
        viewModelScope.launch(Dispatchers.IO) {
            list.clear()
            list.addAll(
                mediaStoreUtils.getMediaList(
                    "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/$type"
                )
            )
            list.addAll(
                mediaStoreUtils.getMediaList(
                    "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/$type",
                    mediaStoreUtils.videoStoreCollection
                )
            )
            communicator.postValue(list)
        }
    }
}