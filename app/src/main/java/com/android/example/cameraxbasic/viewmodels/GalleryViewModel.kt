package com.android.example.cameraxbasic.viewmodels

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.android.example.cameraxbasic.utils.MediaStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val DAY_FILTER = 1
const val MONTH_FILTER = 2

class GalleryViewModel : ViewModel() {
    var filterType = DAY_FILTER
    private val displayDayDateFormat: SimpleDateFormat =
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val displayMonthFormat: SimpleDateFormat =
        SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val communicator: MutableLiveData<MutableList<Any>> by lazy {
        MutableLiveData<MutableList<Any>>()
    }
    val list: MutableList<Any> = mutableListOf()


    fun loadImages(context: Context, type: String, filterType: Int = DAY_FILTER) {
        val mediaStoreUtils = MediaStoreUtils(context)
        viewModelScope.launch(Dispatchers.IO) {
            list.clear()
            val newMutableList = arrayListOf<MediaStoreFile>()

            newMutableList.addAll(
                mediaStoreUtils.getMediaList(
                    "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/$type"
                )
            )
            newMutableList.addAll(
                mediaStoreUtils.getMediaList(
                    "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/$type",
                    mediaStoreUtils.videoStoreCollection
                )
            )
//Fri May 05 00:00:00 GMT+05:30 2023
            if (filterType == DAY_FILTER) {
                val groupBy = newMutableList.groupBy {
                    it.dayTaken
                }

                Log.d("groupBy", groupBy.toString())
                groupBy.forEach {
                    list.add(displayDayDateFormat.format(it.key))
                    list.addAll(it.value)
                }
            } else {
                //displayMonthFormat
                val groupBy = newMutableList.groupBy {
                    it.monthTaken
                }

                Log.d("groupBy", groupBy.toString())
                groupBy.forEach {
                    list.add(displayMonthFormat.format(it.key))
                    list.addAll(it.value)
                }
            }
            communicator.postValue(list)
        }
    }
}