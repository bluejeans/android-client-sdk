/*
 * Copyright (c) 2020 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import com.bjnclientcore.inmeeting.contentshare.ContentShareType;
import com.bjnclientcore.media.individualstream.StreamPriority;
import com.bjnclientcore.media.individualstream.StreamQuality;
import com.bjnclientcore.media.individualstream.VideoStreamStyle;
import com.bluejeans.android.sdksample.ar.augmentedfaces.AugmentedFacesFragment;
import com.bluejeans.android.sdksample.dialog.WaitingRoomDialog;
import com.bluejeans.android.sdksample.isc.IStreamConfigUpdatedCallback;
import com.bluejeans.android.sdksample.isc.IscGalleryFragment;
import com.bluejeans.android.sdksample.isc.usecases.RemoteAssistFragment;
import com.bluejeans.android.sdksample.isc.usecases.RemoteLearningFragment;
import com.bluejeans.android.sdksample.menu.MenuFragment;
import com.bluejeans.android.sdksample.menu.MenuFragment.IMenuCallback;
import com.bluejeans.android.sdksample.menu.adapters.AudioDeviceAdapter;
import com.bluejeans.android.sdksample.menu.adapters.VideoDeviceAdapter;
import com.bluejeans.android.sdksample.menu.adapters.VideoLayoutAdapter;
import com.bluejeans.android.sdksample.menu.adapters.VideoStreamStyleAdapter;
import com.bluejeans.android.sdksample.menu.adapters.VideoStreamUseCasesAdapter;
import com.bluejeans.android.sdksample.participantlist.IscParticipantListFragment;
import com.bluejeans.android.sdksample.participantlist.ParticipantListFragment;
import com.bluejeans.bluejeanssdk.devices.AudioDevice;
import com.bluejeans.bluejeanssdk.devices.VideoDevice;
import com.bluejeans.bluejeanssdk.devices.VideoDeviceService;
import com.bluejeans.bluejeanssdk.logging.LoggingService;
import com.bluejeans.bluejeanssdk.meeting.ClosedCaptioningService;
import com.bluejeans.bluejeanssdk.meeting.ContentShareAvailability;
import com.bluejeans.bluejeanssdk.meeting.ContentShareState;
import com.bluejeans.bluejeanssdk.meeting.MeetingService;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.bluejeanssdk.meeting.WaitingRoomParticipantEvent;
import com.bluejeans.bluejeanssdk.permission.PermissionService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import static com.bluejeans.android.sdksample.utils.AudioDeviceHelper.getAudioDeviceName;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MainActivity";

    private final String appVersionString = "v" + SampleApplication.getBlueJeansSDK().getVersion();
    private final PermissionService mPermissionService = SampleApplication.getBlueJeansSDK().getPermissionService();
    private final LoggingService mLoggingService = SampleApplication.getBlueJeansSDK().getLoggingService();
    private final MeetingService mMeetingService = SampleApplication.getBlueJeansSDK().getMeetingService();
    private final VideoDeviceService mVideoDeviceService = SampleApplication.getBlueJeansSDK().getVideoDeviceService();

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final CompositeDisposable mInMeetingDisposable = new CompositeDisposable();
    private boolean mIsAudioMuted = false, mIsVideoMuted = false, mIsVideoMutedBeforeBackgrounding = false, mIsVideoSourceCustom = false;
    private boolean mIsRemoteContentAvailable;

    //View IDs
    private ConstraintLayout mSelfView, mJoinLayout, mWaitingRoomLayout;
    private Button btnJoin, btnExitWaitingRoom;
    private CheckBox mCbShowIsc;
    private ImageView mIvMic, mIvClose, mIvMenuOption, mIvLogUploadButton;
    private ImageView mIvVideo;
    private EditText mEtEventId, mEtPassCode, mEtName;
    private TextView mTvProgressMsg, mAppVersion, mTvClosedCaption, mTvWaitingRoom;
    private ConstraintLayout mControlPanelContainer;
    private ImageView mIvParticipant;
    private ImageView mIvScreenShare;
    private ImageView mCameraSettings;
    private MenuFragment mBottomSheetFragment;
    private IscGalleryFragment iscGalleryFragment;
    private InMeetingFragment inMeetingFragment;
    private RemoteLearningFragment remoteLearningFragment;
    private RemoteAssistFragment remoteAssistFragment;
    private AugmentedFacesFragment mAugmentedFacesFragment;
    private ParticipantListFragment mParticipantListFragment = null;
    private IscParticipantListFragment mIscParticipantListFragment = null;
    private FragmentContainerView mCustomVideoFragmentContainer = null;

    //For alter dialog
    private VideoDeviceAdapter mVideoDeviceAdapter = null;
    private AudioDeviceAdapter mAudioDeviceAdapter = null;
    private VideoLayoutAdapter mVideoLayoutAdapter = null;
    private VideoStreamStyleAdapter mStreamStyleAdapter = null;
    private VideoStreamUseCasesAdapter mUseCasesAdapter = null;
    private AlertDialog mAudioDialog = null;
    private AlertDialog mVideoLayoutDialog = null;
    private AlertDialog mVideoDeviceDialog = null;
    private AlertDialog mStreamStyleDialog = null;
    private AlertDialog mStreamUseCasesDialog = null;
    private AlertDialog mUploadLogsDialog = null;
    private AlertDialog mCameraSettingsDialog = null;
    private ProgressBar mProgressBar = null;
    private SeekBar zoomSeekBar = null;
    private int mZoomScaleFactor = 1; // default value of 1, no zoom to start with
    private boolean mIsInWaitingRoom = false;
    private boolean mIsWaitingRoomEnabled = false;
    private boolean mIsScreenShareInProgress = false;
    private boolean mIsReconnecting = false;
    private boolean mIsCallInProgress = false;
    private boolean mIsIscEnabled = false;

    private String mCurrentPinnedParticipant = null;

    private IStreamConfigUpdatedCallback streamConfigUpdatedCallback = new IStreamConfigUpdatedCallback() {

        @Override
        public void onStreamConfigurationUpdated(String seamGuid, StreamQuality streamQuality, StreamPriority streamPriority) {
            if (getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof IscGalleryFragment && iscGalleryFragment != null) {
                iscGalleryFragment.updateStreamConfigurations(seamGuid, streamQuality, streamPriority);
            }
        }

        @Override
        public void pinParticipant(String participantId, Boolean isPinned) {
            if (getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof IscGalleryFragment && iscGalleryFragment != null) {
                iscGalleryFragment.pinParticipant(participantId, isPinned);
                if (mIscParticipantListFragment != null) {
                    if (isPinned) {
                        mCurrentPinnedParticipant = participantId;
                        mIscParticipantListFragment.setPinnedParticipant(participantId);
                    } else {
                        mCurrentPinnedParticipant = null;
                        mIscParticipantListFragment.setPinnedParticipant(null);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // permission service needs activity to be registered before calling request for permissions
        mPermissionService.register(this);
        initViews();
        checkCameraPermissionAndStartSelfVideo();
        activateSDKSubscriptions();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            configurePortraitView();
        } else {
            configureLandscapeView();
        }
        Log.d(TAG, "onConfigurationChanged");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsVideoMutedBeforeBackgrounding = mIsVideoMuted;
        mMeetingService.setVideoMuted(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMeetingService.setVideoMuted(mIsVideoMutedBeforeBackgrounding);
        if (!mIsScreenShareInProgress) {
            MeetingService.MeetingState meetingState = mMeetingService.getMeetingState().getValue();
            handleMeetingState(meetingState);
        }
        mMeetingService.requestAudioFocus();
    }

    @Override
    protected void onDestroy() {
        if (!(mMeetingService.getMeetingState().getValue() instanceof MeetingService.MeetingState.Idle)) {
            mMeetingService.endMeeting();
            endMeeting();
        }
        mDisposable.dispose();
        mInMeetingDisposable.dispose();
        mBottomSheetFragment = null;
        mIsCallInProgress = false;
        super.onDestroy();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnJoin:
                if (mMeetingService.getMeetingState().getValue() instanceof MeetingService.MeetingState.Idle) {
                    checkAllPermissionsAndJoin();
                } else {
                    showToastMessage(getString(R.string.meeting_in_progress));
                }
                break;
            case R.id.imgClose:
                mMeetingService.endMeeting();
                endMeeting();
                break;
            case R.id.imgMenuOption:
                if (mBottomSheetFragment != null) {
                    mBottomSheetFragment.show(getSupportFragmentManager(), mBottomSheetFragment.getTag());
                }
                break;
            case R.id.ivMic:
                mIsAudioMuted = !mIsAudioMuted;
                mMeetingService.setAudioMuted(mIsAudioMuted);
                toggleAudioMuteUnMuteView(mIsAudioMuted);
                break;
            case R.id.ivVideo:
                mIsVideoMuted = !mIsVideoMuted;
                mMeetingService.setVideoMuted(mIsVideoMuted);
                toggleVideoMuteUnMuteView(mIsVideoMuted);
                break;
            case R.id.imgRoster:
                if (isVideoStreamRoster()) {
                    if (mIscParticipantListFragment != null) {
                        mIscParticipantListFragment.setPinnedParticipant(mCurrentPinnedParticipant);
                    }
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.rosterContainer, mIscParticipantListFragment)
                            .addToBackStack("IscParticipantListFragment")
                            .commit();
                } else {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.rosterContainer, mParticipantListFragment)
                            .addToBackStack("ParticipantListFragment")
                            .commit();
                }
                break;
            case R.id.imgScreenShare:
                if (mMeetingService.getContentShareService().getContentShareState().getValue() instanceof ContentShareState.Stopped) {
                    mIsScreenShareInProgress = true;
                    MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    activityResultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
                } else {
                    mIsScreenShareInProgress = false;
                    mMeetingService.getContentShareService().stopContentShare();
                }
                break;
            case R.id.ivCameraSettings:
                showCameraSettingsDialog();
                break;

            case R.id.imgUploadLogs:
                showUploadLogsDialog();
                break;

            case R.id.btn_exit_waiting_room:
                mMeetingService.endMeeting();
                endMeeting();
                break;
            default:
        }
    }

    private void showUploadLogsDialog() {
        mUploadLogsDialog = new AlertDialog.Builder(this)
                .setView(R.layout.submit_log_dialog).create();
        mUploadLogsDialog.setCanceledOnTouchOutside(true);
        mUploadLogsDialog.show();
        EditText editText = mUploadLogsDialog.findViewById(R.id.description);
        Button submitButton = mUploadLogsDialog.findViewById(R.id.btn_submit);
        mProgressBar = mUploadLogsDialog.findViewById(R.id.progressBar);
        if (editText != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (submitButton != null) {
                        submitButton.setEnabled(count != 0);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
        if (submitButton != null) {
            submitButton.setOnClickListener(v -> {
                String description = Objects.requireNonNull(editText).getText().toString();
                submitButton.setEnabled(false);
                mProgressBar.setVisibility(View.VISIBLE);
                uploadLogs(description);
            });
        }
    }

    private void uploadLogs(String comments) {
        if (!TextUtils.isEmpty(comments)) {
            String userName = (TextUtils.isEmpty(mEtName.getText().toString()) ? "Guest"
                    : mEtName.getText().toString());
            mDisposable.add(
                    mLoggingService.uploadLog(comments, userName)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(result ->
                                    {
                                        Log.i(TAG, "Log uploaded successfully " + result);
                                        if (result != LoggingService.LogUploadResult.Success.INSTANCE) {
                                            showToastMessage(getString(R.string.upload_logs_failure));
                                        } else {
                                            showToastMessage(getString(R.string.upload_logs_success));
                                        }
                                        mUploadLogsDialog.dismiss();
                                    },
                                    error -> {
                                        Log.e(TAG, "Error while uploading logs");
                                        mProgressBar.setVisibility(View.GONE);
                                        showToastMessage(getString(R.string.upload_logs_failure));
                                    }));
        } else {
            showToastMessage("Please enter your comments.");
        }
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        mMeetingService.getContentShareService()
                                .startContentShare(new ContentShareType.Screen(data));
                    }
                }
            });

    private void activateSDKSubscriptions() {
        subscribeForMeetingStatus();
        subscribeToWaitingRoomEvents();
        subscribeForAudioDevices();
        subscribeForCurrentAudioDevice();
        subscribeForVideoDevices();
        subscribeForCurrentVideoDevice();
    }

    private void activateInMeetingSubscriptions() {
        subscribeForVideoMuteStatus();
        subscribeForAudioMuteStatus();
        subscribeForVideoLayout();
        subscribeForParticipants();
        subscribeForContentShareState();
        subscribeForContentShareAvailability();
        subscribeForRemoteContentState();
        subscribeForContentShareEvents();
        subscribeForClosedCaptionText();
        subscribeForClosedCaptionState();
        subscribeForHDCaptureState();
        subscribeToActiveSpeaker();
        subscribeForModeratorWaitingRoomEvents();

        mIsCallInProgress = true;
    }

    private void checkCameraPermissionAndStartSelfVideo() {
        if (mPermissionService.hasPermission(PermissionService.Permission.Camera.INSTANCE)) {
            startSelfVideo();
        } else {
            PermissionService.Permission[] notificationPermission = {PermissionService.Permission.Notifications.INSTANCE};
            PermissionService.Permission[] arr = {PermissionService.Permission.Camera.INSTANCE};
            mDisposable.add(
                    mPermissionService.requestPermissions(notificationPermission).flatMap((Function<PermissionService.RequestStatus, SingleSource<?>>) requestStatus -> {
                                        Log.i(TAG, "Notification permission state: " + requestStatus);
                                        return mPermissionService.requestPermissions(arr);
                                    }
                            )
                            .subscribe(
                                    grantedStatus -> {
                                        if (grantedStatus == PermissionService.RequestStatus.Granted.INSTANCE) {
                                            startSelfVideo();
                                        } else {
                                            Log.d(TAG, "Camera or notification permission denied");
                                        }
                                    },
                                    err -> Log.e(TAG, "Error in requesting permission subscription")));
        }
    }

    private void startSelfVideo() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.selfViewFrame, mVideoDeviceService.getSelfVideoFragment())
                .commit();
        mMeetingService.setVideoMuted(false);
    }

    private void checkAllPermissionsAndJoin() {
        if (mPermissionService.hasAllPermissions()) {
            hideKeyboard();
            joinMeeting();
        } else {
            requestAllPermissionsAndJoin();
        }
    }

    private void requestAllPermissionsAndJoin() {
        mDisposable.add(mPermissionService.requestAllPermissions().subscribe(
                areAllPermissionsGranted -> {
                    if (areAllPermissionsGranted == PermissionService.RequestStatus.Granted.INSTANCE) {
                        joinMeeting();
                    } else {
                        Log.i(TAG, "Not enough permissions to join a meeting");
                    }
                },
                err -> Log.e(TAG, "Error in requesting permissions subscription " + err.getMessage())));
    }

    private void joinMeeting() {
        String meetingId = mEtEventId.getText().toString();
        String passcode = mEtPassCode.getText().toString();
        String name = (TextUtils.isEmpty(mEtName.getText().toString()) ? "AndroidSDK"
                : mEtName.getText().toString());
        showJoiningInProgressView();
        mInMeetingDisposable.add(mMeetingService.joinMeeting(
                        new MeetingService.JoinParams
                                (meetingId, passcode, name))
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Log.i(TAG, "Join state is " + result);
                            if (result != MeetingService.JoinResult.Success.INSTANCE) {
                                showOutOfMeetingView();
                            }
                        },
                        error -> showOutOfMeetingView()));

    }

    private void endMeeting() {
        mIsReconnecting = false;
        if (mSelfView.getVisibility() == View.GONE) {
            if (mAugmentedFacesFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(mAugmentedFacesFragment)
                        .commit();
                mAugmentedFacesFragment = null;
                mCustomVideoFragmentContainer.setVisibility(View.VISIBLE);
            }
            mSelfView.setVisibility(View.VISIBLE);
        }
        if (mIsInWaitingRoom) {
            mIsInWaitingRoom = false;
            showWaitingRoomUI();
        } else {
            showOutOfMeetingView();
        }

        if (mCameraSettingsDialog != null)
            mCameraSettingsDialog.dismiss();
        mIsRemoteContentAvailable = false;
        mIsCallInProgress = false;
        OnGoingMeetingService.stopService(this);
        mInMeetingDisposable.clear();
    }

    // Return Unit.INSTANCE; is needed for a kotlin java interop
    // Refer https://developer.android.com/kotlin/interop#lambda_arguments for more details
    private void subscribeForMeetingStatus() {
        mDisposable.add(
                mMeetingService.getMeetingState().getRxObservable().observeOn(AndroidSchedulers.mainThread())
                        .subscribe(state -> {
                            Log.i(TAG, "State: " + state);
                            handleMeetingState(state);
                        }, err -> {
                            Log.e(TAG, err.getMessage());
                        })
        );
    }

    private void subscribeToWaitingRoomEvents() {
        mDisposable.add(mMeetingService.getWaitingRoomEvent().observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        event -> {
                            if (event instanceof MeetingService.WaitingRoomEvent.Denied) {
                                showToastMessage("Denied by moderator");
                            } else if (event instanceof MeetingService.WaitingRoomEvent.Demoted) {
                                showToastMessage("Demoted by moderator");
                            } else if (event instanceof MeetingService.WaitingRoomEvent.Admitted) {
                                showToastMessage("Moderator has approved");
                            } else {
                                Log.w(TAG, "Unrecognized event: " + event);
                            }
                        },
                        err -> {
                            Log.e(TAG, err.getMessage());
                        })
        );
    }

    private void subscribeForRemoteContentState() {
        mInMeetingDisposable.add(mMeetingService.getContentShareService().getReceivingRemoteContent().subscribeOnUI(isReceivingRemoteContent -> {
            if (isReceivingRemoteContent != null) {
                mIsRemoteContentAvailable = isReceivingRemoteContent;
                if (isReceivingRemoteContent) {
                    showInMeetingView();
                } else {
                    handleRemoteContentState();
                }
            }
            return Unit.INSTANCE;
        }, err -> {
            Log.e(TAG, "Error in remote content subscription " + err.getMessage());
            return Unit.INSTANCE;
        }));
    }

    private void subscribeForAudioMuteStatus() {
        mInMeetingDisposable.add(mMeetingService.getAudioMuted().subscribe(
                isMuted -> {
                    if (isMuted != null) {
                        // This could be due to local mute or remote mute
                        mIsAudioMuted = isMuted;
                        toggleAudioMuteUnMuteView(isMuted);
                    }
                    Log.i(TAG, " Audio Mute state " + isMuted);
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in audio mute status subscription");
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForVideoMuteStatus() {
        mInMeetingDisposable.add(mMeetingService.getVideoMuted().subscribeOnUI(
                isMuted -> {
                    if (isMuted != null) {
                        // This could be due to local mute or remote mute
                        mIsVideoMuted = isMuted;
                        toggleVideoMuteUnMuteView(isMuted);
                    }

                    Log.i(TAG, " Video Mute state " + isMuted);
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in video mute status subscription " + err.getMessage());
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForVideoLayout() {
        mInMeetingDisposable.add(mMeetingService.getVideoLayout().subscribeOnUI(
                videoLayout -> {
                    if (videoLayout != null) {
                        String videoLayoutName = null;
                        if (videoLayout.equals(MeetingService.VideoLayout.Speaker.INSTANCE)) {
                            videoLayoutName = getString(R.string.speaker_view);
                        } else if (videoLayout.equals(MeetingService.VideoLayout.Gallery.INSTANCE)) {
                            videoLayoutName = getString(R.string.gallery_view);
                        } else if (videoLayout.equals(MeetingService.VideoLayout.People.INSTANCE)) {
                            videoLayoutName = getString(R.string.people_view);
                        } else if (videoLayout.equals(MeetingService.VideoLayout.Custom.INSTANCE)) {
                            videoLayoutName = getString(R.string.custom_view);
                        }

                        if (getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof IscGalleryFragment &&
                            videoLayout != MeetingService.VideoLayout.Custom.INSTANCE) {
                            replaceInMeetingFragment(false);
                        }

                        if (mSelfView.getVisibility() == View.GONE) {
                            mSelfView.setVisibility(View.VISIBLE);
                        }

                        Log.i(TAG, "Received layout from server: " + videoLayoutName);
                        if (mBottomSheetFragment != null) {
                            mBottomSheetFragment.updateVideoLayout(videoLayoutName);
                            updateCurrentVideoLayoutForAlertDialog(videoLayoutName);
                        }
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in video layout subscription " + err.getMessage());
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForCurrentAudioDevice() {
        mDisposable.add(mMeetingService.getAudioDeviceService().getCurrentAudioDevice().subscribeOnUI(
                currentAudioDevice -> {
                    if (currentAudioDevice != null) {
                        mBottomSheetFragment.updateAudioDevice(getAudioDeviceName(currentAudioDevice));
                        updateCurrentAudioDeviceForAlertDialog(currentAudioDevice);
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in current audio device subscription " + err.getMessage());
                    return Unit.INSTANCE;
                }));

    }

    private void subscribeForAudioDevices() {
        mDisposable.add(mMeetingService.getAudioDeviceService().getAudioDevices().subscribeOnUI(audioDevices -> {
                    if (audioDevices != null) {
                        mAudioDeviceAdapter.clear();
                        mAudioDeviceAdapter.addAll(audioDevices);
                        mAudioDeviceAdapter.notifyDataSetChanged();
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in audio devices subscription " + err.getMessage());
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForCurrentVideoDevice() {
        mDisposable.add(mVideoDeviceService.getCurrentVideoDevice().subscribeOnUI(
                currentVideoDevice -> {
                    if (currentVideoDevice != null) {
                        mBottomSheetFragment.updateVideoDevice(currentVideoDevice.getName());
                        updateCurrentVideoDeviceForAlertDialog(currentVideoDevice);
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in current video device subscription " + err.getMessage());
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForParticipants() {
        mInMeetingDisposable.add(mMeetingService.getParticipantsService().getParticipants().subscribeOnUI(
                participantList -> {
                    if (mParticipantListFragment != null && participantList != null) {
                        mParticipantListFragment.updateMeetingList(participantList);
                    }

                    if (mIscParticipantListFragment != null && participantList != null) {
                        mIscParticipantListFragment.updateMeetingList(participantList);
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in Participants subscription " + err.getMessage());
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForContentShareAvailability() {
        mInMeetingDisposable.add(mMeetingService.getContentShareService().getContentShareAvailability().subscribeOnUI(
                contentShareAvailability -> {
                    if (contentShareAvailability instanceof ContentShareAvailability.Available) {
                        mIvScreenShare.setVisibility(View.VISIBLE);
                    } else {
                        mIvScreenShare.setVisibility(View.GONE);
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in content share availability" + err.getMessage());
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForContentShareState() {
        mInMeetingDisposable.add(mMeetingService.getContentShareService().getContentShareState().subscribeOnUI(
                contentShareState -> {
                    if (contentShareState instanceof ContentShareState.Stopped) {
                        mIvScreenShare.setSelected(false);
                        MeetingNotificationUtility.updateNotificationMessage(this, getString(R.string.meeting_notification_message));
                    } else if (contentShareState != null) {
                        mIvScreenShare.setSelected(true);
                        MeetingNotificationUtility.updateNotificationMessage(this, getString(R.string.screen_share_notification_message));
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in content share state subscription" + err.getMessage());
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForContentShareEvents() {
        mInMeetingDisposable.add(mMeetingService.getContentShareService().getContentShareEvent()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        contentShareEvent -> Log.i(TAG, "Content share event is " + contentShareEvent),
                        err -> Log.e(TAG, "Error in content share events subscription" + err.getMessage())));
    }

    private void subscribeForVideoDevices() {
        mDisposable.add(mVideoDeviceService.getVideoDevices().subscribeOnUI(videoDevices -> {
                    if (videoDevices != null) {
                        mVideoDeviceAdapter.clear();
                        mVideoDeviceAdapter.addAll(videoDevices);
                        mVideoDeviceAdapter.notifyDataSetChanged();
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in video devices subscription");
                    return Unit.INSTANCE;
                }));
    }

    private void subscribeForClosedCaptionText() {
        mInMeetingDisposable.add(mMeetingService.getClosedCaptioningService().getClosedCaptionText()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(content -> {
                    mTvClosedCaption.setText(content);
                }, error -> Log.e(TAG, "Error closed caption subscription " + error)));
    }

    private void subscribeForClosedCaptionState() {
        mInMeetingDisposable.add(mMeetingService.getClosedCaptioningService().getClosedCaptioningState().subscribeOnUI(
                closedCaptioningState -> {
                    if (closedCaptioningState != null) {
                        if (closedCaptioningState == ClosedCaptioningService.
                                ClosedCaptioningState.Started.INSTANCE) {
                            mTvClosedCaption.setVisibility(View.VISIBLE);
                            mBottomSheetFragment.updateClosedCaptionSwitchState(true);
                        } else {
                            mBottomSheetFragment.updateClosedCaptionSwitchState(false);
                            mTvClosedCaption.setVisibility(View.GONE);
                        }
                    }
                    return Unit.INSTANCE;
                }, err -> {
                    Log.e(TAG, "Error subscribing closed caption event");
                    return Unit.INSTANCE;
                }
        ));
    }

    private void subscribeForHDCaptureState() {
        mInMeetingDisposable.add(
                mVideoDeviceService.is720pVideoCaptureEnabled()
                        .subscribeOnUI(
                                hdCaptureState -> {
                                    mBottomSheetFragment.updateHDCaptureState(hdCaptureState);
                                    if (zoomSeekBar != null) {
                                        zoomSeekBar.setProgress(1);
                                    }
                                    return Unit.INSTANCE;
                                }, err -> {
                                    Log.e(TAG, "Error subscribing hd capture state");
                                    return Unit.INSTANCE;
                                }
                        ));
    }

    private void subscribeToActiveSpeaker() {
        mInMeetingDisposable.add(mMeetingService.getParticipantsService().getActiveSpeaker().getRxObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe(participant -> {
                    if (participant.getValue() != null) {
                        Log.i(TAG, participant.getValue().getName() + " is the active speaker.");
                    } else {
                        Log.e(TAG, "Participant information is missing");
                    }
                }, err -> {
                    Log.e(TAG, "Exception while subscribing to active speaker: " + err.getMessage());
                })
        );
    }

    private void subscribeForModeratorWaitingRoomEvents() {
        boolean isModerator = SampleApplication.getBlueJeansSDK()
                .getBlueJeansClient().getMeetingSession().isModerator();
        if (isModerator) {
            mInMeetingDisposable.add(mMeetingService.getModeratorWaitingRoomService().isWaitingRoomEnabled()
                    .getRxObservable().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            isEnabled -> {
                                if (isEnabled.getValue() == true) {
                                    mIsWaitingRoomEnabled = true;
                                } else {
                                    mIsWaitingRoomEnabled = false;
                                }
                            }, err -> {
                                Log.e(TAG, "Error subscribing to waiting room enabled");
                            }
                    ));

            mInMeetingDisposable.add(mMeetingService.getModeratorWaitingRoomService().getWaitingRoomParticipantEvents()
                    .getRxObservable().observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            event -> {
                                if (event.getValue() instanceof WaitingRoomParticipantEvent.Added) {
                                    if (((WaitingRoomParticipantEvent.Added) event.getValue()).getParticipants().size() == 1) {
                                        Log.i(TAG,
                                                "WAITING_ROOM: " + ((WaitingRoomParticipantEvent.Added) event.getValue()).getParticipants().get(0).getName() + " has arrived in the waiting room");
                                        showToastMessage(((WaitingRoomParticipantEvent.Added) event.getValue()).getParticipants().get(0).getName() + " has arrived in the waiting room");
                                    } else if (((WaitingRoomParticipantEvent.Added) event.getValue()).getParticipants().size() > 1) {
                                        Log.i(TAG, "WAITING_ROOM: " + getString(R.string.multiple_participants_arrived_wr));
                                        showToastMessage(getString(R.string.multiple_participants_arrived_wr));
                                    }
                                } else if (event.getValue() instanceof WaitingRoomParticipantEvent.Removed) {
                                    if (((WaitingRoomParticipantEvent.Removed) event.getValue()).getParticipants().size() == 1) {
                                        Log.i(TAG,
                                                "WAITING_ROOM: " + ((WaitingRoomParticipantEvent.Removed) event.getValue()).getParticipants().get(0).getName() + " has left the waiting room");
                                        showToastMessage(((WaitingRoomParticipantEvent.Removed) event.getValue()).getParticipants().get(0).getName() + " has left the waiting room");
                                    } else if (((WaitingRoomParticipantEvent.Removed) event.getValue()).getParticipants().size() > 1) {
                                        Log.i(TAG, "WAITING_ROOM: " + getString(R.string.multiple_participants_left_wr));
                                        showToastMessage(getString(R.string.multiple_participants_left_wr));
                                    }
                                }
                            }, err -> {
                                Log.e(TAG, "Unknown waiting room participant event: " + err.getMessage());
                            }
                    )
            );
        }
    }

    private boolean areInMeetingServicesAvailable() {
        return mMeetingService.getMeetingState().getValue() instanceof MeetingService.MeetingState.Connecting
                || mMeetingService.getMeetingState().getValue() instanceof MeetingService.MeetingState.Connected
                || mMeetingService.getMeetingState().getValue() instanceof MeetingService.MeetingState.Reconnecting;
    }

    private void initViews() {
        // we are caching the fragment as when user change layout
        // if not cached, fragment dimensions are returned null resulting in no layout being displayed
        // user can also use detach and attach fragments on page listener inorder to relayout.

        mIvClose = findViewById(R.id.imgClose);
        mIvMenuOption = findViewById(R.id.imgMenuOption);
        mIvParticipant = findViewById(R.id.imgRoster);
        mIvScreenShare = findViewById(R.id.imgScreenShare);
        mCameraSettings = findViewById(R.id.ivCameraSettings);
        mControlPanelContainer = findViewById(R.id.control_panel_container);
        //Self View
        mSelfView = findViewById(R.id.selfView);
        mIvMic = findViewById(R.id.ivMic);
        mIvVideo = findViewById(R.id.ivVideo);
        //Join Layout
        mJoinLayout = findViewById(R.id.joinInfo);
        mEtEventId = findViewById(R.id.etEventId);
        mEtPassCode = findViewById(R.id.etPasscode);
        mEtName = findViewById(R.id.etName);
        mCbShowIsc = findViewById(R.id.cbShowIsc);
        // Waiting Room Layout
        mWaitingRoomLayout = findViewById(R.id.waiting_room_layout);
        //Progress View
        mTvProgressMsg = findViewById(R.id.tvProgressMsg);
        mAppVersion = findViewById(R.id.tvAppVersion);
        mTvClosedCaption = findViewById(R.id.tvClosedCaption);
        mTvWaitingRoom = findViewById(R.id.tv_waiting_room);
        mCustomVideoFragmentContainer = findViewById(R.id.custom_video_fragment_container);
        btnJoin = findViewById(R.id.btnJoin);
        btnJoin.setOnClickListener(this);
        btnExitWaitingRoom = findViewById(R.id.btn_exit_waiting_room);
        btnExitWaitingRoom.setOnClickListener(this);
        mIvLogUploadButton = findViewById(R.id.imgUploadLogs);
        mIvLogUploadButton.setOnClickListener(this);
        mIvClose.setOnClickListener(this);
        mIvMenuOption.setOnClickListener(this);
        mIvParticipant.setOnClickListener(this);
        mIvScreenShare.setOnClickListener(this);
        mIvMic.setOnClickListener(this);
        mIvVideo.setOnClickListener(this);
        mCameraSettings.setOnClickListener(this);
        mBottomSheetFragment = new MenuFragment(mIOptionMenuCallback, mIsWaitingRoomEnabled, mCbShowIsc.isChecked());
        mBottomSheetFragment.updateVideoStreamStyle(getResources().getStringArray(R.array.stream_styles)[0]);
        mParticipantListFragment = new ParticipantListFragment();
        mIscParticipantListFragment = new IscParticipantListFragment(streamConfigUpdatedCallback, getApplicationContext());
        mVideoLayoutAdapter = getVideoLayoutAdapter();
        mVideoDeviceAdapter = getVideoDeviceAdapter(new ArrayList<>());
        mAudioDeviceAdapter = getAudioDeviceAdapter(new ArrayList<>());
        mStreamStyleAdapter = getStreamStyleAdapter(Arrays.asList(getResources().getStringArray(R.array.stream_styles)));
        mUseCasesAdapter = getVideoStreamUseCasesAdapter(Arrays.asList(getResources().getStringArray(R.array.isc_use_cases)));
        mAppVersion.setText(appVersionString);
    }

    private void showInMeetingFragment(Boolean showIscFragment) {
        if (getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) == null) {
            if (showIscFragment) {
                iscGalleryFragment = new IscGalleryFragment();
                getSupportFragmentManager().beginTransaction().replace(
                        R.id.inMeetingFragmentContainer,
                        iscGalleryFragment
                ).commit();
            } else {
                inMeetingFragment = new InMeetingFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(
                                R.id.inMeetingFragmentContainer,
                                inMeetingFragment
                        ).commit();
            }
        }
    }

    private void removeInMeetingFragment() {
        Fragment inMeetingFragment = getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer);
        if (inMeetingFragment != null)
            getSupportFragmentManager().beginTransaction()
                    .remove(inMeetingFragment)
                    .commit();
    }

    private void replaceInMeetingFragment(Boolean showIscFragment) {
        if (iscGalleryFragment != null) iscGalleryFragment.onDestroy();
        if (inMeetingFragment != null) inMeetingFragment.onDestroy();
        if (remoteLearningFragment != null) remoteLearningFragment.onDestroy();
        if (remoteAssistFragment != null) remoteAssistFragment.onDestroy();
        if (mAugmentedFacesFragment != null) mAugmentedFacesFragment.onDestroy();
        if (showIscFragment) {
            iscGalleryFragment = new IscGalleryFragment();
            getSupportFragmentManager().beginTransaction().replace(
                    R.id.inMeetingFragmentContainer,
                    iscGalleryFragment
            ).commit();
        } else {
            inMeetingFragment = new InMeetingFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(
                            R.id.inMeetingFragmentContainer,
                            inMeetingFragment
                    ).commit();
        }
    }

    private void showJoiningInProgressView() {
        mJoinLayout.setVisibility(View.GONE);
        mTvProgressMsg.setVisibility(View.VISIBLE);
        mTvProgressMsg.setText(getString(R.string.connectingState));
        mAppVersion.setVisibility(View.GONE);
        mIvClose.setVisibility(View.VISIBLE);
        mIvMenuOption.setVisibility(View.VISIBLE);
        mIvLogUploadButton.setVisibility(View.GONE);
        mControlPanelContainer.setBackgroundResource(R.drawable.meeting_controls_panel_bg);
    }

    private void showInMeetingView() {
        mAppVersion.setVisibility(View.GONE);
        mTvProgressMsg.setVisibility(View.GONE);
        mJoinLayout.setVisibility(View.GONE);
        mIvClose.setVisibility(View.VISIBLE);
        mIvMenuOption.setVisibility(View.VISIBLE);
        mIvParticipant.setVisibility(View.VISIBLE);
        mIvLogUploadButton.setVisibility(View.GONE);
        mTvClosedCaption.setVisibility(View.VISIBLE);
        mControlPanelContainer.setBackgroundResource(R.drawable.meeting_controls_panel_bg);
    }

    private void showOutOfMeetingView() {
        mTvProgressMsg.setVisibility(View.GONE);
        mIvClose.setVisibility(View.GONE);
        mIvMenuOption.setVisibility(View.GONE);
        mJoinLayout.setVisibility(View.VISIBLE);
        mIvParticipant.setVisibility(View.GONE);
        mIvScreenShare.setVisibility(View.GONE);
        mAppVersion.setVisibility(View.VISIBLE);
        mIvLogUploadButton.setVisibility(View.VISIBLE);
        mControlPanelContainer.setBackgroundResource(0);
        mTvClosedCaption.setVisibility(View.GONE);
        mTvClosedCaption.setText(null);
        if (mBottomSheetFragment != null && mBottomSheetFragment.isAdded()) {
            mBottomSheetFragment.dismiss();
        }
        if (mParticipantListFragment != null && mParticipantListFragment.isAdded() && !mCbShowIsc.isChecked()) {
            getSupportFragmentManager().beginTransaction().remove(mParticipantListFragment).commit();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("ParticipantListFragment");
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }
        }
        if (mIscParticipantListFragment != null && mIscParticipantListFragment.isAdded() && mCbShowIsc.isChecked()) {
            getSupportFragmentManager().beginTransaction().remove(mIscParticipantListFragment).commit();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("IscParticipantListFragment");
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }
        }
        closeCameraSettings();
        if (mAudioDialog != null && mAudioDialog.isShowing()) {
            mAudioDialog.dismiss();
        }
        if (mVideoDeviceDialog != null && mVideoDeviceDialog.isShowing()) {
            mVideoDeviceDialog.dismiss();
        }
        if (mVideoLayoutDialog != null && mVideoLayoutDialog.isShowing()) {
            mVideoLayoutDialog.dismiss();
        }
    }

    private void hideProgress() {
        mTvProgressMsg.setVisibility(View.GONE);
        mTvProgressMsg.setText("");
    }

    private void toggleAudioMuteUnMuteView(boolean isMuted) {
        int resID = isMuted ? R.drawable.mic_off_black : R.drawable.mic_on_black;
        mIvMic.setImageResource(resID);
    }

    private void toggleVideoMuteUnMuteView(boolean isMuted) {
        int resID = isMuted ? R.drawable.videocam_off_black : R.drawable.videocam_on_black;
        mIvVideo.setImageResource(resID);
        if (isMuted) {
            closeCameraSettings();
            mCameraSettings.setVisibility(View.GONE);
        } else {
            mCameraSettings.setVisibility(View.VISIBLE);
        }
    }

    private void closeCameraSettings() {
        mZoomScaleFactor = 1;
        if (mCameraSettingsDialog != null && mCameraSettingsDialog.isShowing()) {
            mCameraSettingsDialog.dismiss();
        }
    }

    private void configurePortraitView() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mSelfView.getLayoutParams();
        params.startToStart = R.id.parent_layout;
        params.topToTop = R.id.parent_layout;
        params.endToEnd = R.id.parent_layout;
        params.dimensionRatio = getResources().getString(R.string.self_view_ratio);
        mSelfView.requestLayout();
    }

    private void configureLandscapeView() {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mSelfView.getLayoutParams();
        params.startToStart = ConstraintLayout.LayoutParams.UNSET;
        params.topToTop = R.id.parent_layout;
        params.endToEnd = R.id.parent_layout;
        params.dimensionRatio = getResources().getString(R.string.self_view_ratio);
        mSelfView.requestLayout();
    }

    private void handleRemoteContentState() {
        if (mIsRemoteContentAvailable) {
            hideProgress();
        }
    }

    private final IMenuCallback mIOptionMenuCallback =
            new IMenuCallback() {
                @Override
                public void showVideoLayoutView(String videoLayoutName) {
                    updateCurrentVideoLayoutForAlertDialog(videoLayoutName);
                    showVideoLayoutDialog();
                }

                @Override
                public void showAudioDeviceView() {
                    showAudioDeviceDialog();
                }

                @Override
                public void showVideoDeviceView() {
                    showVideoDeviceDialog();
                }

                @Override
                public void showIscStreamStyleView() {
                    if (mCbShowIsc.isChecked() && getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof IscGalleryFragment) {
                        showStreamStyleDialog();
                    } else {
                        showToastMessage(getResources().getString(R.string.no_stream_styles));
                    }
                }

                @Override
                public void showVideoStreamUseCases() {
                    if (mMeetingService.getVideoLayout().getValue() != MeetingService.VideoLayout.Custom.INSTANCE) {
                        mMeetingService.setVideoLayout(MeetingService.VideoLayout.Custom.INSTANCE);
                    }
                    showStreamUseCasesDialog();
                }

                @Override
                public void handleClosedCaptionSwitchEvent(Boolean enabled) {
                    if (enabled) {
                        mMeetingService.getClosedCaptioningService().startClosedCaptioning();
                        mTvClosedCaption.setVisibility(View.VISIBLE);
                    } else {
                        mMeetingService.getClosedCaptioningService().stopClosedCaptioning();
                        mTvClosedCaption.setVisibility(View.GONE);
                    }
                }

                @Override
                public void handleHDCaptureSwitchEvent(Boolean isChecked) {
                    Log.d(TAG, "Enabling hdCapture " + isChecked);
                    mVideoDeviceService.set720pVideoCaptureEnabled(isChecked);
                }

                @Override
                public void handleHDReceiveSwitchEvent(Boolean isChecked) {
                    Log.d(TAG, "Enabling hdReceive " + isChecked);
                    mVideoDeviceService.set720pVideoReceiveEnabled(isChecked);
                }

                @Override
                public void showWaitingRoom() {
                    showWaitingRoomDialog();
                }

                @Override
                public void setWaitingRoomEnabled(boolean enabled) {
                    Log.i(TAG, "WR status: " + enabled);
                    mMeetingService.getModeratorWaitingRoomService().setWaitingRoomEnabled(enabled);
                }

                @Override
                public void setCustomVideoSource(boolean isCustom) {
                    mIsVideoSourceCustom = isCustom;
                    mBottomSheetFragment.updateCustomVideoSwitchState(mIsVideoSourceCustom);
                    if (isCustom) {
                        mAugmentedFacesFragment = null;
                        mAugmentedFacesFragment = new AugmentedFacesFragment();

                        mSelfView.setVisibility(View.GONE);
                        mCustomVideoFragmentContainer.setVisibility(View.VISIBLE);
                        getSupportFragmentManager().beginTransaction()
                                .replace(mCustomVideoFragmentContainer.getId(), mAugmentedFacesFragment)
                                .commit();
                    } else {
                        getSupportFragmentManager().beginTransaction()
                                .remove(mAugmentedFacesFragment)
                                .commit();
                        mCustomVideoFragmentContainer.setVisibility(View.GONE);
                        mSelfView.setVisibility(View.VISIBLE);
                    }
                }
            };

    private ArrayList<String> videoLayoutOptionList() {
        ArrayList<String> videoLayoutList = new ArrayList<>();
        videoLayoutList.add(getString(R.string.people_view));
        videoLayoutList.add(getString(R.string.speaker_view));
        videoLayoutList.add(getString(R.string.gallery_view));
        videoLayoutList.add(getString(R.string.custom_view));
        return videoLayoutList;
    }

    private void showVideoLayoutDialog() {
        mVideoLayoutDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.video_layouts))
                .setAdapter(mVideoLayoutAdapter,
                        (dialog, which) -> selectVideoLayout(which)).create();
        mVideoLayoutDialog.show();
    }

    private void showAudioDeviceDialog() {
        mAudioDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.audio_devices))
                .setAdapter(mAudioDeviceAdapter,
                        (dialog, which) -> selectAudioDevice(which)).create();
        mAudioDialog.show();
    }

    private void showVideoDeviceDialog() {
        mVideoDeviceDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.video_devices))
                .setAdapter(mVideoDeviceAdapter,
                        (dialog, which) -> selectVideoDevice(which)).create();
        mVideoDeviceDialog.show();
    }

    private void showStreamStyleDialog() {
        mStreamStyleDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.video_stream_styles))
                .setAdapter(mStreamStyleAdapter,
                        (dialog, which) -> selectStreamStyle(which)).create();
        mStreamStyleDialog.show();
    }

    private void showStreamUseCasesDialog() {
        mStreamUseCasesDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.isc_use_case))
                .setAdapter(mUseCasesAdapter,
                        (dialog, which) -> selectVideoStreamUseCase(which)).create();
        mStreamUseCasesDialog.show();
    }

    private void showWaitingRoomDialog() {
        List<ParticipantsService.Participant> waitingRoomParticipants = mMeetingService.getModeratorWaitingRoomService().getWaitingRoomParticipants().getValue();
        if (waitingRoomParticipants != null && waitingRoomParticipants.size() > 0) {
            WaitingRoomDialog.newInstance(waitingRoomParticipants, mMeetingService)
                    .show(getSupportFragmentManager(), "WaitingRoomDialog");
        } else {
            showToastMessage(getString(R.string.no_wr_participants));
        }
    }

    private void updateCurrentVideoDeviceForAlertDialog(VideoDevice videoDevice) {
        List<VideoDevice> videoDevices = mVideoDeviceService.getVideoDevices().getValue();
        if (videoDevices != null) {
            mVideoDeviceAdapter.updateSelectedPosition(videoDevices.indexOf(videoDevice));
        }
    }

    private void updateCurrentAudioDeviceForAlertDialog(AudioDevice currentAudioDevice) {
        List<AudioDevice> audioDevices = mMeetingService.getAudioDeviceService().getAudioDevices().getValue();
        if (audioDevices != null) {
            mAudioDeviceAdapter.updateSelectedPosition(audioDevices.indexOf(currentAudioDevice));
        }
    }

    private void updateCurrentVideoLayoutForAlertDialog(String videoLayoutName) {
        mVideoLayoutAdapter.updateSelectedPosition(videoLayoutOptionList().indexOf(videoLayoutName));
    }

    private VideoLayoutAdapter getVideoLayoutAdapter() {
        return new VideoLayoutAdapter(this, android.R.layout.simple_list_item_single_choice,
                videoLayoutOptionList());
    }

    private VideoDeviceAdapter getVideoDeviceAdapter(List<VideoDevice> videoDevices) {
        return new VideoDeviceAdapter(this, android.R.layout.simple_list_item_single_choice,
                videoDevices);
    }

    private AudioDeviceAdapter getAudioDeviceAdapter(List<AudioDevice> audioDevices) {
        return new AudioDeviceAdapter(this, android.R.layout.simple_list_item_single_choice,
                audioDevices);
    }

    private VideoStreamStyleAdapter getStreamStyleAdapter(List<String> styles) {
        return new VideoStreamStyleAdapter(this, android.R.layout.simple_list_item_single_choice,
                styles);
    }

    private VideoStreamUseCasesAdapter getVideoStreamUseCasesAdapter(List<String> useCases) {
        return new VideoStreamUseCasesAdapter(this, android.R.layout.simple_list_item_single_choice,
                useCases);
    }

    private void setVideoLayout(String videoLayoutName) {
        MeetingService.VideoLayout videoLayout = null;
        if (videoLayoutName.equals(getString(R.string.people_view))) {
            videoLayout = MeetingService.VideoLayout.People.INSTANCE;
        } else if (videoLayoutName.equals(getString(R.string.gallery_view))) {
            videoLayout = MeetingService.VideoLayout.Gallery.INSTANCE;
        } else if (videoLayoutName.equals(getString(R.string.speaker_view))) {
            videoLayout = MeetingService.VideoLayout.Speaker.INSTANCE;
        } else if (videoLayoutName.equals(getString(R.string.custom_view))) {
            videoLayout = MeetingService.VideoLayout.Custom.INSTANCE;
        }
        if (videoLayout != null) {
            if (mCbShowIsc.isChecked() && (getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof IscGalleryFragment ||
                    getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof RemoteLearningFragment ||
                    getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof RemoteAssistFragment)) {
                if (videoLayout != MeetingService.VideoLayout.Custom.INSTANCE) {
                    mIsIscEnabled = false;
                    replaceInMeetingFragment(false);
                }
            } else if (mCbShowIsc.isChecked() && videoLayout == MeetingService.VideoLayout.Custom.INSTANCE) {
                mIsIscEnabled = true;
                replaceInMeetingFragment(true);
            } else if (!mCbShowIsc.isChecked() && videoLayout == MeetingService.VideoLayout.Custom.INSTANCE) {
                showToastMessage(getString(R.string.no_individual_streams));
                return;
            }
            mMeetingService.setVideoLayout(videoLayout);
            if (mBottomSheetFragment != null) {
                mBottomSheetFragment.updateVideoLayout(videoLayoutName);
                updateCurrentVideoLayoutForAlertDialog(videoLayoutName);
            }

            if (!videoLayoutName.equals(getString(R.string.custom_view)) && mSelfView.getVisibility() == View.GONE) {
                mSelfView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void selectAudioDevice(int position) {
        AudioDevice audioDevice = mAudioDeviceAdapter.getItem(position);
        mAudioDeviceAdapter.updateSelectedPosition(position);
        mMeetingService.getAudioDeviceService().selectAudioDevice(audioDevice);
    }

    private void selectVideoDevice(int position) {
        mVideoDeviceAdapter.updateSelectedPosition(position);
        VideoDevice videoDevice = mVideoDeviceAdapter.getItem(position);
        closeCameraSettings();
        mVideoDeviceService.selectVideoDevice(videoDevice);
    }

    private void selectStreamStyle(int position) {
        mStreamStyleAdapter.updateSelectedPosition(position);
        String style = mStreamStyleAdapter.getItem(position);
        if (position == 0) {
            mMeetingService.getVideoStreamService()
                    .setVideoStreamStyle(VideoStreamStyle.FIT_TO_VIEW);
        } else {
            mMeetingService.getVideoStreamService()
                    .setVideoStreamStyle(VideoStreamStyle.SCALE_AND_CROP);
        }

        if (getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof IscGalleryFragment &&
                iscGalleryFragment != null) {
            iscGalleryFragment.setVideoStreamStyle();
        }

        mBottomSheetFragment.updateVideoStreamStyle(style);
    }

    private void selectVideoStreamUseCase(int position) {
        mUseCasesAdapter.updateSelectedPosition(position);
        mMeetingService.getVideoStreamService().setVideoStreamStyle(VideoStreamStyle.FIT_TO_VIEW);
        mStreamStyleAdapter.updateSelectedPosition(0);
        mBottomSheetFragment.updateVideoStreamStyle(getResources().getStringArray(R.array.stream_styles)[0]);
        switch (position) {
            case 0:
                if (iscGalleryFragment != null) iscGalleryFragment.onDestroy();
                if (inMeetingFragment != null) inMeetingFragment.onDestroyView();
                if (remoteLearningFragment != null) remoteLearningFragment.onDestroy();
                if (remoteAssistFragment != null) remoteAssistFragment.onDestroy();

                iscGalleryFragment = new IscGalleryFragment();
                inMeetingFragment = new InMeetingFragment();
                remoteLearningFragment = new RemoteLearningFragment();
                mSelfView.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.inMeetingFragmentContainer, remoteLearningFragment)
                        .commit();
                break;

            case 1:
                if (iscGalleryFragment != null) iscGalleryFragment.onDestroy();
                if (inMeetingFragment != null) inMeetingFragment.onDestroyView();
                if (remoteLearningFragment != null) remoteLearningFragment.onDestroy();
                if (remoteAssistFragment != null) remoteAssistFragment.onDestroy();

                iscGalleryFragment = new IscGalleryFragment();
                inMeetingFragment = new InMeetingFragment();
                remoteAssistFragment = new RemoteAssistFragment();
                mSelfView.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.inMeetingFragmentContainer, remoteAssistFragment)
                        .commit();
                break;
        }
    }

    private void selectVideoLayout(int position) {
        mVideoLayoutAdapter.updateSelectedPosition(position);
        setVideoLayout(mVideoLayoutAdapter.getItem(position));
    }

    public void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showCameraSettingsDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        zoomSeekBar = new SeekBar(this);
        zoomSeekBar.setMax(10);
        zoomSeekBar.setMin(1);
        zoomSeekBar.setProgress(mZoomScaleFactor);
        builder.setTitle(getString(R.string.camera_setting_title));
        builder.setView(zoomSeekBar);
        try {
            CameraCharacteristics cameraCharacteristics = getCurrentCameraCharacteristics();
            zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mZoomScaleFactor = progress;
                    Rect activeRegion = cameraCharacteristics
                            .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    mVideoDeviceService.setRepeatingCaptureRequest(CaptureRequest.SCALER_CROP_REGION,
                            getCropRegionForZoom(activeRegion, progress), null);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mCameraSettingsDialog = builder.create();
        mCameraSettingsDialog.show();
    }

    private void handleMeetingState(MeetingService.MeetingState meetingState) {
        if (meetingState instanceof MeetingService.MeetingState.Connected) {
            // add this flag to avoid screen shots.
            // This also allows protection of screen during screen casts from 3rd party apps.
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            if (!mIsCallInProgress) {
                if (mCbShowIsc.isChecked()) {
                    mMeetingService.setVideoLayout(MeetingService.VideoLayout.Custom.INSTANCE);
                }
                OnGoingMeetingService.startService(getApplicationContext());
                activateInMeetingSubscriptions();
            }
            hideProgress();
            showInMeetingFragment(mCbShowIsc.isChecked());
            if (mIsReconnecting) {
                mIsReconnecting = false;
                showToastMessage(getString(R.string.reconnected));
            }
        } else if (meetingState instanceof MeetingService.MeetingState.Idle) {
            endMeeting();
            removeInMeetingFragment();
            mIsInWaitingRoom = false;
            mCurrentPinnedParticipant = null;
            showWaitingRoomUI();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else if (meetingState instanceof MeetingService.MeetingState.WaitingRoom) {
            removeInMeetingFragment();
            mIsInWaitingRoom = true;
            mInMeetingDisposable.clear();
            mIsCallInProgress = false;
            showOutOfMeetingView();
            showWaitingRoomUI();
        } else if (meetingState instanceof MeetingService.MeetingState.Connecting) {
            mIsInWaitingRoom = false;
            showWaitingRoomUI();
            showInMeetingView();
            mEtPassCode.setVisibility(View.GONE);
            mEtName.setVisibility(View.GONE);
            mEtEventId.setVisibility(View.GONE);
            btnJoin.setVisibility(View.GONE);
            mJoinLayout.setVisibility(View.GONE);
            mTvProgressMsg.setVisibility(View.VISIBLE);
            mTvProgressMsg.setText("Connecting...");
        } else if (meetingState instanceof MeetingService.MeetingState.Reconnecting) {
            mIsReconnecting = true;
            showToastMessage(getString(R.string.reconnecting));
        }
    }

    /**
     * It calculates the new crop region by finding out the delta between active camera region's
     * x and y coordinates and divide by zoom scale factor to get updated camera's region.
     *
     * @param cameraActiveRegion active area of the image sensor.
     * @param zoomFactor         scale factor
     * @return Rect coordinates of crop region to be zoomed.
     */
    private Rect getCropRegionForZoom(Rect cameraActiveRegion, int zoomFactor) {
        int xCenter = cameraActiveRegion.width() / 2;
        int yCenter = cameraActiveRegion.height() / 2;
        int xDelta = (int) (0.5f * cameraActiveRegion.width() / zoomFactor);
        int yDelta = (int) (0.5f * cameraActiveRegion.height() / zoomFactor);
        return new Rect(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta,
                yCenter + yDelta);
    }

    private CameraCharacteristics getCurrentCameraCharacteristics() throws CameraAccessException {
        VideoDevice currentVideoDevice = mVideoDeviceService.getCurrentVideoDevice().getValue();
        if (currentVideoDevice != null) {
            String cameraId = mVideoDeviceService.getCurrentVideoDevice().getValue().getId();
            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            return cameraManager.getCameraCharacteristics(cameraId);
        } else {
            Log.e(TAG, "No active camera device");
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
        }
    }

    private void showWaitingRoomUI() {
        if (mIsInWaitingRoom) {
            mEtPassCode.setVisibility(View.GONE);
            mEtName.setVisibility(View.GONE);
            mEtEventId.setVisibility(View.GONE);
            btnJoin.setVisibility(View.GONE);
            mJoinLayout.setVisibility(View.GONE);
            mWaitingRoomLayout.setVisibility(View.VISIBLE);

            if (mMeetingService.getMeetingInformation().getValue() != null
                    && mMeetingService.getMeetingInformation().getValue().getMeetingTitle() != null) {
                mTvWaitingRoom.setText(
                        mMeetingService.getMeetingInformation().getValue().getMeetingTitle()
                );
            }
        } else {
            mEtPassCode.setVisibility(View.VISIBLE);
            mEtName.setVisibility(View.VISIBLE);
            mEtEventId.setVisibility(View.VISIBLE);
            btnJoin.setVisibility(View.VISIBLE);
            mJoinLayout.setVisibility(View.VISIBLE);
            mWaitingRoomLayout.setVisibility(View.GONE);
        }
        hideProgress();
    }

    private Boolean isVideoStreamRoster() {
        return getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof IscGalleryFragment ||
                getSupportFragmentManager().findFragmentById(R.id.inMeetingFragmentContainer) instanceof RemoteAssistFragment;
    }
}