/*
 * Copyright 2020 The Android Open Source Project
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

package com.android.example.cameraxbasic.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.databinding.GalleryImageItemBinding
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File


/** Fragment used for each individual page showing a photo inside of [GalleryFragment] */
class PhotoFragment internal constructor() : Fragment() {

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
//                              savedInstanceState: Bundle?) = ImageView(context)

    lateinit var galleryImageItemBinding: GalleryImageItemBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        galleryImageItemBinding = GalleryImageItemBinding.inflate(inflater, container, false)
        return galleryImageItemBinding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments ?: return
        val resource = args.getString(FILE_NAME_KEY)?.let { File(it) } ?: R.drawable.ic_photo
       // Glide.with(view).load(resource).into(galleryImageItemBinding.galleryImage)
        galleryImageItemBinding.galleryImage.setImageURI(Uri.parse(resource.toString()))
    }

    companion object {
        private const val FILE_NAME_KEY = "file_name"

        fun create(mediaStoreFile: MediaStoreFile) = PhotoFragment().apply {
            val image = mediaStoreFile.file
            arguments = Bundle().apply {
                putString(FILE_NAME_KEY, image.absolutePath)
            }
        }
    }
}