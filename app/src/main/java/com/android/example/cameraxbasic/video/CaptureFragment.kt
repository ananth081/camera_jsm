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
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.concurrent.futures.await
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Consumer
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.example.cameraxbasic.R
import com.android.example.cameraxbasic.camera.CameraActivity
import com.android.example.cameraxbasic.camera.VideoActivity
import com.android.example.cameraxbasic.databinding.FragmentCaptureBinding
import com.android.example.cameraxbasic.fragments.CameraFragment
import com.android.example.cameraxbasic.video.extensions.getAspectRatio
import com.android.example.cameraxbasic.video.extensions.getAspectRatioString
import com.android.example.cameraxbasic.video.extensions.getNameString
import com.example.android.camera.utils.GenericListAdapter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

class CaptureFragment : Fragment() {

    // UI with ViewBinding
    lateinit var captureViewBinding: FragmentCaptureBinding
    //private val captureViewBinding get() = _captureViewBinding
    private val captureLiveStatus = MutableLiveData<String>()

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

    // Camera UI  states and inputs
    enum class UiState {
        IDLE,       // Not recording, all UI controls are active.
        RECORDING,  // Camera is recording, only display Pause/Resume & Stop button.
        FINALIZED,  // Recording just completes, disable all RECORDING UI controls.
        RECOVERY    // For future use.
    }

    private var cameraIndex = 0
    private var qualityIndex = DEFAULT_QUALITY_IDX
    private var audioEnabled = false

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

        // create the user required QualitySelector (video resolution): we know this is
        // supported, a valid qualitySelector will be created.
        val quality = cameraCapabilities[cameraIndex].qualities[qualityIndex]
        val qualitySelector = QualitySelector.from(quality)

        captureViewBinding.previewView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val orientation = this@CaptureFragment.resources.configuration.orientation
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

        captureViewBinding.flashLight.let { flashLightImg ->
            flashLightImg.isEnabled = true
            var torchState = false
            flashLightImg.setOnClickListener {
                torchState = TorchState.ON == getTorchState()
                if (torchState) {
                    flashLightImg.setImageResource(R.drawable.flash_circle_1)
                    setTorchState(false)
                } else {
                    flashLightImg.setImageResource(R.drawable.flash_circle_on)
                    setTorchState(true)
                }
            }
        }
    }

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
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
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
                captureViewBinding.cameraButton.setBackgroundResource(R.drawable.ic_placeholder_img)
                captureViewBinding.cameraButton.visibility = View.VISIBLE
                captureViewBinding.cameraButton.isEnabled = true


                outputUri = event.outputResults.outputUri

                context?.let { context ->
                    outputUri?.let { uri ->
                        val createVideoThumb = createVideoThumb(context, uri)
                        createVideoThumb?.let {
                            captureViewBinding.cameraButton.clipToOutline = true
                            captureViewBinding.cameraButton.setImageBitmap(it)
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
    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    private fun initializeUI() {
        if (outputUri == null) {
            captureViewBinding.cameraButton.setBackgroundResource(R.drawable.ic_placeholder_img)
        }

        captureViewBinding.cameraButton.apply {
            setOnClickListener {
                outputUri?.let {
                    val intent = Intent(context, VideoActivity::class.java)
                    intent.putExtra("video_uri", it)
                    startActivity(intent)
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
                if (!this@CaptureFragment::recordingState.isInitialized ||
                    recordingState is VideoRecordEvent.Finalize
                ) {
                    enableUI(false)  // Our eventListener will turn on the Recording UI.
                    startRecording()
                } else {
                    when (recordingState) {
                        is VideoRecordEvent.Start -> {
                            currentRecording?.pause()
                            captureViewBinding.stopButton.visibility = View.VISIBLE
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
                // stopping: hide it after getting a click before we go to viewing fragment
                captureViewBinding.stopButton.visibility = View.INVISIBLE
                if (currentRecording == null || recordingState is VideoRecordEvent.Finalize) {
                    return@setOnClickListener
                }

                val recording = currentRecording
                if (recording != null) {
                    recording.stop()
                    currentRecording = null
                }
                captureViewBinding.captureButton.setImageResource(R.drawable.ic_shutter_normal)
            }
            // ensure the stop button is initialized disabled & invisible
            visibility = View.INVISIBLE
            isEnabled = false
        }

        captureLiveStatus.observe(viewLifecycleOwner) {
            captureViewBinding.captureStatus.apply {
                post { text = it }
            }
        }

        captureViewBinding.cancel.setOnClickListener {
            activity?.finish()
        }

        captureViewBinding.cameraZoomText05?.setOnClickListener {
            camera?.cameraControl?.setLinearZoom(0.05f)
            captureViewBinding.cameraZoomText05?.text = "1x"
            captureViewBinding.cameraZoomText0?.setBackgroundResource(R.drawable.zoom_button_bg_inactive)
            context?.getColor(R.color.ic_white)?.let { it1 -> captureViewBinding.cameraZoomText0?.setTextColor(it1) }
            context?.resources?.getColor(R.color.txBlack)?.let { it1 -> captureViewBinding.cameraZoomText05?.setTextColor(it1) }
            it.setBackgroundResource(R.drawable.zoom_button_bg)
        }

        captureViewBinding.cameraZoomText0?.setOnClickListener {
            camera?.cameraControl?.setLinearZoom(1f)
            captureViewBinding.cameraZoomText0?.text = "2x"
            captureViewBinding.cameraZoomText05?.setBackgroundResource(R.drawable.zoom_button_bg_inactive)
            context?.getColor(R.color.ic_white)?.let { it1 -> captureViewBinding.cameraZoomText05?.setTextColor(it1) }
            context?.resources?.getColor(R.color.txBlack)?.let { it1 -> captureViewBinding.cameraZoomText0?.setTextColor(it1) }
            it.setBackgroundResource(R.drawable.zoom_button_bg)
        }

        captureViewBinding.photoBtn?.setOnTouchListener { v, event ->
            captureViewBinding?.photoBtn?.background =
                ResourcesCompat.getDrawable(resources, R.drawable.bg_photo_text, context?.theme)
            captureViewBinding?.photoBtn?.setTextColor(resources.getColor(R.color.ic_white))
            captureViewBinding?.videoBtn?.setBackgroundColor(resources.getColor(R.color.black_overlay))
            captureViewBinding?.videoBtn?.setTextColor(resources.getColor(R.color.ic_white))
            (activity as CameraActivity).showPhotoFragment()
            true
        }

//        captureViewBinding?.videoBtn?.setOnTouchListener { v, event ->
//            captureViewBinding?.photoBtn?.setBackgroundColor(resources.getColor(R.color.black_overlay))
//            captureViewBinding?.photoBtn?.setTextColor(resources.getColor(R.color.ic_white))
//            captureViewBinding?.videoBtn?.background =
//                ResourcesCompat.getDrawable(resources, R.drawable.bg_photo_text, context?.theme)
//            captureViewBinding?.videoBtn?.setTextColor(resources.getColor(R.color.ic_white))
//            (activity as CameraActivity).showVideoFragment()
//            true
//        }

        captureLiveStatus.value = getString(R.string.Idle)
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
                captureViewBinding.captureButton.setImageResource(R.drawable.ic_resume)
            }
            is VideoRecordEvent.Resume -> {
                captureViewBinding.captureButton.setImageResource(R.drawable.ic_pause)
            }
        }

        val stats = event.recordingStats
        val size = stats.numBytesRecorded / 1000
        val time = TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        var text = "${state}: recorded ${size}KB, in ${time}second"
        if (event is VideoRecordEvent.Finalize)
            text = "${text}\nFile saved to: ${event.outputResults.outputUri}"

        captureLiveStatus.value = text
        Log.i(TAG, "recording event: $text")
    }

    /**
     * Enable/disable UI:
     *    User could select the capture parameters when recording is not in session
     *    Once recording is started, need to disable able UI to avoid conflict.
     */
    private fun enableUI(enable: Boolean) {
        arrayOf(
            captureViewBinding.cameraButton,
            captureViewBinding.captureButton,
            captureViewBinding.stopButton,
            captureViewBinding.audioSelection,
            captureViewBinding.qualitySelection
        ).forEach {
            it.isEnabled = enable
        }
        // disable the camera button if no device to switch
        if (cameraCapabilities.size <= 1) {
            captureViewBinding.cameraButton.isEnabled = false
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
    private fun showUI(state: UiState, status: String = "idle") {
        captureViewBinding.let {
            when (state) {
                UiState.IDLE -> {
                    it.captureButton.setImageResource(R.drawable.ic_shutter_normal)
                    it.stopButton.visibility = View.INVISIBLE

                    it.cameraButton.visibility = View.VISIBLE
                    it.audioSelection.visibility = View.VISIBLE
                    it.qualitySelection.visibility = View.VISIBLE
                }
                UiState.RECORDING -> {
                    it.cameraButton.visibility = View.INVISIBLE
                    it.audioSelection.visibility = View.INVISIBLE
                    it.qualitySelection.visibility = View.INVISIBLE
                    it.saveText.visibility = View.GONE
                    it.dualCamera.visibility = View.GONE
                    it.captureButton.setImageResource(R.drawable.ic_pause)
                    it.captureButton.isEnabled = true
                    it.stopButton.visibility = View.VISIBLE
                    it.stopButton.isEnabled = true
                }
                UiState.FINALIZED -> {
                    it.captureButton.setImageResource(R.drawable.ic_shutter_normal)
                    it.saveText.visibility = View.VISIBLE
                    it.dualCamera.visibility = View.VISIBLE
                    it.stopButton.visibility = View.INVISIBLE
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
        captureViewBinding = FragmentCaptureBinding.inflate(inflater, container, false)
        return captureViewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCameraFragment()
    }

    override fun onDestroyView() {
       // captureViewBinding = null
        super.onDestroyView()
    }

    companion object {
        // default Quality selection if no input from UI
        const val DEFAULT_QUALITY_IDX = 0
        val TAG: String = CaptureFragment::class.java.simpleName
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
                        if (activity != null && activity is CameraActivity) {
                            (activity as CameraActivity).updateZoomText(zoomRatio)
                        }
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

        captureViewBinding.previewView.setOnTouchListener { view, motionEvent ->
            scaleGestureDetector.onTouchEvent(
                motionEvent
            )
        }
    }
//
//    @SuppressLint("RestrictedApi")
//    fun setCameraZoomLevels(fl: Float) {
//        videoCapture.camera?.let {
//            camera?.cameraControl?.setZoomRatio(fl)
//        }
//    }

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
                        },300)

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
}