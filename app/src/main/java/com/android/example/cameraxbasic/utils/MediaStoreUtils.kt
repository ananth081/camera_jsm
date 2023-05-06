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

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.core.net.toUri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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

    val mediaStoreCollection: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        context.getExternalFilesDir(null)?.toUri()
    }

    private suspend fun getMediaStoreImageCursor(mediaStoreCollection: Uri): Cursor? {
        var cursor: Cursor?
        withContext(Dispatchers.IO) {
            val projection = arrayOf(imageDataColumnIndex, imageIdColumnIndex)
            val sortOrder = "DATE_ADDED DESC"
            cursor = context.contentResolver.query(
                mediaStoreCollection, projection, null, null, sortOrder
            )
        }
        return cursor
    }


    fun deleteImageAPI29(context: Context) {
        mediaStoreCollection?.let {
            val resolver: ContentResolver = context.contentResolver
            try {
                resolver.delete(it, null, null)
            } catch (ex: java.lang.Exception) {
                Log.d("", "deleteImageAPI29: ex"+ex.printStackTrace())
            }
        }
    }

    suspend fun getLatestImageFilename(): String? {
        var filename: String?
        if (mediaStoreCollection == null) return null

        getMediaStoreImageCursor(mediaStoreCollection).use { cursor ->
            if (cursor?.moveToFirst() != true) return null
            filename = cursor.getString(cursor.getColumnIndexOrThrow(imageDataColumnIndex))
        }

        return filename
    }


    suspend fun getImages(): MutableList<MediaStoreFile> {
        val files = mutableListOf<MediaStoreFile>()
        if (mediaStoreCollection == null) return files

        getMediaStoreImageCursor(mediaStoreCollection).use { cursor ->
            val imageDataColumn = cursor?.getColumnIndexOrThrow(imageDataColumnIndex)
            val imageIdColumn = cursor?.getColumnIndexOrThrow(imageIdColumnIndex)

            if (cursor != null && imageDataColumn != null && imageIdColumn != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(imageIdColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val contentFile = File(cursor.getString(imageDataColumn))
                    files.add(MediaStoreFile(contentUri, contentFile, id))
                }
            }
        }

        return files
    }

    suspend fun deleteImages(){
        //val files = mutableListOf<MediaStoreFile>()

        mediaStoreCollection?.let {
            getMediaStoreImageCursor(it).use { cursor ->
            }
        }

    }

    companion object {
        // Suppress DATA index deprecation warning since we need the file location for the Glide library
        @Suppress("DEPRECATION")
        private const val imageDataColumnIndex = MediaStore.Images.Media.DATA
        private const val imageIdColumnIndex = MediaStore.Images.Media._ID
    }
}

data class MediaStoreFile(val uri: Uri, val file: File, val id: Long)

//    private suspend fun getMediaStoreImageCursor(mediaStoreCollection: Uri): Cursor? {
//        var cursor: Cursor?
//        val appName = context.resources.getString(R.string.app_name)
//        val DIRECTORY_NAME = "%Pictures/$appName%"
//        val selectionArgs = arrayOf(DIRECTORY_NAME)
//
//        withContext(Dispatchers.IO) {
//            val sortOrder = "DATE_ADDED DESC"
//            val projection = arrayOf(imageDataColumnIndex, imageIdColumnIndex)
////            val sortOrder = "DATE_ADDED DESC"
////            cursor = context.contentResolver.query(
////                mediaStoreCollection, projection, null, null, sortOrder
////            )
//            cursor = context.contentResolver.query(
//                MediaStore.Files.getContentUri("external"),
//                projection,
//                MediaStore.Images.Media.DATA + " like ? ",
//                selectionArgs,
//                sortOrder);
//        }
//        return cursor
//    }

//    suspend fun getImages(): MutableList<MediaStoreFile> {
//        val files = mutableListOf<MediaStoreFile>()
//        if (mediaStoreCollection == null) return files
//
//        getMediaStoreImageCursor(mediaStoreCollection).use { cursor ->
//            val imageDataColumn = cursor?.getColumnIndexOrThrow(imageDataColumnIndex)
//            val imageIdColumn = cursor?.getColumnIndexOrThrow(imageIdColumnIndex)
//
//            if (cursor != null && imageDataColumn != null && imageIdColumn != null) {
//                while (cursor.moveToNext()) {
//                    val id = cursor.getLong(imageIdColumn)
//                    val contentUri: Uri = ContentUris.withAppendedId(
//                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                        id
//                    )
//                    val contentFile = File(cursor.getString(imageDataColumn))
//                    files.add(MediaStoreFile(contentUri, contentFile, id))
//                }
//            }
//        }
//
//        return files
//    }
