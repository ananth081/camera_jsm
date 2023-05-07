/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.cameraxbasic.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import androidx.core.net.toUri
import com.android.example.cameraxbasic.viewmodels.PUBLISHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * A utility class for accessing this app's photo storage.
 *
 * Since this app doesn't request any external storage permissions, it will only be able to access
 * photos taken with this app. If the app is uninstalled, the photos taken with this app will stay
 * on the device, but reinstalling the app will not give it access to photos taken with the app's
 * previous instance. You can request further permissions to change this app's access. See this
 * guide for more: https://developer.android.com/training/data-storage.
 */
class MediaStoreUtils(private val context: Context) {

    val imageStoreCollection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        context.getExternalFilesDir(null)?.toUri()!!
    }
    val videoStoreCollection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        context.getExternalFilesDir(null)?.toUri()!!
    }

    private suspend fun getMediaStoreImageCursor(mediaStoreCollection: Uri): Cursor? {
        var cursor: Cursor?
        withContext(Dispatchers.IO) {
            val projection = arrayOf(imageDataColumnIndex, imageIdColumnIndex, dateAddedColumnIndex)
            val sortOrder = "DATE_ADDED DESC"
            cursor = context.contentResolver.query(
                mediaStoreCollection, projection, null, null, sortOrder
            )
        }
        return cursor
    }

    private suspend fun getPublishedImageCursor(mediaStoreCollection: Uri): Cursor? {
        var cursor: Cursor?

        withContext(Dispatchers.IO) {
            val projection = arrayOf(imageDataColumnIndex, imageIdColumnIndex)
            val sortOrder = "DATE_ADDED DESC"
            cursor = context.contentResolver.query(
                mediaStoreCollection,
                projection,
                android.provider.MediaStore.Images.Media.DATA + "like ?",
                arrayOf(PUBLISHED),
                sortOrder,
                null
            )
        }
        return cursor
    }


    fun deleteImageAPI29(context: Context) {
        imageStoreCollection?.let {
            val resolver: ContentResolver = context.contentResolver
            try {
                resolver.delete(it, null, null)
            } catch (ex: java.lang.Exception) {
                Log.d("", "deleteImageAPI29: ex" + ex.printStackTrace())
            }
        }
    }

    suspend fun getLatestImageFilename(): String? {
        var filename: String?
        if (imageStoreCollection == null) return null

        getMediaStoreImageCursor(imageStoreCollection).use { cursor ->
            if (cursor?.moveToFirst() != true) return null
            filename = cursor.getString(cursor.getColumnIndexOrThrow(imageDataColumnIndex))
        }

        return filename
    }

    suspend fun getMediaList(
        type: String = "",
        uri: Uri = imageStoreCollection
    ): MutableList<MediaStoreFile> {
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US)

        val files = mutableListOf<MediaStoreFile>()
        getMediaStoreImageCursor(uri).use { cursor ->
            val imageDataColumn = cursor?.getColumnIndexOrThrow(imageDataColumnIndex)
            val imageIdColumn = cursor?.getColumnIndexOrThrow(imageIdColumnIndex)
            val imageDateAddedIDColumn = cursor?.getColumnIndexOrThrow(dateAddedColumnIndex)
            if (cursor != null && imageDataColumn != null && imageIdColumn != null) {
                while (cursor.moveToNext()) {

                    if (cursor.getString(imageDataColumn)
                            .contains(type)
                    ) {
                        val dateTaken = imageDateAddedIDColumn?.let { cursor.getLong(it) } ?: 0L
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = dateTaken

                        val dateObj = formatter.format(calendar.time)
                        val date = formatter.parse(dateObj) ?: Date()
                        val id = cursor.getLong(imageIdColumn)
                        val contentUri: Uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val contentFile = File(cursor.getString(imageDataColumn))
                        files.add(
                            MediaStoreFile(
                                contentUri, contentFile, id,
                                fileType = if (uri == videoStoreCollection) FILE_TYPE_VIDEO
                                else FILE_TYPE_IMAGE, date
                            )
                        )
                    }
                }
            }
        }
        return files
    }


    companion object {
        // Suppress DATA index deprecation warning since we need the file location for the Glide library
        @Suppress("DEPRECATION")
        private const val imageDataColumnIndex = MediaStore.Images.Media.DATA
        private const val imageIdColumnIndex = MediaStore.Images.Media._ID
        private const val dateAddedColumnIndex = MediaStore.Images.Media.DATE_TAKEN
    }
}

data class MediaStoreFile(
    val uri: Uri,
    val file: File,
    val id: Long,
    val fileType: String = FILE_TYPE_IMAGE, val dateTaken: Date = Date()
)