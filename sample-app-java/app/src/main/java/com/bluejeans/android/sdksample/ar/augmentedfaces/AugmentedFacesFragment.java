package com.bluejeans.android.sdksample.ar.augmentedfaces;

import static com.bluejeans.android.sdksample.ar.common.helpers.ARUtils.getBitmapFromSurface;
import static com.bluejeans.android.sdksample.ar.common.helpers.ARUtils.isARCoreSupportedAndUpToDate;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bjnclientcore.media.BJNVideoFrame;
import com.bjnclientcore.media.CustomFrameData;
import com.bjnclientcore.media.FrameFormat;
import com.bjnclientcore.media.FrameOrientation;
import com.bjnclientcore.media.VideoSource;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.android.sdksample.ar.common.helpers.DisplayRotationHelper;
import com.bluejeans.android.sdksample.ar.common.rendering.BackgroundRenderer;
import com.bluejeans.android.sdksample.ar.common.rendering.ObjectRenderer;
import com.bluejeans.android.sdksample.databinding.FragmentAugmentedFacesBinding;
import com.bluejeans.bluejeanssdk.customvideo.CustomVideoSourceService;
import com.bluejeans.bluejeanssdk.permission.PermissionService;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AugmentedFacesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AugmentedFacesFragment extends Fragment implements GLSurfaceView.Renderer {

    private static final String TAG = "AugmentedFacesFragment";

    private static final String MODEL_FRECKLES = "models/freckles.png";
    private static final String MODEL_NOSE = "models/nose.obj";
    private static final String MODEL_NOSE_FUR = "models/nose_fur.png";
    private static final String MODEL_FOREHEAD_RIGHT = "models/forehead_right.obj";
    private static final String MODEL_FOREHEAD_LEFT = "models/forehead_left.obj";
    private static final String MODEL_EAR_FUR = "models/ear_fur.png";

    private Session session = null;
    private DisplayRotationHelper displayRotationHelper = null;
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final AugmentedFaceRenderer augmentedFaceRenderer = new AugmentedFaceRenderer();
    private final ObjectRenderer noseObject = new ObjectRenderer();
    private final ObjectRenderer rightEarObject = new ObjectRenderer();
    private final ObjectRenderer leftEarObject = new ObjectRenderer();
    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final Handler handler = new Handler();
    private final float[] noseMatrix = new float[16];
    private final float[] rightEarMatrix = new float[16];
    private final float[] leftEarMatrix = new float[16];
    private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};

    private final PermissionService mPermissionService = SampleApplication.getBlueJeansSDK().getPermissionService();
    private final CustomVideoSourceService mCustomVideoSourceService = SampleApplication.getBlueJeansSDK().getCustomVideoSourceService();

    private int width, height;

    private FragmentAugmentedFacesBinding binding = null;
    public static AugmentedFacesFragment newInstance() {
        AugmentedFacesFragment fragment = new AugmentedFacesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCustomVideoSourceService.setVideoSource(VideoSource.Custom.INSTANCE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAugmentedFacesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        displayRotationHelper = new DisplayRotationHelper(this.getContext());

        // Set up renderer.
        binding.surfaceView.setPreserveEGLContextOnPause(true);
        binding.surfaceView.setEGLContextClientVersion(2);
        binding.surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        binding.surfaceView.setRenderer(this);
        binding.surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        binding.surfaceView.setWillNotDraw(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (session != null) {
            try {
                Log.i(TAG, "resuming session");
                session.resume();
                binding.surfaceView.onResume();
                displayRotationHelper.onResume();
            } catch (CameraNotAvailableException e) {
                throw new RuntimeException(e);
            }
        } else if (mPermissionService.hasPermission(PermissionService.Permission.Camera.INSTANCE)) {
            initARSession();
            if (session != null) {
                CameraConfigFilter cameraConfigFilter = new CameraConfigFilter(session);
                cameraConfigFilter.setFacingDirection(CameraConfig.FacingDirection.FRONT);
                List<CameraConfig> cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter);

                if (!cameraConfigs.isEmpty()) {
                    height = cameraConfigs.get(0).getImageSize().getHeight();
                    width = cameraConfigs.get(0).getImageSize().getWidth();
                    session.setCameraConfig(cameraConfigs.get(0));
                }

                configureSession();
                try {
                    session.resume();
                } catch (CameraNotAvailableException e) {
                    throw new RuntimeException(e);
                }

                binding.surfaceView.onResume();
                displayRotationHelper.onResume();
            } else {
                Log.e(TAG, "Something went wrong");
                this.onDestroyView();
            }
        } else {
            Log.e(TAG, "Something went wrong");
            this.onDestroyView();
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        if (session != null) {
            Log.i(TAG, "pausing session");
            displayRotationHelper.onPause();
            binding.surfaceView.onPause();
            session.pause();
            Log.i(TAG, "session paused successfully");
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroyView");
        if (session != null) {
            handler.post(() -> {
                Log.i(TAG, "closing session");
                session.close();
                session = null;
                mCustomVideoSourceService.setVideoSource(VideoSource.Camera.INSTANCE);
            });
        }
        super.onDestroy();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        try {
            Context context = this.getContext();
            backgroundRenderer.createOnGlThread(context);
            augmentedFaceRenderer.createOnGlThread(context, MODEL_FRECKLES);
            augmentedFaceRenderer.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            noseObject.createOnGlThread(context, MODEL_NOSE, MODEL_NOSE_FUR);
            noseObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            noseObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
            rightEarObject.createOnGlThread(context, MODEL_FOREHEAD_RIGHT, MODEL_EAR_FUR);
            rightEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            rightEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
            leftEarObject.createOnGlThread(context, MODEL_FOREHEAD_LEFT, MODEL_EAR_FUR);
            leftEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
            leftEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            Frame frame = session.update();
            Camera camera = frame.getCamera();

            float[] projectionMatrix = new float[16];
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

            float[] viewMatrix = new float[16];
            camera.getViewMatrix(viewMatrix, 0);

            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            backgroundRenderer.draw(frame);

            Collection<AugmentedFace> faces = session.getAllTrackables(AugmentedFace.class);
            for (AugmentedFace face : faces) {
                if (face.getTrackingState() != TrackingState.TRACKING) {
                    break;
                }

                float scaleFactor = 1.0f;
                GLES20.glDepthMask(false);

                float[] modelMatrix = new float[16];
                face.getCenterPose().toMatrix(modelMatrix, 0);
                augmentedFaceRenderer.draw(
                        projectionMatrix, viewMatrix, modelMatrix, colorCorrectionRgba, face);

                face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_RIGHT).toMatrix(rightEarMatrix, 0);
                rightEarObject.updateModelMatrix(rightEarMatrix, scaleFactor);
                rightEarObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);

                face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_LEFT).toMatrix(leftEarMatrix, 0);
                leftEarObject.updateModelMatrix(leftEarMatrix, scaleFactor);
                leftEarObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);

                face.getRegionPose(AugmentedFace.RegionType.NOSE_TIP).toMatrix(noseMatrix, 0);
                noseObject.updateModelMatrix(noseMatrix, scaleFactor);
                noseObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR);
            }

            Bitmap bitmap = getBitmapFromSurface(width, height);
            if (bitmap != null) {
                sendARFrame(bitmap);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Exception on the OpenGL thread", t);
        } finally {
            GLES20.glDepthMask(true);
        }
    }

    private void initARSession() {
        try {
            if (session == null && isARCoreSupportedAndUpToDate(this.getContext(), this.getActivity())) {
                session = new Session(this.getContext(), EnumSet.noneOf(Session.Feature.class));
                Log.i(TAG, "AR session created");
            } else {
                Log.e(TAG, "Either session is not null or AR is not supported");
            }
        } catch (UnavailableDeviceNotCompatibleException e) {
            Log.e(TAG, "Exception occured: " + e.getMessage());
        } catch (UnavailableSdkTooOldException e) {
            Log.e(TAG, "Exception occured: " + e.getMessage());
        } catch (UnavailableArcoreNotInstalledException e) {
            Log.e(TAG, "Exception occured: " + e.getMessage());
        } catch (UnavailableApkTooOldException e) {
            Log.e(TAG, "Exception occured: " + e.getMessage());
        }
    }

    private void sendARFrame(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(byteBuffer);

        CustomFrameData frameData = new CustomFrameData(byteBuffer, 0, null, null, 0, 0, width, height);
        BJNVideoFrame frame = new BJNVideoFrame(
                frameData, FrameOrientation.ORIENTATION_0, FrameFormat.ARGB, false
        );
        mCustomVideoSourceService.pushCustomVideoFrame(frame);
    }

    private void configureSession() {
        Config config = new Config(session);
        config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
        session.configure(config);
    }
}