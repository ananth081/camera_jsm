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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.window.WindowManager
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.camera.CameraActivity
import com.android.example.cameraxbasic.camera.GalleryActivity
import com.android.example.cameraxbasic.databinding.CameraPreviewBinding
import com.android.example.cameraxbasic.save.SaveDialog
import com.android.example.cameraxbasic.utils.ANIMATION_FAST_MILLIS
import com.android.example.cameraxbasic.utils.ANIMATION_SLOW_MILLIS
import com.android.example.cameraxbasic.utils.CapturedMediaDto
import com.android.example.cameraxbasic.utils.DELETED_LIST_INTENT_KEY
import com.android.example.cameraxbasic.utils.MEDIA_LIST_KEY
import com.android.example.cameraxbasic.utils.MediaStoreUtils
import com.android.example.cameraxbasic.viewmodels.APP_NAME
import com.android.example.cameraxbasic.viewmodels.CaptureViewModel
import com.android.example.cameraxbasic.viewmodels.PUBLISHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 */
private var PERMISSIONS_REQUIRED =
    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
private var WRITE_PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

class CameraFragment : Fragment() {

    //private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private lateinit var cameraPreview: CameraPreviewBinding
    private lateinit var mediaStoreUtils: MediaStoreUtils
    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var windowManager: WindowManager
    private var fromRetakeScreen: String? = null
    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    val captureViewModel: CaptureViewModel by activityViewModels()

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService


    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation

            }
        } ?: Unit
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
//        if (!PermissionsFragment.hasPermissions(requireContext())) {
//            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
//                CameraFragmentDirections.actionCameraToPermissions()
//            )
//        }
        // if (!PermissionsFragment.hasPermissions(requireContext())) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                PERMISSIONS_REQUIRED,
                100
            )
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                WRITE_PERMISSIONS_REQUIRED,
                101
            )
        }
        // }

    }

    override fun onDestroyView() {
        //  cameraPreview = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        cameraPreview = CameraPreviewBinding.inflate(inflater, container, false)
        //  _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        //  return fragmentCameraBinding.root
        return cameraPreview!!.root
    }

    private fun setGalleryThumbnail(filename: Uri) {
        // Run the operations in the view's thread
        lifecycleScope.launch(Dispatchers.Main) {
            cameraPreview.photoViewButton.visibility = View.VISIBLE
            cameraPreview.photoView.let { photoViewButton ->
                photoViewButton.post {
                    // Remove thumbnail padding
                    photoViewButton.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

                    // Load thumbnail into circular button using Glide
//                photoViewButton.setImageURI(Uri.parse(filename))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val bitmap = context?.contentResolver?.loadThumbnail(
                            filename,
                            Size(100, 100),
                            null
                        )
                        photoViewButton.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()


        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        // Initialize WindowManager to retrieve display metrics
        windowManager = WindowManager(view.context)

        // Initialize MediaStoreUtils for fetching this app's images
        mediaStoreUtils = MediaStoreUtils(requireContext())

        // Wait for the views to be properly laid out
        cameraPreview.viewFinder?.post {
            // Keep track of the display in which this view is attached
            cameraPreview?.viewFinder?.let {
                displayId = it?.display?.displayId ?: 0
            }

//            lifecycleScope.launch {
//                context?.let {
//                    mediaStoreUtils.deleteImageAPI29(it)
//                }
//            }

            // Build UI controls
            updateCameraUi()

            // Set up the camera and its use cases
            lifecycleScope.launch {
                setUpCamera()
            }
        }

//        cameraPreview?.photoBtn?.setOnTouchListener { v, event ->
//            cameraPreview?.photoBtn?.background =
//                ResourcesCompat.getDrawable(resources, R.drawable.bg_photo_text, context?.theme)
//            cameraPreview?.photoBtn?.setTextColor(resources.getColor(R.color.ic_white))
//            cameraPreview?.videoBtn?.setBackgroundColor(resources.getColor(R.color.black_overlay))
//            cameraPreview?.videoBtn?.setTextColor(resources.getColor(R.color.ic_white))
//            (activity as CameraActivity).showPhotoFragment()
//            true
//        }

        cameraPreview?.videoBtn?.setOnTouchListener { v, event ->
            captureViewModel.deleteUnsavedMediaWithoutNotify(requireContext())
            cameraPreview?.photoBtn?.setBackgroundColor(resources.getColor(R.color.trans))
            cameraPreview?.photoBtn?.setTextColor(resources.getColor(R.color.ic_white))
            cameraPreview?.videoBtn?.background =
                ResourcesCompat.getDrawable(resources, R.drawable.bg_photo_text, context?.theme)
            cameraPreview?.videoBtn?.setTextColor(resources.getColor(R.color.ic_white))
            (activity as CameraActivity).showVideoFragment()
            true
        }
//        cameraPreview?.videoBtn?.setOnClickListener {
//            cameraPreview?.photoBtn?.setBackgroundColor(resources.getColor(R.color.black_overlay))
//            cameraPreview?.photoBtn?.setTextColor(resources.getColor(R.color.ic_white))
//            cameraPreview?.videoBtn?.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_photo_text, context?.theme)
//            cameraPreview?.videoBtn?.setTextColor(resources.getColor(R.color.ic_white))
//        }

//        val tabLayout = cameraPreview.c
//            tabLayout.addTab(tabLayout.newTab().setText("Photo"))
//        tabLayout.addTab(tabLayout.newTab().setText("Video"))
//
//        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab?) {
//                if (0 == tab?.position) {
//                    showPhotoFragment(CameraFragment())
//                } else {
//                    showVideoFragment(CaptureFragment())
//                }
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab?) {
//
//            }
//
//            override fun onTabReselected(tab: TabLayout.Tab?) {
//
//            }
//
//        })
        Log.d("PRS", "test")
        cameraPreview.saveText.visibility = View.INVISIBLE
        captureViewModel.cancelCommunicator.observe(viewLifecycleOwner) {
            handleCancelClicked()
        }
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged: called" + "")
        super.onConfigurationChanged(newConfig)
        // Rebind the camera with the updated display metrics
        bindCameraUseCases()
        Log.d(TAG, "onConfigurationChanged: called" + "")

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private suspend fun setUpCamera() {
        cameraProvider = mContext?.let { ProcessCameraProvider.getInstance(it).await() }

        // Select lensFacing depending on the available cameras
        lensFacing = when {
            hasBackCamera() -> CameraSelector.LENS_FACING_BACK
            hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
            else -> throw IllegalStateException("Back and front camera are unavailable")
        }

        // Enable or disable switching between cameras
        updateCameraSwitchButton()

        // Build and bind the camera use cases
        bindCameraUseCases()
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = windowManager.getCurrentWindowMetrics().bounds
        Log.d(TAG, "Screen metrics: ${metrics.width()} x ${metrics.height()}")

        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = cameraPreview?.viewFinder?.display?.rotation ?: 0
        Log.d(TAG, "bindCameraUseCases: rotation==" + rotation)

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // durin    g the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()


        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        if (camera != null) {
            // Must remove observers from the previous camera instance
            removeCameraStateObservers(camera!!.cameraInfo)
        }

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(cameraPreview?.viewFinder?.surfaceProvider)
            observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun removeCameraStateObservers(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.removeObservers(viewLifecycleOwner)
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.OPEN -> {
                        // Setup Camera resources and begin processing
//                        Toast.makeText(
//                            context,
//                            "CameraState: Open",
//                            Toast.LENGTH_SHORT
//                        ).show()
                        cameraPreview?.placeHolderLayout?.visibility = View.GONE
                    }

                    else -> {
                        //Handle any other condition
                    }
                }
            }
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {

        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {

        // Remove previous UI if any
        cameraPreview?.root?.let {
            cameraPreview!!.root.removeView(it)
        }

//        cameraUiContainerBinding = CameraUiContainerBinding.inflate(
//            LayoutInflater.from(requireContext()),
//            fragmentCameraBinding.root,
//            true
//        )

//        cameraPreview?.photoView?.setImageDrawable(resources.getDrawable(R.drawable.ic_placeholder_img))
        // In the background, load latest photo taken (if any) for gallery thumbnail
//        lifecycleScope.launch {
//            val thumbnailUri = mediaStoreUtils.getLatestImageFilename()
//            thumbnailUri?.let {
//                setGalleryThumbnail(it)
//            }
//        }

        // Listener for button used to capture photo

        cameraPreview.cameraCaptureButton?.setOnClickListener {
            cameraPreview.saveText.visibility = View.VISIBLE


            val sound = MediaActionSound()
            sound.play(MediaActionSound.SHUTTER_CLICK)

            val anim: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.flash_screen);
            anim.fillAfter = true
            cameraPreview?.viewFinder?.startAnimation(anim)

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->
                // Create time stamped name and MediaStore entry.
                val name = SimpleDateFormat(FILENAME, Locale.US).format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/$PUBLISHED"
                        )
                    }
                }


                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(
                        requireContext().contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = output.savedUri
                            val capturedMediaDto =
                                savedUri?.let { uri ->
                                    captureViewModel.mediaList.add(
                                        CapturedMediaDto(contentValues, uri)
                                    )
                                }
                            val saveTxt = "Save(${captureViewModel.getSize()})"
                            lifecycleScope.launch(Dispatchers.Main) {
                                cameraPreview.saveText.text = saveTxt
                            }
                            Log.d(TAG, "Photo capture succeeded: $savedUri")
                            savedUri?.let { it1 -> setGalleryThumbnail(it1) }
                            // We can only change the foreground Drawable using API level 23+ API
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                // Update the gallery thumbnail with latest picture taken

                                if ("retake_picture" == fromRetakeScreen) {
                                    lifecycleScope.launch {
                                        val list = captureViewModel.mediaList.map {
                                            it.uri.toString()
                                        }.toList()
                                        val intent = Intent(context, GalleryActivity::class.java)
                                        intent.putStringArrayListExtra(
                                            MEDIA_LIST_KEY,
                                            ArrayList(list)
                                        )
                                        startActivity(intent)
                                    }
                                }
                            }
                        }
                    })

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Display flash animation to indicate that photo was captured
                    cameraPreview?.viewFinder?.postDelayed({
                        cameraPreview!!.viewFinder.foreground = ColorDrawable(Color.WHITE)
                        cameraPreview!!.viewFinder.postDelayed(
                            { cameraPreview!!.viewFinder.foreground = null }, ANIMATION_FAST_MILLIS
                        )
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }

        cameraPreview?.dualCamera?.let {
            it.isEnabled = true
            it.setOnClickListener {

                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                cameraPreview?.placeHolderLayout?.visibility = View.VISIBLE

                bindCameraUseCases()

                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    cameraPreview?.flashLight?.isEnabled = false
                    cameraPreview?.flashLight?.visibility = View.INVISIBLE


                } else if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraPreview?.flashLight?.isEnabled = true
                    cameraPreview?.flashLight?.isVisible = true
                }
            }
        }
        cameraPreview?.flashLight?.let { flashLightImg ->
            flashLightImg.isEnabled = true
            var torchState: Boolean
            flashLightImg.setOnClickListener {
                torchState = TorchState.ON == camera?.cameraInfo?.torchState?.value
                if (torchState) {
                    flashLightImg.background = resources.getDrawable(R.drawable.flash_circle_1)
                    camera?.cameraControl?.enableTorch(false)
                } else {
                    flashLightImg.background = resources.getDrawable(R.drawable.flash_circle_on)
                    camera?.cameraControl?.enableTorch(true)
                }
            }
        }

        cameraPreview?.photoViewButton?.setOnClickListener {

            if (captureViewModel.mediaList.isNotEmpty()) {

                val list = captureViewModel.mediaList.map {
                    it.uri.toString()
                }.toList()
                val intent = Intent(context, GalleryActivity::class.java)
                intent.putStringArrayListExtra(MEDIA_LIST_KEY, ArrayList(list))
                startForResult.launch(intent)
            }

        }

        cameraPreview?.cancel?.setOnClickListener {
            if (captureViewModel.mediaList.isNotEmpty()) {
                captureViewModel.deleteUnsavedMedia(requireContext())
            } else {
                handleCancelClicked()
            }
        }

        cameraPreview?.saveText?.setOnClickListener {


//
//            val appName = "JSM Analysis"
//            val appName2 = "Test"
//            val filePath1 =
//                File(Environment.getExternalStorageDirectory().path + File.separator + "Pictures/${appName}")
//           //var fileList = filePath1.listFiles()?.toList()
//            var filePath2 = File(Environment.getExternalStorageDirectory().path + File.separator + "Pictures/${appName2}")
////            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
////
////                Files.move(
////                    filePath1.toPath(),
////                    filePath2.toPath(),
////                    StandardCopyOption.REPLACE_EXISTING
////                )
////            }
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
////                val filePath = File(Environment.getExternalStorageDirectory().path + File.separator + "/Pictures/JSM Analysis")
//                var fileList:List<File>? = null
//                fileList = filePath1.listFiles()?.toList()
//                var i = 0
//                fileList?.forEach {
//                  // filePath2 = File(it.toString().replace("JSM Analysis","Test"))
//                    copyDir(fileList?.get(i)?.toPath()!!, filePath2.toPath())
//                    i++
//                }
//
//
//               // copyDir(fileList?.get(0)?.toPath()!!, filePath2.toPath())
//
//            }
            val dialog = SaveDialog.newInstance()
            dialog.show(childFragmentManager, SaveDialog::class.java.simpleName)
        }

        setScale()


        cameraPreview?.cameraZoomText05?.setOnClickListener {
            camera?.cameraControl?.setLinearZoom(0.02f)
            cameraPreview?.cameraZoomText05?.text = "1x"
            cameraPreview?.cameraZoomText0?.setBackgroundResource(R.drawable.zoom_button_bg_inactive)
            context?.getColor(R.color.ic_white)
                ?.let { it1 -> cameraPreview?.cameraZoomText0?.setTextColor(it1) }
            context?.resources?.getColor(R.color.txBlack)
                ?.let { it1 -> cameraPreview?.cameraZoomText05?.setTextColor(it1) }
            it.setBackgroundResource(R.drawable.zoom_button_bg)
        }

        cameraPreview?.cameraZoomText0?.setOnClickListener {
            camera?.cameraControl?.setLinearZoom(0.7f)
            cameraPreview?.cameraZoomText0?.text = "2x"
            cameraPreview?.cameraZoomText05?.setBackgroundResource(R.drawable.zoom_button_bg_inactive)
            context?.getColor(R.color.ic_white)
                ?.let { it1 -> cameraPreview?.cameraZoomText05?.setTextColor(it1) }
            context?.resources?.getColor(R.color.txBlack)
                ?.let { it1 -> cameraPreview?.cameraZoomText0?.setTextColor(it1) }
            it.setBackgroundResource(R.drawable.zoom_button_bg)
        }
    }

    private fun handleCancelClicked() {
        captureViewModel.reset()
        activity?.finish()
    }

    fun copyDir(src: Path, dest: Path) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            //  Files.move(src,dest,StandardCopyOption.REPLACE_EXISTING)
//        Files.walk(src).forEach {
//            Files.copy(
//                it, dest.resolve(src.relativize(it)),
//                StandardCopyOption.REPLACE_EXISTING
//            )
//        }
        }
    }

    override fun onDetach() {
        super.onDetach()
        captureViewModel.reset()
    }

    fun setCameraZoomLevels(zoomValue: Float) {
        camera?.cameraControl?.setLinearZoom(zoomValue)
    }

    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        try {
            cameraPreview?.dualCamera?.isEnabled =
                hasBackCamera() && hasFrontCamera()
            if (!(hasBackCamera() && hasFrontCamera())) {
                cameraPreview?.dualCamera?.visibility = View.GONE
            }
        } catch (exception: CameraInfoUnavailableException) {
            cameraPreview?.dualCamera?.isEnabled = false

        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    companion object {
        private const val TAG = "CameraXBasic"
        const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_TYPE = "image/jpeg"
        const val VIDEO_TYPE = "video/mp4"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }


    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                fromRetakeScreen = intent?.extras?.getString("source")
                val fileUri = intent?.extras?.getString("file_uri")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val list =
                        captureViewModel.mediaList.filter { it.uri.toString() == fileUri }.toList()
                    captureViewModel.mediaList.removeAll(list)
                }
                hideRetakeUiControls()
                val list = intent?.getStringArrayListExtra(DELETED_LIST_INTENT_KEY)
                if (list != null && list.isNotEmpty())
                    captureViewModel.deleteList(list, requireContext())
                // Handle the Intent
            }
        }


    private fun hideRetakeUiControls() {
        cameraPreview?.saveText?.visibility = View.GONE
        cameraPreview?.photoViewButton?.visibility = View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setScale() {
        val listener: ScaleGestureDetector.OnScaleGestureListener =
            object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
                    val f: ZoomState? = camera?.cameraInfo?.zoomState?.value
                    val scale: Float = scaleGestureDetector.scaleFactor
                    val zoomRatio = scale * f?.zoomRatio!!


                    camera?.cameraControl?.setZoomRatio(zoomRatio)
                    if (activity != null && activity is CameraActivity) {
                        (activity as CameraActivity).updateZoomText(zoomRatio)
                    }
                    return true
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                }

            }
        val scaleGestureDetector = ScaleGestureDetector(mContext!!, listener)

        cameraPreview?.viewFinder?.setOnTouchListener { view, motionEvent ->
            scaleGestureDetector.onTouchEvent(
                motionEvent
            )
        }

        var rectSize = 100

        cameraPreview?.viewFinder?.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                MotionEvent.ACTION_UP -> {
                    // Get the MeteringPointFactory from PreviewView
                    val factory = cameraPreview?.viewFinder!!.meteringPointFactory

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory.createPoint(motionEvent.x, motionEvent.y)

                    // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                    val action = FocusMeteringAction.Builder(point).build()

                    // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                    // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                    camera?.cameraControl?.startFocusAndMetering(action)
                    val focusRects = listOf(
                        RectF(
                            motionEvent.x - rectSize,
                            motionEvent.y - rectSize,
                            motionEvent.x + rectSize,
                            motionEvent.y + rectSize
                        )
                    )
                    cameraPreview?.rectOverlayFocus?.let { overlay ->
                        overlay.post {
                            overlay.drawRectBounds(focusRects)
                        }
                    }

                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }
    }

    private var mContext: Context? = context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onDestroy() {
        super.onDestroy()

    }


}
