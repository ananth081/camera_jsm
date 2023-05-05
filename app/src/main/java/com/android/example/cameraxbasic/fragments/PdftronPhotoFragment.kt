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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.databinding.PdftronImageItemBinding
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.PDFViewCtrl
import com.pdftron.pdf.config.ViewerBuilder2
import com.pdftron.pdf.config.ViewerConfig
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment2
import com.pdftron.pdf.model.FileInfo
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
import java.io.File


/** Fragment used for each individual page showing a photo inside of [GalleryFragment] */
class PdftronPhotoFragment internal constructor() : Fragment(), PdfViewCtrlTabHostFragment2.TabHostListener {

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
//                              savedInstanceState: Bundle?) = ImageView(context)

    private lateinit var mHostFragment: PdfViewCtrlTabHostFragment2
    private var mPdfViewCtrl: PDFViewCtrl? = null
    private var mPdfDoc: PDFDoc? = null
    lateinit var galleryImageItemBinding: PdftronImageItemBinding
    var mediastoreFile:File? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        galleryImageItemBinding = PdftronImageItemBinding.inflate(inflater, container, false)
        return galleryImageItemBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments ?: return

       // Glide.with(view).load(resource).into(galleryImageItemBinding.galleryImage)
        //galleryImageItemBinding.galleryImage.setImageURI(Uri.parse(resource.toString()))
        val config = ViewerConfig.Builder()
            .addToolbarBuilder(getToolbar())
            .toolbarTitle(getString(R.string.app_name))
            .showToolbarSwitcher(false)
            .hidePresetBar(true)
            .multiTabEnabled(false)
            .showThumbnailView(false)
            .showSearchView(false)
            .showReflowOption(false)
            .showBookmarksView(false)
            .showShareOption(false)
            .showDocumentSettingsOption(false)
            .showEditPagesOption(false)
            .showSaveCopyOption(false)
            .showPrintOption(false)
            .showCloseTabOption(false)
            .showTopToolbar(false)
            .build()

        mHostFragment = ViewerBuilder2
            .withUri(Uri.fromFile(mediastoreFile))
            .usingConfig(config)
            //.usingTheme(R.style.Theme_PdftronSampleActivity)
            .build(requireContext())

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, mHostFragment)
            .commit()
    }

    companion object {
        private const val FILE_NAME_KEY = "file_name"

        fun create(mediaStoreFile: MediaStoreFile) = PdftronPhotoFragment().apply {
            val image = mediaStoreFile.file
            mediastoreFile = mediaStoreFile.file
            arguments = Bundle().apply {
                putString(FILE_NAME_KEY, image.absolutePath)
            }
        }
    }

    private fun getToolbar(): AnnotationToolbarBuilder {
        return AnnotationToolbarBuilder.withTag("Toolbar")
            .addToolButton(
                ToolbarButtonType.INK,
                DefaultToolbars.ButtonId.INK.value()
            )
            .addToolButton(ToolbarButtonType.ARROW, DefaultToolbars.ButtonId.ARROW.value())
            .addToolButton(
                ToolbarButtonType.LINE,
                DefaultToolbars.ButtonId.LINE.value()
            )
            .addToolButton(ToolbarButtonType.IMAGE, DefaultToolbars.ButtonId.IMAGE.value())
    }

    override fun onTabHostShown() {
        TODO("Not yet implemented")
    }

    override fun onTabHostHidden() {
        TODO("Not yet implemented")
    }

    override fun onLastTabClosed() {
        TODO("Not yet implemented")
    }

    override fun onTabChanged(tag: String?) {
        TODO("Not yet implemented")
    }

    override fun onOpenDocError(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onNavButtonPressed() {
        TODO("Not yet implemented")
    }

    override fun onShowFileInFolder(fileName: String?, filepath: String?, itemSource: Int) {
        TODO("Not yet implemented")
    }

    override fun canShowFileInFolder(): Boolean {
        TODO("Not yet implemented")
    }

    override fun canShowFileCloseSnackbar(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onToolbarCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onToolbarPrepareOptionsMenu(menu: Menu?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onToolbarOptionsItemSelected(item: MenuItem?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onStartSearchMode() {
        TODO("Not yet implemented")
    }

    override fun onExitSearchMode() {
        TODO("Not yet implemented")
    }

    override fun canRecreateActivity(): Boolean {
        return false
    }

    override fun onTabPaused(fileInfo: FileInfo?, isDocModifiedAfterOpening: Boolean) {

    }

    override fun onJumpToSdCardFolder() {

    }

    override fun onTabDocumentLoaded(tag: String?) {

    }
}