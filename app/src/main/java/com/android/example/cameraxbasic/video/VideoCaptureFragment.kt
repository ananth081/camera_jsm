/**
 * Copyright 2021 The Android Open Source Project
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

/**
 * Simple app to demonstrate CameraX Video capturing with Recorder ( to local files ), with the
 * following simple control follow:
 *   - user starts capture.
 *   - this app disables all UI selections.
 *   - this app enables capture run-time UI (pause/resume/stop).
 *   - user controls recording with run-time UI, eventually tap "stop" to end.
 *   - this app informs CameraX recording to stop with recording.stop() (or recording.close()).
 *   - CameraX notify this app that the recording is indeed stopped, with the Finalize event.
 *   - this app starts VideoViewer fragment to view the captured result.
 */

package com.android.example.cameraxbasic.video

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.concurrent.futures.await
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.camera.CameraActivity
import com.android.example.cameraxbasic.camera.VideoListActivity
import com.android.example.cameraxbasic.databinding.FragmentVideoCaptureBinding
import com.android.example.cameraxbasic.extensions.animateXTranslation
import com.android.example.cameraxbasic.extensions.toPx
import com.android.example.cameraxbasic.fragments.CameraFragment
import com.android.example.cameraxbasic.save.SaveDialog
import com.android.example.cameraxbasic.utils.CapturedMediaDto
import com.android.example.cameraxbasic.utils.DELETED_LIST_INTENT_KEY
import com.android.example.cameraxbasic.utils.MEDIA_LIST_KEY
import com.android.example.cameraxbasic.video.extensions.getAspectRatio
import com.android.example.cameraxbasic.video.extensions.getAspectRatioString
import com.android.example.cameraxbasic.video.extensions.getNameString
import com.android.example.cameraxbasic.viewmodels.APP_NAME
import com.android.example.cameraxbasic.viewmodels.CaptureViewModel
import com.android.example.cameraxbasic.viewmodels.PUBLISHED
import com.android.example.cameraxbasic.viewmodels.isTablet
import com.example.android.camera.utils.GenericListAdapter
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

class VideoCaptureFragment : Fragment() {

    // UI with ViewBinding
    lateinit var captureViewBinding: FragmentVideoCaptureBinding
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    //private val captureViewBinding get() = _captureViewBinding
    private val captureLiveStatus = MutableLiveData<String>()
    val captureViewModel: CaptureViewModel by activityViewModels()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    private val cameraCapabilities = mutableListOf<CameraCapability>()

    private lateinit var videoCapture: androidx.camera.video.VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent
    private var outputUri: Uri? = null
    private var camera: Camera? = null
    private var fromRetakeScreen: String? = null


    // Camera UI  states and inputs
    enum class UiState {
        IDLE,       // Not recording, all UI controls are active.
        RECORDING,  // Camera is recording, only display Pause/Resume & Stop button.
        FINALIZED,  // Recording just completes, disable all RECORDING UI controls.
        RECOVERY    // For future use.
    }

    private var cameraIndex = 0
    private var qualityIndex = DEFAULT_QUALITY_IDX
    private var audioEnabled = true

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private var enumerationDeferred: Deferred<Unit>? = null

    // main cameraX capture functions
    /**
     *   Always bind preview + video capture use case combinations in this sample
     *   (VideoCapture can work on its own). The function should always execute on
     *   the main thread.
     */
    @SuppressLint("RestrictedApi")
    private suspend fun bindCaptureUsecase() {
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()

        val cameraSelector = getCameraSelector(cameraIndex)

        lensFacing = cameraSelector.lensFacing ?: 0
        // create the user required QualitySelector (video resolution): we know this is
        // supported, a valid qualitySelector will be created.
        val quality = cameraCapabilities[cameraIndex].qualities[qualityIndex]
        val qualitySelector = QualitySelector.from(quality)

        captureViewBinding.previewView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val orientation = this@VideoCaptureFragment.resources.configuration.orientation
            dimensionRatio = quality.getAspectRatioString(
                quality,
                (orientation == Configuration.ORIENTATION_PORTRAIT)
            )
        }

        val preview = Preview.Builder()
            .setTargetAspectRatio(quality.getAspectRatio(quality))
            .build().apply {
                setSurfaceProvider(captureViewBinding.previewView.surfaceProvider)
            }

        // build a recorder, which can:
        //   - record video/audio to MediaStore(only shown here), File, ParcelFileDescriptor
        //   - be used create recording(s) (the recording performs recording)
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                videoCapture,
                preview
            )
            val cameraInfo = videoCapture.camera?.cameraInfo
            cameraInfo?.let { observeCameraState(it) }
        } catch (exc: Exception) {
            // we are on main thread, let's reset the controls on the UI.
            Log.e(TAG, "Use case binding failed", exc)
            resetUIandState("bindToLifecycle failed: $exc")
        }
        enableUI(true)
        setScale()

        captureViewBinding.cancelLayout.flashLight.let { flashLightImg ->
            flashLightImg.isEnabled = true
            var torchState = false
            flashLightImg.setOnClickListener {
                torchState = TorchState.ON == getTorchState()
                if (torchState) {
                    flashLightImg.background = resources.getDrawable(R.drawable.flash_circle_off)
                    setTorchState(false)
                } else {
                    flashLightImg.background = resources.getDrawable(R.drawable.flash_circle_on)
                    setTorchState(true)
                }
            }
        }
        handleCameraZoomClick()
    }

    val contentValues = ContentValues()

    /**
     * Kick start the video recording
     *   - config Recorder to capture to MediaStoreOutput
     *   - register RecordEvent Listener
     *   - apply audio request from user
     *   - start recording!
     * After this function, user could start/pause/resume/stop recording and application listens
     * to VideoRecordEvent for the current recording status.
     */
    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        contentValues.clear()
        contentValues.apply {
            put(MediaStore.MediaColumns.MIME_TYPE, CameraFragment.VIDEO_TYPE)
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$APP_NAME/$PUBLISHED"
                )
            }
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            requireActivity().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        currentRecording = videoCapture.output
            .prepareRecording(requireActivity(), mediaStoreOutput)
            .apply { if (audioEnabled) withAudioEnabled() }
            .start(mainThreadExecutor, captureListener)

        Log.i(TAG, "Recording started")
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            // display the captured video
            lifecycleScope.launch {
                captureViewBinding.videoPreviewImage?.setBackgroundResource(R.drawable.ic_placeholder_img)
                captureViewBinding.videoThumbnailLayout?.visibility = View.VISIBLE
                captureViewBinding.videoPreviewImage.visibility = View.VISIBLE
                captureViewBinding.videoPreviewImage.isEnabled = true


                outputUri = event.outputResults.outputUri

                val capturedMediaDto =
                    outputUri?.let { uri ->
                        captureViewModel.mediaList.add(
                            CapturedMediaDto(contentValues, uri)
                        )
                    }
                lifecycleScope.launch(Dispatchers.Main) {
                    val text = "Save (${captureViewModel.getSize()})"
                    captureViewBinding.saveText.text = text
                }
                context?.let { context ->
                    outputUri?.let { uri ->
                        val createVideoThumb = createVideoThumb(context, uri)
                        createVideoThumb?.let {
                            captureViewBinding.videoPreviewImage.clipToOutline = true
                            captureViewBinding.videoPreviewImage.setImageBitmap(it)
                        }
                    }
                }

//                val intent = Intent(context, VideoActivity::class.java)
//                intent.putExtra("video_uri", event.outputResults.outputUri)
//                startActivity(intent)

//                navController.navigate(
//                   CaptureFragmentDirections.actionCaptureToVideoViewer(
//                        event.outputResults.outputUri
//                    )
//                )
            }
        }
    }

    fun createVideoThumb(context: Context, uri: Uri): Bitmap? {
        try {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, uri)
            return mediaMetadataRetriever.frameAtTime
        } catch (ex: Exception) {
            Toast
                .makeText(context, "Error retrieving bitmap", Toast.LENGTH_SHORT)
                .show()
        }
        return null

    }

    /**
     * Retrieve the asked camera's type(lens facing type). In this sample, only 2 types:
     *   idx is even number:  CameraSelector.LENS_FACING_BACK
     *          odd number:   CameraSelector.LENS_FACING_FRONT
     */
    private fun getCameraSelector(idx: Int): CameraSelector {
        if (cameraCapabilities.size == 0) {
            Log.i(TAG, "Error: This device does not have any camera, bailing out")
            requireActivity().finish()
        }
        return (cameraCapabilities[idx % cameraCapabilities.size].camSelector)
    }

    data class CameraCapability(val camSelector: CameraSelector, val qualities: List<Quality>)

    /**
     * Query and cache this platform's camera capabilities, run only once.
     */
    init {
        enumerationDeferred = lifecycleScope.async {
            whenCreated {
                val provider = ProcessCameraProvider.getInstance(requireContext()).await()

                provider.unbindAll()
                for (camSelector in arrayOf(
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraSelector.DEFAULT_FRONT_CAMERA
                )) {
                    try {
                        // just get the camera.cameraInfo to query capabilities
                        // we are not binding anything here.
                        if (provider.hasCamera(camSelector)) {
                            camera = provider.bindToLifecycle(requireActivity(), camSelector)
                            QualitySelector
                                .getSupportedQualities(camera!!.cameraInfo)
                                .filter { quality ->
                                    listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                                        .contains(quality)
                                }.also {
                                    cameraCapabilities.add(CameraCapability(camSelector, it))
                                }
                        }
                    } catch (exc: java.lang.Exception) {
                        Log.e(TAG, "Camera Face $camSelector is not supported")
                    }
                }
            }
        }
    }

    /**
     * One time initialize for CameraFragment (as a part of fragment layout's creation process).
     * This function performs the following:
     *   - initialize but disable all UI controls except the Quality selection.
     *   - set up the Quality selection recycler view.
     *   - bind use cases to a lifecycle camera, enable UI controls.
     */
    private fun initCameraFragment() {
        initializeUI()
        viewLifecycleOwner.lifecycleScope.launch {
            if (enumerationDeferred != null) {
                enumerationDeferred!!.await()
                enumerationDeferred = null
            }
            initializeQualitySectionsUI()

            bindCaptureUsecase()
        }
    }

    /**
     * Initialize UI. Preview and Capture actions are configured in this function.
     * Note that preview and capture are both initialized either by UI or CameraX callbacks
     * (except the very 1st time upon entering to this fragment in onCreateView()
     */
    @SuppressLint("ClickableViewAccessibility", "MissingPermission", "RestrictedApi")
    private fun initializeUI() {

        if (outputUri == null) {
            captureViewBinding.videoPreviewImage.setBackgroundResource(R.drawable.ic_placeholder_img)
        }
        captureViewBinding.saveText.setOnClickListener {
            val dialog = SaveDialog.newInstance()
            dialog.show(childFragmentManager, SaveDialog::class.java.simpleName)
        }
        captureViewBinding.videoPreviewImage.apply {
            setOnClickListener {
                outputUri?.let {
                    val intent = Intent(context, VideoListActivity::class.java)
                    if (captureViewModel.mediaList.isNotEmpty()) {

                        val list = captureViewModel.mediaList.map {
                            it.uri.toString()
                        }.toList()
                        intent.putStringArrayListExtra(MEDIA_LIST_KEY, ArrayList(list))
                        startForResult.launch(intent)
                    }
                    // intent.putExtra("video_uri", it)
                    //startActivity(intent)
                }
                /*   cameraIndex = (cameraIndex + 1) % cameraCapabilities.size
                   // camera device change is in effect instantly:
                   //   - reset quality selection
                   //   - restart preview
                   qualityIndex = DEFAULT_QUALITY_IDX
                   initializeQualitySectionsUI()
                   enableUI(false)
                   viewLifecycleOwner.lifecycleScope.launch {
                       bindCaptureUsecase()
                   }*/
            }
            //isEnabled = true
        }

        captureViewBinding.dualCamera?.let {
            it.setOnClickListener {
                cameraIndex = (cameraIndex + 1) % cameraCapabilities.size
                // camera device change is in effect instantly:
                //   - reset quality selection
                //   - restart preview
                qualityIndex = DEFAULT_QUALITY_IDX
                initializeQualitySectionsUI()
                enableUI(false)
                viewLifecycleOwner.lifecycleScope.launch {
                    bindCaptureUsecase()
                }

                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    captureViewBinding.cancelLayout?.flashLight?.isEnabled = false
                    captureViewBinding.cancelLayout?.flashLight?.visibility = View.INVISIBLE
                    captureViewBinding?.cameraZoom?.visibility = View.GONE
                } else if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    captureViewBinding.cancelLayout?.flashLight?.isEnabled = true
                    captureViewBinding.cancelLayout?.flashLight?.isVisible = true
                    captureViewBinding.cancelLayout?.flashLight?.visibility = View.VISIBLE
                    captureViewBinding?.cameraZoom?.visibility = View.VISIBLE
                }
            }
        }

        // audioEnabled by default is disabled.
        captureViewBinding.audioSelection.isChecked = audioEnabled
        captureViewBinding.audioSelection.setOnClickListener {
            audioEnabled = captureViewBinding.audioSelection.isChecked
        }

        // React to user touching the capture button
        captureViewBinding.captureButton.apply {
            setOnClickListener {
                if (!this@VideoCaptureFragment::recordingState.isInitialized ||
                    recordingState is VideoRecordEvent.Finalize
                ) {
                    enableUI(false)  // Our eventListener will turn on the Recording UI.
                    startRecording()
                } else {
                    when (recordingState) {
                        is VideoRecordEvent.Start -> {
                            currentRecording?.pause()
                            captureViewBinding.recordLayout?.visibility = View.VISIBLE
                        }
                        is VideoRecordEvent.Pause -> currentRecording?.resume()
                        is VideoRecordEvent.Resume -> currentRecording?.pause()
                        else -> throw IllegalStateException("recordingState in unknown state")
                    }
                }
            }
            isEnabled = false
        }

        captureViewBinding.stopButton.apply {
            setOnClickListener {
                captureViewBinding.saveText.visibility = View.VISIBLE
                captureViewBinding.videoThumbnailLayout?.visibility = View.VISIBLE
                // stopping: hide it after getting a click before we go to viewing fragment
                captureViewBinding.recordLayout?.visibility = View.INVISIBLE
                if (currentRecording == null || recordingState is VideoRecordEvent.Finalize) {
                    return@setOnClickListener
                }

                val recording = currentRecording
                if (recording != null) {
                    recording.stop()
                    currentRecording = null
                }
                captureViewBinding.captureButton.setImageResource(R.drawable.ic_record)
            }
            // ensure the stop button is initialized disabled & invisible
            visibility = View.VISIBLE
            isEnabled = false
        }

        captureViewBinding.pauseButton?.apply {
            setOnClickListener {
                // stopping: hide it after getting a click before we go to viewing fragment
                Log.d(TAG, "recordingStats: ===" + recordingState.getNameString())
                currentRecording?.pause()

                recordingState?.let {
                    if (it.getNameString() == "Paused") {
                        currentRecording?.resume()
                    }
                }
            }
            // ensure the stop button is initialized disabled & invisible
            visibility = View.VISIBLE
        }

        captureLiveStatus.observe(viewLifecycleOwner) {
            captureViewBinding.captureStatus.apply {
                post { text = it }
            }
        }

        //todo handle cancel click to onBackPress
//        captureViewBinding.cancel.setOnClickListener {
//            if (captureViewModel.mediaList.isNotEmpty()) {
//                captureViewModel.deleteUnsavedMedia(requireContext())
//            } else {
//                handleCancelClicked()
//            }
//        }

        captureViewBinding.photoBtn?.setOnTouchListener { v, event ->
            captureViewModel.deleteUnsavedMediaWithoutNotify(requireContext())
            (activity as CameraActivity).showPhotoFragment()
            true
        }

        captureViewBinding.videoBtn?.setOnTouchListener { v, event ->
            (activity as CameraActivity).showVideoFragment()
            true
        }
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
        captureViewBinding.saveText?.visibility = View.GONE
        captureViewBinding.videoThumbnailLayout?.visibility = View.INVISIBLE
    }


    private fun handleCancelClicked() {
        captureViewModel.reset()
        activity?.finish()
    }

    /**
     * UpdateUI according to CameraX VideoRecordEvent type:
     *   - user starts capture.
     *   - this app disables all UI selections.
     *   - this app enables capture run-time UI (pause/resume/stop).
     *   - user controls recording with run-time UI, eventually tap "stop" to end.
     *   - this app informs CameraX recording to stop with recording.stop() (or recording.close()).
     *   - CameraX notify this app that the recording is indeed stopped, with the Finalize event.
     *   - this app starts VideoViewer fragment to view the captured result.
     */
    private fun updateUI(event: VideoRecordEvent) {
        val state = if (event is VideoRecordEvent.Status) recordingState.getNameString()
        else event.getNameString()
        when (event) {
            is VideoRecordEvent.Status -> {
                // placeholder: we update the UI with new status after this when() block,
                // nothing needs to do here.
            }
            is VideoRecordEvent.Start -> {
                showUI(UiState.RECORDING, event.getNameString())
            }
            is VideoRecordEvent.Finalize -> {
                showUI(UiState.FINALIZED, event.getNameString())
            }
            is VideoRecordEvent.Pause -> {
                captureViewBinding.pauseButton?.setImageResource(R.drawable.ic_resume)
            }
            is VideoRecordEvent.Resume -> {
                captureViewBinding.pauseButton?.setImageResource(R.drawable.baseline_pause_24)
            }
        }

        setVideoRecordingStatus(event)
        Log.i(TAG, "recording event: ${captureLiveStatus.value}")
    }

    private fun setVideoRecordingStatus(event: VideoRecordEvent) {
        val stats = event.recordingStats
        stats.recordedDurationNanos
        val size = stats.numBytesRecorded / 1000
        val seconds = TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        val f: NumberFormat = DecimalFormat("00")
        val hours = (seconds / 3600)
        val minutes = (seconds % 3600 / 60)
        val sec = (seconds % 60)
        captureLiveStatus.value = formatTime(sec, minutes, hours)
    }

    private fun formatTime(seconds: Long, minutes: Long, hours: Long): String? {
        return String.format("%02d", hours) + " : " + String.format(
            "%02d",
            minutes
        ) + " : " + String.format("%02d", seconds)
    }

    /**
     * Enable/disable UI:
     *    User could select the capture parameters when recording is not in session
     *    Once recording is started, need to disable able UI to avoid conflict.
     */
    private fun enableUI(enable: Boolean) {
        arrayOf(
            captureViewBinding.videoPreviewImage,
            captureViewBinding.captureButton,
            captureViewBinding.recordLayout,
            captureViewBinding.audioSelection,
            captureViewBinding.qualitySelection
        ).forEach {
            it?.isEnabled = enable
        }
        // disable the camera button if no device to switch
        if (cameraCapabilities.size <= 1) {
            captureViewBinding.videoThumbnailLayout?.isEnabled = false
        }
        // disable the resolution list if no resolution to switch
        if (cameraCapabilities[cameraIndex].qualities.size <= 1) {
            captureViewBinding.qualitySelection.apply { isEnabled = false }
        }
    }

    /**
     * initialize UI for recording:
     *  - at recording: hide audio, qualitySelection,change camera UI; enable stop button
     *  - otherwise: show all except the stop button
     */
    private fun showUI(state: UiState, status: String = "") {
        captureViewBinding.let {
            when (state) {
                UiState.IDLE -> {
                    it.captureButton.setImageResource(R.drawable.ic_record)
                    it.recordLayout?.visibility = View.INVISIBLE
                    it.videoPreviewImage.visibility = View.VISIBLE
                    it.audioSelection.visibility = View.VISIBLE
                    it.qualitySelection.visibility = View.VISIBLE
                }
                UiState.RECORDING -> {
                    it.videoPreviewImage.visibility = View.INVISIBLE
                    it.audioSelection.visibility = View.INVISIBLE
                    it.qualitySelection.visibility = View.INVISIBLE
                    // it.saveText.visibility = View.GONE
                    it.dualCamera?.visibility = View.GONE
                    it.cancelLayout.flashLight.visibility = View.GONE
                    it.cancelLayout.cancel.visibility = View.GONE
                    it.pauseButton.setImageResource(R.drawable.baseline_pause_24)
                    // it.captureButton.setImageResource(R.drawable.ic_pause)
                    // it.captureButtonFrame?.visibility = View.INVISIBLE
                    // it.recordLayout.visibility = View.VISIBLE
                    it.stopButton.isEnabled = true
                    it.captureStatus.visibility = View.VISIBLE

                    handleCaptureAndRecordLayoutAnimation(false)
                }
                UiState.FINALIZED -> {
                    // it.captureButtonFrame?.visibility = View.VISIBLE
                    it.captureButton.isEnabled = true
                    it.captureButton.setImageResource(R.drawable.ic_record)
                    // it.saveText.visibility = View.VISIBLE
                    it.dualCamera?.visibility = View.VISIBLE
                    it.cancelLayout.flashLight.visibility = View.VISIBLE
                    it.cancelLayout.cancel.visibility = View.VISIBLE

                    // it.recordLayout.visibility = View.INVISIBLE
                    it.captureStatus.visibility = View.GONE
                    handleCaptureAndRecordLayoutAnimation(true)
                }
                else -> {
                    val errorMsg = "Error: showUI($state) is not supported"
                    Log.e(TAG, errorMsg)
                    return
                }
            }
            it.captureStatus.text = status
        }
    }

    private fun handleCaptureAndRecordLayoutAnimation(isShowCaptureButton: Boolean) {
        if (isShowCaptureButton) {
            captureViewBinding.captureButtonFrame?.animate()?.alpha(1f)?.setDuration(500)?.start()
            captureViewBinding.videoThumbnailLayout?.animate()?.alpha(1f)?.setDuration(500)?.start()
            captureViewBinding.videoPreviewImage.animate()?.alpha(1f)?.setDuration(500)?.start()
            captureViewBinding.saveText.animate()?.alpha(1f)?.setDuration(500)?.start()

            captureViewBinding.recordLayout.animate()?.alpha(0f)?.setDuration(500)?.start()
        } else {
            captureViewBinding.recordLayout.alpha = 0f
            captureViewBinding.recordLayout.visibility = View.VISIBLE

            captureViewBinding.captureButtonFrame?.animate()?.alpha(0f)?.setDuration(500)?.start()
            captureViewBinding.videoThumbnailLayout?.animate()?.alpha(0f)?.setDuration(500)?.start()
            captureViewBinding.saveText.animate()?.alpha(0f)?.setDuration(500)?.start()

            captureViewBinding.recordLayout.animate()?.alpha(1f)?.setDuration(500)?.start()
        }
    }

    /**
     * ResetUI (restart):
     *    in case binding failed, let's give it another change for re-try. In future cases
     *    we might fail and user get notified on the status
     */
    private fun resetUIandState(reason: String) {
        enableUI(true)
        showUI(UiState.IDLE, reason)

        cameraIndex = 0
        qualityIndex = DEFAULT_QUALITY_IDX
        audioEnabled = false
        captureViewBinding.audioSelection.isChecked = audioEnabled
        initializeQualitySectionsUI()
    }

    /**
     *  initializeQualitySectionsUI():
     *    Populate a RecyclerView to display camera capabilities:
     *       - one front facing
     *       - one back facing
     *    User selection is saved to qualityIndex, will be used
     *    in the bindCaptureUsecase().
     */
    private fun initializeQualitySectionsUI() {
        val selectorStrings = cameraCapabilities[cameraIndex].qualities.map {
            it.getNameString()
        }
        // create the adapter to Quality selection RecyclerView
        captureViewBinding.qualitySelection.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = GenericListAdapter(
                selectorStrings,
                itemLayoutId = R.layout.video_quality_item
            ) { holderView, qcString, position ->

                holderView.apply {
                    findViewById<TextView>(R.id.qualityTextView)?.text = qcString
                    // select the default quality selector
                    isSelected = (position == qualityIndex)
                }

                holderView.setOnClickListener { view ->
                    if (qualityIndex == position) return@setOnClickListener

                    captureViewBinding.qualitySelection.let {
                        // deselect the previous selection on UI.
                        it.findViewHolderForAdapterPosition(qualityIndex)
                            ?.itemView
                            ?.isSelected = false
                    }
                    // turn on the new selection on UI.
                    view.isSelected = true
                    qualityIndex = position

                    // rebind the use cases to put the new QualitySelection in action.
                    enableUI(false)
                    viewLifecycleOwner.lifecycleScope.launch {
                        bindCaptureUsecase()
                    }
                }
            }
            isEnabled = false
        }
    }

    // System function implementations
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        captureViewBinding = FragmentVideoCaptureBinding.inflate(inflater, container, false)
        return captureViewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        captureViewBinding.saveText.visibility = View.INVISIBLE
        initCameraFragment()
        captureViewModel.cancelCommunicator.observe(viewLifecycleOwner) {
            handleCancelClicked()
        }

    }

    override fun onDestroyView() {
        // captureViewBinding = null
        super.onDestroyView()
    }

    companion object {
        // default Quality selection if no input from UI
        const val DEFAULT_QUALITY_IDX = 0
        val TAG: String = VideoCaptureFragment::class.java.simpleName
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private fun setScale() {
        val listener: ScaleGestureDetector.OnScaleGestureListener =
            object : ScaleGestureDetector.OnScaleGestureListener {
                @SuppressLint("RestrictedApi")
                override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
                    videoCapture.camera?.let {
                        val f: ZoomState? = it.cameraInfo.zoomState.value
                        val scale: Float = scaleGestureDetector.scaleFactor
                        val zoomRatio = scale * f?.zoomRatio!!
                        camera?.cameraControl?.setZoomRatio(zoomRatio)
                        it.cameraControl.setZoomRatio(zoomRatio)
                    }

                    return true
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                }

            }
        val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)

//        captureViewBinding.previewView.setOnTouchListener { view, motionEvent ->
//            scaleGestureDetector.onTouchEvent(
//                motionEvent
//            )
//        }
        val handler = Handler(Looper.getMainLooper())

        var rectSize = 100
        captureViewBinding.previewView.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            scaleGestureDetector.onTouchEvent(motionEvent)
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    captureViewBinding.rectOverlayFocus?.visibility = View.VISIBLE
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    // Get the MeteringPointFactory from PreviewView
                    handler.postDelayed({
                        captureViewBinding.rectOverlayFocus?.visibility = View.GONE
                    },500)
                    val factory = captureViewBinding.previewView?.meteringPointFactory

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory?.createPoint(motionEvent.x, motionEvent.y)

                    // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                    val action = point?.let { FocusMeteringAction.Builder(it).build() }

                    // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                    // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                    action?.let { camera?.cameraControl?.startFocusAndMetering(it) }
                    val focusRects = listOf(
                        RectF(
                            motionEvent.x - rectSize,
                            motionEvent.y - rectSize,
                            motionEvent.x + rectSize,
                            motionEvent.y + rectSize
                        )
                    )
                    captureViewBinding.rectOverlayFocus?.let { overlay ->
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

    //
//    @SuppressLint("RestrictedApi")
//    fun setCameraZoomLevels(fl: Float) {
//        videoCapture.camera?.let {
//            camera?.cameraControl?.setZoomRatio(fl)
//        }
//    }
    private val startForResults =
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
                //hideRetakeUiControls()
                val list = intent?.getStringArrayListExtra(DELETED_LIST_INTENT_KEY)
                if (list != null && list.isNotEmpty())
                    captureViewModel.deleteList(list, requireContext())
                // Handle the Intent
            }
        }

    @SuppressLint("RestrictedApi")
    fun setLinearZoom(fl: Float) {
        videoCapture.camera?.let {
            it.cameraControl.setLinearZoom(fl)
        }
    }

    @SuppressLint("RestrictedApi")
    fun getTorchState(): Int? {
        var torchState: Int? = 0
        videoCapture.camera?.let {
            torchState = it.cameraInfo.torchState.value
        }
        return torchState
    }

    @SuppressLint("RestrictedApi")
    fun setTorchState(state: Boolean) {
        videoCapture.camera?.let {
            it.cameraControl?.enableTorch(state)
        }
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        // Ask the user to close other camera apps
//                        Toast.makeText(
//                            context,
//                            "CameraState: Pending Open",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.Type.OPENING -> {
                        // Show the Camera UI
//                        Toast.makeText(
//                            context,
//                            "CameraState: Opening",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.Type.OPEN -> {
                        // Setup Camera resources and begin processing
//                        Toast.makeText(
//                            context,
//                            "CameraState: Open",
//                            Toast.LENGTH_SHORT
//                        ).show()
                        captureViewBinding.placeHolderLayout.postDelayed({
                            captureViewBinding?.placeHolderLayout?.visibility = View.GONE
                        }, 500)

                    }
                    CameraState.Type.CLOSING -> {
                        // Close camera UI
//                        Toast.makeText(
//                            context,
//                            "CameraState: Closing",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.Type.CLOSED -> {
                        // Free camera resources
//                        Toast.makeText(
//                            context,
//                            "CameraState: Closed",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
//                        Toast.makeText(
//                            context,
//                            "Stream config error",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the
                        // camera
//                        Toast.makeText(
//                            context,
//                            "Camera in use",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
//                        Toast.makeText(
//                            context,
//                            "Max cameras in use",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
//                        Toast.makeText(
//                            context,
//                            "Other recoverable error",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
//                        Toast.makeText(
//                            context,
//                            "Camera disabled",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
//                        Toast.makeText(
//                            context,
//                            "Fatal error",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
//                        Toast.makeText(
//                            context,
//                            "Do not disturb mode enabled",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                }
            }
        }
    }

    private fun handleCameraZoomClick() {
        if(isTablet()){
            captureViewBinding.zoomSlider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    camera?.cameraControl?.setLinearZoom(progress / 100.toFloat())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }else {
            captureViewBinding.cameraZoom?.setOnClickListener {
                captureViewBinding.cameraZoom?.post {

                    val marginHorizontal = 1.toPx(requireContext()).toFloat()
                    var isAtStart = true

                    val bgWidth = captureViewBinding.zoomToggleBg?.width?.plus(marginHorizontal)
                        ?: marginHorizontal
                    val translationXEndVal =
                        (captureViewBinding.cameraZoom?.width?.minus(bgWidth)) ?: 0f

                    val startValue: Float
                    val endValue: Float

                    if ((captureViewBinding.zoomToggleBg?.translationX ?: 0f) > marginHorizontal) {
                        // translated to end
                        startValue = translationXEndVal
                        endValue = marginHorizontal
                        isAtStart = false
                    } else {
                        // is at start
                        startValue = marginHorizontal
                        endValue = translationXEndVal
                        isAtStart = true
                    }

                    captureViewBinding.zoomToggleBg?.animateXTranslation(
                        startValue,
                        endValue,
                        duration = 400,
                        onAnimEnded = {
                            if (isAtStart) {
                                setCameraZoomLevels(0.7f)
                                (captureViewBinding.tvZoomTwo as TextView).setTextColor(
                                    resources.getColor(
                                        android.R.color.black,
                                        null
                                    )
                                )
                                (captureViewBinding.tvZoomOne as TextView).setTextColor(
                                    resources.getColor(
                                        android.R.color.white,
                                        null
                                    )
                                )
                            } else {
                                setCameraZoomLevels(0.3f)
                                (captureViewBinding.tvZoomOne as TextView).setTextColor(
                                    resources.getColor(
                                        android.R.color.black,
                                        null
                                    )
                                )
                                (captureViewBinding.tvZoomTwo as TextView).setTextColor(
                                    resources.getColor(
                                        android.R.color.white,
                                        null
                                    )
                                )
                            }
                        })
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    fun setCameraZoomLevels(zoomValue: Float) {
        videoCapture.camera?.let {
            it.cameraControl.setLinearZoom(zoomValue)
        }

    }
}
