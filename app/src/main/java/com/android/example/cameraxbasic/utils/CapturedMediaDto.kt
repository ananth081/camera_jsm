package com.android.example.cameraxbasic.utils

import android.content.ContentValues
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class CapturedMediaDto(val contentValues: ContentValues, val uri: Uri, val id: Long = 0L)