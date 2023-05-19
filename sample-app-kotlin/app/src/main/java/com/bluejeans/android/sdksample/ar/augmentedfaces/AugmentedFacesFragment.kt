package com.bluejeans.android.sdksample.ar.augmentedfaces

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bjnclientcore.media.BJNVideoFrame
import com.bjnclientcore.media.CustomFrameData
import com.bjnclientcore.media.FrameFormat
import com.bjnclientcore.media.FrameOrientation
import com.bjnclientcore.media.VideoSource
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.android.sdksample.ar.common.helpers.ARCoreUtils.getBitmapFromSurface
import com.bluejeans.android.sdksample.ar.common.helpers.DisplayRotationHelper
import com.bluejeans.android.sdksample.ar.common.rendering.BackgroundRenderer
import com.bluejeans.android.sdksample.ar.common.rendering.ObjectRenderer
import com.bluejeans.android.sdksample.databinding.FragmentAugmentedFacesBinding
import com.bluejeans.bluejeanssdk.permission.PermissionService
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Config.AugmentedFaceMode
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.EnumSet
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A simple [Fragment] subclass.
 * Use the [AugmentedFacesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AugmentedFacesFragment(private val activity: Activity) : DialogFragment(), GLSurfaceView.Renderer {

    private var session: Session? = null
    private var displayRotationHelper: DisplayRotationHelper? = null
    private var message: String? = null

    private var _binding: FragmentAugmentedFacesBinding? = null
    private val binding get() = _binding!!

    private var installRequested = false

    private val permissionService = SampleApplication.blueJeansSDK.permissionService
    private val customVideoService = SampleApplication.blueJeansSDK.customVideoSourceService

    private val backgroundRenderer: BackgroundRenderer = BackgroundRenderer()
    private val augmentedFaceRenderer = AugmentedFaceRenderer()
    private val noseObject: ObjectRenderer = ObjectRenderer()
    private val rightEarObject: ObjectRenderer = ObjectRenderer()
    private val leftEarObject: ObjectRenderer = ObjectRenderer()

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val noseMatrix = FloatArray(16)
    private val rightEarMatrix = FloatArray(16)
    private val leftEarMatrix = FloatArray(16)
    private val DEFAULT_COLOR = floatArrayOf(0f, 0f, 0f, 0f)

    private var width = 640
    private var height = 480

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        customVideoService.setVideoSource(VideoSource.Custom)
        context?.let { displayRotationHelper = DisplayRotationHelper(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAugmentedFacesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.surfaceView.preserveEGLContextOnPause = true
        binding.surfaceView.setEGLContextClientVersion(2)
        binding.surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.

        binding.surfaceView.setRenderer(this)
        binding.surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        binding.surfaceView.setWillNotDraw(false)
    }

    override fun onResume() {
        super.onResume()
        if (permissionService.hasPermission(PermissionService.Permission.Camera)) {
            context?.let { ctx ->
                initARSession(ctx, this.activity)
                session?.let { session ->
                    try {
                        val cameraConfigFilter = CameraConfigFilter(session)
                        cameraConfigFilter.facingDirection = CameraConfig.FacingDirection.FRONT
                        val cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter)
                        if (cameraConfigs.isNotEmpty()) {
                            // Element 0 contains the camera config that best matches the session feature
                            // and filter settings.
                            session.cameraConfig = cameraConfigs[0]
                            width = cameraConfigs[0].imageSize.width
                            height = cameraConfigs[0].imageSize.height
                        } else {
                            message = "This device does not have a front-facing (selfie) camera"
                            Log.e(TAG, message!!)
                        }
                        configureSession()

                    } catch (e: UnavailableArcoreNotInstalledException) {
                        Log.e(TAG, "AR core exception: ${e.message}")
                        message = "Please install ARCore"
                    } catch (e: UnavailableApkTooOldException) {
                        Log.e(TAG, "AR core exception: ${e.message}")
                        message = "Please update ARCore"
                    } catch (e: UnavailableSdkTooOldException) {
                        Log.e(TAG, "AR core exception: ${e.message}")
                        message = "Please update this app"
                    } catch (e: UnavailableDeviceNotCompatibleException) {
                        Log.e(TAG, "AR core exception: ${e.message}")
                        message = "This device does not support AR"
                    } catch (e: Exception) {
                        Log.e(TAG, "AR core exception: ${e.message}")
                        message = "Failed to create AR session"
                    }

                    if (message != null) {
                        showToastMessage(message!!)
                        return
                    }
                }

                try {
                    session!!.resume()
                } catch (e: CameraNotAvailableException) {
                    showToastMessage("Camera not available. Try restarting the app.")
                    session = null
                    return
                }
            }

            binding.surfaceView.onResume()
            displayRotationHelper?.onResume()
        } else {
            return
        }
    }

    override fun onPause() {
        super.onPause()
        if (session != null) {
            displayRotationHelper?.onPause()
            binding.surfaceView.onPause()
            session?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch(Dispatchers.IO) {
            session?.close()
            session = null
        }
        customVideoService.setVideoSource(VideoSource.Camera)
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        try {
            context?.let {
                backgroundRenderer.createOnGlThread(it)
                augmentedFaceRenderer.createOnGlThread(it, MODEL_FRECKLES)
                augmentedFaceRenderer.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
                noseObject.createOnGlThread(it, MODEL_NOSE, MODEL_NOSE_FUR)
                noseObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
                noseObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending)
                rightEarObject.createOnGlThread(it, MODEL_FOREHEAD_RIGHT, MODEL_EAR_FUR)
                rightEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
                rightEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending)
                leftEarObject.createOnGlThread(it, MODEL_FOREHEAD_LEFT, MODEL_EAR_FUR)
                leftEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
                leftEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }
    }

    override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
        Log.i(TAG, "Surface changed: ${p1}x$p2")
        displayRotationHelper!!.onSurfaceChanged(p1, p2)
        GLES20.glViewport(0, 0, p1, p2)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (session == null) {
            return
        }

        displayRotationHelper!!.updateSessionIfNeeded(session!!)

        try {
            session!!.setCameraTextureName(backgroundRenderer.textureId)

            val frame = session!!.update()
            val camera = frame.camera

            val projectionMatrix = FloatArray(16)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

            val viewMatrix = FloatArray(16)
            camera.getViewMatrix(viewMatrix, 0)

            val colorCorrectionRgba = FloatArray(4)
            frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)

            backgroundRenderer.draw(frame)
            val faces = session!!.getAllTrackables(
                AugmentedFace::class.java
            )
            for (face in faces) {
                if (face.trackingState != TrackingState.TRACKING) {
                    break
                }
                val scaleFactor = 1.0f
                GLES20.glDepthMask(false)
                val modelMatrix = FloatArray(16)
                face.centerPose.toMatrix(modelMatrix, 0)
                augmentedFaceRenderer.draw(
                    projectionMatrix, viewMatrix, modelMatrix, colorCorrectionRgba, face
                )

                // 2. Next, render the 3D objects attached to the forehead.
                face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_RIGHT)
                    .toMatrix(rightEarMatrix, 0)
                rightEarObject.updateModelMatrix(rightEarMatrix, scaleFactor)
                rightEarObject.draw(
                    viewMatrix,
                    projectionMatrix,
                    colorCorrectionRgba,
                    DEFAULT_COLOR
                )
                face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_LEFT)
                    .toMatrix(leftEarMatrix, 0)
                leftEarObject.updateModelMatrix(leftEarMatrix, scaleFactor)
                leftEarObject.draw(
                    viewMatrix,
                    projectionMatrix,
                    colorCorrectionRgba,
                    DEFAULT_COLOR
                )

                face.getRegionPose(AugmentedFace.RegionType.NOSE_TIP).toMatrix(noseMatrix, 0)
                noseObject.updateModelMatrix(noseMatrix, scaleFactor)
                noseObject.draw(
                    viewMatrix,
                    projectionMatrix,
                    colorCorrectionRgba,
                    DEFAULT_COLOR
                )
            }

            val bitmap = getBitmapFromSurface(width, height)
            sendARFrame(bitmap, width, height)

        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        } finally {
            GLES20.glDepthMask(true)
        }
    }

    private fun initARSession(context: Context, activity: Activity) {
        try {
            if (session == null) {
                when (ArCoreApk.getInstance().requestInstall(activity, installRequested)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        session = Session(context, EnumSet.noneOf(Session.Feature::class.java))
                        Log.i(TAG, "AR session created")
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = false
                        return
                    }
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Log.e(TAG, "Exception occured: ${e.message}")
            return
        }
    }

    private fun sendARFrame(bmp: Bitmap, width: Int, height: Int) {
        val byteBuffer = ByteBuffer.allocate(bmp.byteCount)
        bmp.copyPixelsToBuffer(byteBuffer)

        val frameData = CustomFrameData(
            byteBuffer, width = width, height = height
        )
        val frame = BJNVideoFrame(
            frameData, FrameOrientation.ORIENTATION_0, FrameFormat.ARGB
        )
        customVideoService.pushCustomVideoFrame(frame)
    }

    private fun configureSession() {
        val config = Config(session)
        config.augmentedFaceMode = AugmentedFaceMode.MESH3D
        session!!.configure(config)
    }

    private fun showToastMessage(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "AugmentedFacesFragment"

        private const val MODEL_FRECKLES = "models/freckles.png"
        private const val MODEL_NOSE = "models/nose.obj"
        private const val MODEL_NOSE_FUR = "models/nose_fur.png"
        private const val MODEL_FOREHEAD_RIGHT = "models/forehead_right.obj"
        private const val MODEL_FOREHEAD_LEFT = "models/forehead_left.obj"
        private const val MODEL_EAR_FUR = "models/ear_fur.png"

        @JvmStatic
        fun newInstance(activity: Activity) =
            AugmentedFacesFragment(activity)
    }
}