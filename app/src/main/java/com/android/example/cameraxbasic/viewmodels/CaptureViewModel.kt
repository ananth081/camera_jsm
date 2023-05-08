package com.android.example.cameraxbasic.viewmodels

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.fragments.CameraFragment
import com.android.example.cameraxbasic.utils.CapturedMediaDto
import com.android.example.cameraxbasic.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


const val PUBLISHED = "Published"
const val DRAFT = "Draft"
const val APP_NAME = "Aconex Media"
//const val PUBLISHED_STRING = "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/$PUBLISHED"

class CaptureViewModel : ViewModel() {
    val mediaList: ArrayList<CapturedMediaDto> = arrayListOf()
    val status: MutableLiveData<Event<Boolean>> = MutableLiveData<Event<Boolean>>()

    val cancelCommunicator: MutableLiveData<String> = MutableLiveData<String>()

    fun reset() {
        mediaList.clear()
    }

    fun getSize(): Int {
        return mediaList.size
    }

    fun moveFileToDraftFolder(context: Context) {
//        viewModelScope.launch {
        val appName = context.resources.getString(R.string.app_name)
        var destinationUri: Uri?
        if (mediaList.isNotEmpty()) {
            mediaList.forEach {
                it.contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/${appName}/${DRAFT}"
                )

                val name = SimpleDateFormat(CameraFragment.FILENAME, Locale.US)
                    .format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        it.contentValues.getAsString(MediaStore.MediaColumns.MIME_TYPE)
                    )
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        val appName = context.resources.getString(R.string.app_name)
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/${appName}/${DRAFT}"
                        )
                    }
                }
                val uri: Uri = if (it.contentValues.getAsString(MediaStore.MediaColumns.MIME_TYPE)
                        .contains("image")
                )
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                destinationUri = context.contentResolver?.insert(
                    uri,
                    contentValues
                )

                val inputStream = context.contentResolver?.openInputStream(it.uri)
                val outputStream = context.contentResolver?.openOutputStream(destinationUri!!)
                inputStream?.let { it1 ->
                    outputStream?.let { it2 ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            FileUtils.copy(it1, it2)
                        }
                        try{
                            context.contentResolver?.delete(it.uri, null, null)
                        }catch (ex:java.lang.Exception){
                            ex.printStackTrace()
                        }
                    }
                }
//                }
//                Log.d("tag", "move and delete operation completed")
//                viewModelScope.launch(Dispatchers.Main) {
//                    status.value = Event(true)
//                }

            }
        }

    }

    fun deleteUnsavedMedia(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mediaList.forEach {
                    context.contentResolver?.delete(it.uri, null, null)
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
            cancelCommunicator.postValue("")
        }
    }

    @Synchronized
    fun deleteUnsavedMediaWithoutNotify(context: Context) {
        Log.d("tag", "deleteUnsavedMediaWithoutNotify")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mediaList.forEach {
                    context.contentResolver?.delete(it.uri, null, null)
                }
                mediaList.clear()
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun deleteList(list: java.util.ArrayList<String>, context: Context) {
        viewModelScope.launch {
            list.forEach { uriString ->
                val set = mediaList.filter { it.uri.toString() == uriString }.toSet()
                mediaList.removeAll(set)
            }
        }
    }
}
