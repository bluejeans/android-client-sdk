/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentContainerView
import com.bjnclientcore.inmeeting.contentshare.ContentShareType
import com.bjnclientcore.media.VideoSource
import com.bjnclientcore.media.individualstream.StreamPriority
import com.bjnclientcore.media.individualstream.StreamQuality
import com.bjnclientcore.media.individualstream.VideoStreamStyle
import com.bjnclientcore.ui.util.extensions.gone
import com.bjnclientcore.ui.util.extensions.visible
import com.bluejeans.android.sdksample.MeetingNotificationUtility.updateNotificationMessage
import com.bluejeans.android.sdksample.ar.augmentedfaces.AugmentedFacesFragment
import com.bluejeans.android.sdksample.databinding.ActivityMainBinding
import com.bluejeans.android.sdksample.dialog.WaitingRoomDialog
import com.bluejeans.android.sdksample.isc.IscGalleryFragment
import com.bluejeans.android.sdksample.isc.IscParticipantListAdapter
import com.bluejeans.android.sdksample.isc.usecases.RemoteAssistFragment
import com.bluejeans.android.sdksample.isc.usecases.RemoteLearningFragment
import com.bluejeans.android.sdksample.menu.MenuFragment
import com.bluejeans.android.sdksample.menu.adapter.MenuItemAdapter
import com.bluejeans.android.sdksample.participantlist.IscParticipantListFragment
import com.bluejeans.android.sdksample.participantlist.ParticipantListFragment
import com.bluejeans.android.sdksample.utils.AudioDeviceHelper.Companion.getAudioDeviceName
import com.bluejeans.bluejeanssdk.devices.AudioDevice
import com.bluejeans.bluejeanssdk.devices.VideoDevice
import com.bluejeans.bluejeanssdk.logging.LoggingService
import com.bluejeans.bluejeanssdk.meeting.*
import com.bluejeans.bluejeanssdk.permission.PermissionService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val permissionService = SampleApplication.blueJeansSDK.permissionService
    private val meetingService = SampleApplication.blueJeansSDK.meetingService
    private val videoDeviceService = SampleApplication.blueJeansSDK.videoDeviceService
    private val loggingService = SampleApplication.blueJeansSDK.loggingService
    private val customVideoSourceService = SampleApplication.blueJeansSDK.customVideoSourceService
    private val appVersionString = "v" + SampleApplication.blueJeansSDK.version
    private val disposable = CompositeDisposable()
    private val inMeetingDisposable = CompositeDisposable()

    private var bottomSheetFragment: MenuFragment? = null
    private var participantListFragment: ParticipantListFragment? = null
    private var iscParticipantListFragment: IscParticipantListFragment? = null
    private var videoDeviceAdapter: MenuItemAdapter<VideoDevice>? = null
    private var audioDeviceAdapter: MenuItemAdapter<AudioDevice>? = null
    private var iscStreamStyleAdapter: MenuItemAdapter<String>? = null
    private var iscUseCasesAdapter: MenuItemAdapter<String>? = null
    private var videoLayoutAdapter: MenuItemAdapter<String>? = null
    private var currentVideoLayout: MeetingService.VideoLayout = MeetingService.VideoLayout.Speaker
    private var audioDeviceDialog: AlertDialog? = null
    private var videoDeviceDialog: AlertDialog? = null
    private var videoLayoutDialog: AlertDialog? = null
    private var iscUseCasesDialog: AlertDialog? = null
    private var cameraSettingsDialog: AlertDialog? = null
    private var uploadLogsDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var zoomSeekBar: SeekBar? = null

    private lateinit var binding: ActivityMainBinding
    private var zoomScaleFactor = 1 // default value of 1, no zoom to start with

    private var isAudioMuted = false
    private var isVideoMuted = false
    private var isVideoMutedBeforeBackgrounding = false
    private var isVideoSourceCustom = false
    private var isRemoteContentAvailable = false
    private var isInWaitingRoom = false
    private var isWaitingRoomEnabled = false
    private var isScreenShareInProgress = false
    private var isReconnecting = false
    private var isCallInProgress = false

    private var iscGalleryFragment: IscGalleryFragment? = null
    private var inMeetingFragment: InMeetingFragment? = null

    private var currentPinnedParticipant: String? = null
    private var remoteLearningFragment: RemoteLearningFragment? = null
    private var remoteAssistFragment: RemoteAssistFragment? = null

    private var augmentedFacesFragment: AugmentedFacesFragment? = null

    private val streamConfigUpdatedCallback =
        object : IscParticipantListAdapter.IStreamConfigUpdatedCallback {
            override fun onStreamConfigUpdated(
                participantGuid: String,
                streamQuality: StreamQuality,
                streamPriority: StreamPriority
            ) {
                if (supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is IscGalleryFragment) {
                    iscGalleryFragment?.updateStreamConfigurations(
                        participantGuid,
                        streamQuality,
                        streamPriority
                    )
                }
            }

            override fun pinParticipant(participantId: String, isPinned: Boolean) {
                if (supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is IscGalleryFragment) {
                    iscGalleryFragment?.let { fragment ->
                        fragment.pinParticipant(participantId, isPinned)
                        if (isPinned) {
                            currentPinnedParticipant = participantId
                            iscParticipantListFragment?.setPinnedParticipant(participantId)
                        } else {
                            currentPinnedParticipant = null
                            iscParticipantListFragment?.setPinnedParticipant(null)
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permissionService.register(this)
        initViews()
        checkCameraPermissionAndStartSelfVideo()
        activateSDKSubscriptions()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> configurePortraitView()
            else -> configureLandscapeView()
        }
    }

    override fun onResume() {
        super.onResume()
        meetingService.setVideoMuted(isVideoMutedBeforeBackgrounding)
        if (!isScreenShareInProgress) {
            handleMeetingState(meetingService.meetingState.value)
        }
        meetingService.requestAudioFocus()
    }

    override fun onDestroy() {
        Timber.tag(TAG).i("onDestroy")
        if (meetingService.meetingState.value == MeetingService.MeetingState.Connected) {
            meetingService.endMeeting()
            endMeeting()
        }
        disposable.dispose()
        inMeetingDisposable.dispose()
        bottomSheetFragment = null
        isCallInProgress = false
        super.onDestroy()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_join -> {
                if (meetingService.meetingState.value == MeetingService.MeetingState.Idle) {
                    checkAllPermissionsAndJoin()
                } else {
                    showToastMessage(getString(R.string.meeting_in_progress))
                }
            }
            R.id.ivClose -> {
                meetingService.endMeeting()
                endMeeting()
            }
            R.id.ivMic -> {
                isAudioMuted = !isAudioMuted
                meetingService.setAudioMuted(isAudioMuted)
                toggleAudioMuteUnMuteView(isAudioMuted)
            }
            R.id.ivVideo -> {
                isVideoMuted = !isVideoMuted
                meetingService.setVideoMuted(isVideoMuted)
                toggleVideoMuteUnMuteView(isVideoMuted)
            }
            R.id.ivMenuOption -> {
                bottomSheetFragment?.let {
                    it.show(supportFragmentManager, it.tag)
                }
            }
            R.id.ivRoster -> {
                if (isVideoStreamRoster()) {
                    iscParticipantListFragment?.let {
                        iscParticipantListFragment?.setPinnedParticipant(currentPinnedParticipant)
                        supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.rosterContainer, it)
                            .addToBackStack("IscParticipantListFragment")
                            .commit()
                    }
                } else {
                    participantListFragment?.let {
                        supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.rosterContainer, it)
                            .addToBackStack("ParticipantListFragment")
                            .commit()
                    }
                }
            }
            R.id.ivScreenShare -> {
                if (meetingService.contentShareService.contentShareState.value is ContentShareState.Stopped) {
                    isScreenShareInProgress = true
                    val mediaProjectionManager =
                        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    startActivityLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                } else {
                    isScreenShareInProgress = false
                    meetingService.contentShareService.stopContentShare()
                }
            }
            R.id.ivCameraSettings -> showCameraSettingsDialog()
            R.id.ivUploadLogs -> showUploadLogsDialog()
            R.id.btn_exit_waiting_room -> {
                meetingService.endMeeting()
                endMeeting()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isVideoMutedBeforeBackgrounding = isVideoMuted
        meetingService.setVideoMuted(true)
    }

    private fun showUploadLogsDialog() {
        uploadLogsDialog = AlertDialog.Builder(this)
            .setView(R.layout.submit_log_dialog).create()
        uploadLogsDialog?.setCanceledOnTouchOutside(true)
        uploadLogsDialog?.show()
        val editText = uploadLogsDialog?.findViewById<EditText>(R.id.description)
        val submitButton = uploadLogsDialog?.findViewById<Button>(R.id.btn_submit)
        progressBar = uploadLogsDialog?.findViewById(R.id.progressBar)
        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                submitButton?.isEnabled = s.toString().trim().isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        submitButton?.setOnClickListener {
            val description = editText?.text.toString()
            submitButton.isEnabled = false
            progressBar?.visible()
            uploadLogs(description)
        }
    }

    private fun uploadLogs(comments: String?) {
        if (!comments.isNullOrEmpty()) {
            disposable.add(
                loggingService.uploadLog(comments, userName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { result ->
                            Log.i(TAG, "Log uploaded successfully $result")
                            if (result !== LoggingService.LogUploadResult.Success) {
                                showToastMessage(getString(R.string.upload_logs_failure))
                            } else {
                                showToastMessage(getString(R.string.upload_logs_success))
                            }
                            uploadLogsDialog?.dismiss()
                        },
                        {
                            Log.e(
                                TAG,
                                "Error while uploading logs ${it.message}"
                            )
                            progressBar?.gone()
                            Toast.makeText(
                                this, getString(R.string.upload_logs_failure),
                                Toast.LENGTH_SHORT
                            ).show()
                        })
            )
        } else {
            showToastMessage("Please enter your comments.")
        }
    }

    private fun showToastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val startActivityLauncher = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { ContentShareType.Screen(it) }?.let {
                meetingService.contentShareService.startContentShare(it)
            }
        }
    }

    private fun activateSDKSubscriptions() {
        subscribeForMeetingStatus()
        subscribeToWaitingRoomEvents()
        subscribeForVideoDevices()
        subscribeForAudioDevices()
        subscribeForCurrentAudioDevice()
        subscribeForCurrentVideoDevice()
    }

    private fun activateInMeetingSubscriptions() {
        subscribeForVideoMuteStatus()
        subscribeForAudioMuteStatus()
        subscribeForVideoLayout()
        subscribeForParticipants()
        subscribeForContentShareState()
        subscribeForContentShareAvailability()
        subscribeForContentShareEvents()
        subscribeForClosedCaptionText()
        subscribeForClosedCaptionState()
        subscribeForHDCaptureState()
        subscribeToActiveSpeaker()
        subscribeForModeratorWaitingRoomEvents()

        isCallInProgress = true
    }

    private fun checkCameraPermissionAndStartSelfVideo() {
        when {
            permissionService.hasPermission(PermissionService.Permission.Camera) -> startSelfVideo()
            else -> {
                disposable.add(permissionService.requestPermissions(arrayOf(PermissionService.Permission.Camera))
                    .subscribe(
                        { status ->
                            when (status) {
                                PermissionService.RequestStatus.Granted -> startSelfVideo()
                                PermissionService.RequestStatus.NotGranted -> onMultiplePermissionDenied()
                                PermissionService.RequestStatus.NotRegistered -> {
                                    Timber.tag(TAG).i("Permission Service not registered.")
                                    Toast.makeText(
                                        this,
                                        "Permission Service not registered.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Timber.tag(TAG).e("Error in requesting permission subscription")
                    })
            }
        }
    }

    private fun startSelfVideo() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.selfViewFrame, videoDeviceService.selfVideoFragment)
            .commit()
        meetingService.setVideoMuted(false)
    }

    private fun checkAllPermissionsAndJoin() {
        if (permissionService.hasAllPermissions()) {
            hideKeyboard()
            joinMeeting()
        } else {
            requestAllPermissionsAndJoin()
        }
    }

    private fun requestAllPermissionsAndJoin() {
        disposable.add(permissionService
            .requestAllPermissions()
            .subscribe(
                { status ->
                    when (status) {
                        PermissionService.RequestStatus.Granted -> joinMeeting()
                        PermissionService.RequestStatus.NotGranted -> onMultiplePermissionDenied()
                        PermissionService.RequestStatus.NotRegistered -> {
                            Timber.tag(TAG).i("Permission Service not registered.")
                            Toast.makeText(
                                this,
                                "Permission Service not registered.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            ) { Timber.tag(TAG).e("Error in requesting permissions subscription") })
    }

    private fun onMultiplePermissionDenied() {
        val deniedPermissions = StringBuilder()
        if (permissionService.listOfDeniedPermission.size > 0) {
            permissionService.listOfDeniedPermission.forEach { permission ->
                deniedPermissions.append("$permission , ")
            }
            val deniedPermissionsString =
                deniedPermissions.substring(0, deniedPermissions.lastIndexOf(","))
            Timber.tag(TAG).i("User Didn't grant $deniedPermissionsString even after asking.")
            Toast.makeText(
                this,
                "User Didn't grant $deniedPermissionsString even after asking.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun joinMeeting() {
        val meetingId = binding.joinInfo.etEventId.text.toString()
        val passcode = binding.joinInfo.etPasscode.text.toString()
        val name = when {
            TextUtils.isEmpty(binding.joinInfo.etName.text.toString()) -> "AndroidSDK"
            else -> binding.joinInfo.etName.text.toString()
        }
        Timber.tag(TAG).i("joinMeeting meetingId = $meetingId passcode = $passcode")
        showJoiningInProgressView()
        inMeetingDisposable.add(
            meetingService.joinMeeting(
                MeetingService.JoinParams(meetingId, passcode, name)
            ).subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        when (result) {
                            MeetingService.JoinResult.Success -> {
                            }
                            else -> Timber.tag(TAG).i("Join result: $result")
                        }
                    },
                    { showOutOfMeetingView() })
        )
    }

    private fun endMeeting() {
        Timber.tag(TAG).i("endMeeting")
        isReconnecting = false

        if (binding.selfView.root.visibility == View.GONE) {
            binding.selfView.root.visibility = View.VISIBLE
        }

        if (isInWaitingRoom) {
            isInWaitingRoom = false
            showWaitingRoomUI()
        } else {
            showOutOfMeetingView()
        }

        cameraSettingsDialog?.dismiss()
        inMeetingDisposable.clear()
        isRemoteContentAvailable = false
        isCallInProgress = false
        OnGoingMeetingService.stopService(this)
    }

    private fun subscribeForMeetingStatus() {
        disposable.add(
            meetingService.meetingState.rxObservable.observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.i(TAG, "State: $it")
                    handleMeetingState(it)
                }, {
                    Log.e(TAG, "${it.message}")
                })
        )
    }

    private fun subscribeToWaitingRoomEvents() {
        disposable.add(
            meetingService.waitingRoomEvent.observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    when (it) {
                        MeetingService.WaitingRoomEvent.Admitted -> showToastMessage("Moderator has approved")
                        MeetingService.WaitingRoomEvent.Denied -> showToastMessage("Denied by moderator")
                        MeetingService.WaitingRoomEvent.Demoted -> {
                            showToastMessage("Demoted by moderator")
                        }
                        else -> Timber.tag(TAG).i("Unrecognized event: $it")
                    }
                }, {
                    Timber.tag(TAG).e(it.message)
                })
        )
    }

    private fun subscribeForAudioMuteStatus() {
        inMeetingDisposable.add(meetingService.audioMuted.subscribe(
            { isMuted ->
                // This could be due to local mute or remote mute
                Timber.tag(TAG).i(" Audio Mute state $isMuted")
                isMuted?.let {
                    isAudioMuted = it
                }
                isMuted?.let { toggleAudioMuteUnMuteView(it) }
            }
        ) {
            Timber.tag(TAG).e("Error in audio mute subscription")
        })
    }

    private fun subscribeForVideoMuteStatus() {
        inMeetingDisposable.add(meetingService.videoMuted.subscribeOnUI(
            { isMuted ->
                // This could be due to local mute or remote mute
                Timber.tag(TAG).i(" Video Mute state $isMuted")
                isMuted?.let {
                    isVideoMuted = it
                }
                isMuted?.let { toggleVideoMuteUnMuteView(it) }
            }
        ) {
            Timber.tag(TAG).e("Error in video mute subscription")
        })
    }

    private fun subscribeForVideoLayout() {
        inMeetingDisposable.add(meetingService.videoLayout.subscribe(
            { videoLayout ->
                if (videoLayout != null) {
                    if (supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is IscGalleryFragment &&
                            videoLayout != MeetingService.VideoLayout.Custom) {
                        replaceInMeetingFragment(false)
                    }
                    currentVideoLayout = videoLayout
                    val videoLayoutName = getVideoLayoutDisplayName(videoLayout)
                    bottomSheetFragment?.updateVideoLayout(videoLayoutName)
                    updateCurrentVideoLayoutForAlertDialog(videoLayoutName)

                    if (binding.selfView.root.visibility == View.GONE) {
                        binding.selfView.root.visibility = View.VISIBLE
                    }
                }
            }
        ) {
            Timber.tag(TAG).e("Error in video layout subscription $it")
        })
    }

    private fun subscribeForCurrentAudioDevice() {
        disposable.add(meetingService.audioDeviceService.currentAudioDevice.subscribeOnUI(
            { currentAudioDevice ->
                if (currentAudioDevice != null) {
                    getAudioDeviceName(currentAudioDevice).let {
                        bottomSheetFragment?.updateAudioDevice(it)
                    }
                    updateCurrentAudioDeviceForAlertDialog(currentAudioDevice)
                }
            }
        ) {
            Timber.tag(TAG).e("Error in current audio device subscription $it")
        })
    }

    private fun subscribeForAudioDevices() {
        disposable.add(
            meetingService.audioDeviceService.audioDevices.subscribeOnUI({ audioDevices ->
                if (audioDevices != null) {
                    audioDeviceAdapter?.let {
                        it.clear()
                        it.addAll(audioDevices)
                        it.notifyDataSetChanged()
                    }
                }
            }) {
                Timber.tag(TAG).e("Error in audio devices subscription $it")
            })
    }

    private fun subscribeForCurrentVideoDevice() {
        disposable.add(videoDeviceService.currentVideoDevice.subscribeOnUI(
            { currentVideoDevice ->
                if (currentVideoDevice != null) {
                    bottomSheetFragment?.updateVideoDevice(currentVideoDevice.name)
                    updateCurrentVideoDeviceForAlertDialog(currentVideoDevice)
                }
            }
        ) { err ->
            Timber.tag(TAG).e("Error in current video device subscription $err")
        })
    }

    private fun subscribeForParticipants() {
        inMeetingDisposable.add(meetingService.participantsService.participants.subscribeOnUI(
            {
                iscParticipantListFragment?.updateMeetingList(it)
                participantListFragment?.updateMeetingList(it)
            }
        ) { err ->
            Timber.tag(TAG).e("Error in Participants subscription${err.message}")
        })
    }

    private fun subscribeForContentShareAvailability() {
        inMeetingDisposable.add(meetingService.contentShareService.contentShareAvailability.subscribeOnUI(
            { contentShareAvailability: ContentShareAvailability? ->
                if (contentShareAvailability is ContentShareAvailability.Available) {
                    binding.ivScreenShare.visible()
                } else {
                    binding.ivScreenShare.gone()
                }
            }
        ) { err: Throwable ->
            Log.e(TAG, "Error in content share availability" + err.message)
            Unit
        })
    }

    private fun subscribeForContentShareState() {
        inMeetingDisposable.add(meetingService.contentShareService.contentShareState.subscribeOnUI(
            { contentShareState: ContentShareState? ->
                if (contentShareState is ContentShareState.Stopped) {
                    binding.ivScreenShare.isSelected = false
                    updateNotificationMessage(
                        this,
                        getString(R.string.meeting_notification_message)
                    )
                } else {
                    binding.ivScreenShare.isSelected = true
                    updateNotificationMessage(
                        this,
                        getString(R.string.screen_share_notification_message)
                    )
                }
            }
        ) { err: Throwable ->
            Log.e(
                TAG,
                "Error in content share state subscription $err.message"
            )
        })
    }

    private fun subscribeForContentShareEvents() {
        inMeetingDisposable.add(
            meetingService.contentShareService.contentShareEvent
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ contentShareEvent: ContentShareEvent ->
                    Log.i(
                        TAG,
                        "Content share event is $contentShareEvent"
                    )
                },
                    { err: Throwable ->
                        Log.e(
                            TAG,
                            "Error in content share events subscription" + err.message
                        )
                    })
        )
    }

    private fun subscribeForModeratorWaitingRoomEvents() {
        SampleApplication.blueJeansSDK.meetingService.moderatorControlsService.isModeratorControlsAvailable.value?.let { isModerator ->
            if (isModerator) {
                inMeetingDisposable.add(
                    meetingService.moderatorWaitingRoomService.isWaitingRoomEnabled.rxObservable.observeOn(
                        AndroidSchedulers.mainThread()
                    )
                        .subscribe(
                            {
                                it.value?.let { isChecked ->
                                    isWaitingRoomEnabled = isChecked
                                }
                            }, {
                                Timber.tag(TAG).e(it.message)
                            }
                        )
                )

                inMeetingDisposable.add(
                    meetingService.moderatorWaitingRoomService.waitingRoomParticipantEvents.rxObservable.observeOn(
                        AndroidSchedulers.mainThread()
                    )
                        .subscribe(
                            {
                                Log.i(TAG, "WAITING_ROOM: WR event is $it")
                                when (it.value) {
                                    is WaitingRoomParticipantEvent.Added -> {
                                        val event = it.value as WaitingRoomParticipantEvent.Added
                                        if (event.participants.size == 1) {
                                            Log.i(
                                                TAG,
                                                "WAITING_ROOM: ${event.participants[0].name} has arrived in the waiting room"
                                            )
                                            showToastMessage("${event.participants[0].name} has arrived in the waiting room")
                                        } else if (event.participants.size > 1) {
                                            Log.i(
                                                TAG,
                                                "WAITING_ROOM:  ${getString(R.string.multiple_participants_arrived_wr)}"
                                            )
                                            showToastMessage(getString(R.string.multiple_participants_arrived_wr))
                                        }
                                    }
                                    is WaitingRoomParticipantEvent.Removed -> {
                                        val event = it.value as WaitingRoomParticipantEvent.Removed
                                        if (event.participants.size == 1) {
                                            Log.i(
                                                TAG,
                                                "WAITING_ROOM: ${event.participants[0].name} has left the waiting room"
                                            )
                                            showToastMessage("${event.participants[0].name} has left the waiting room")
                                        } else if (event.participants.size > 1) {
                                            Log.i(
                                                TAG,
                                                "WAITING_ROOM: ${getString(R.string.multiple_participants_left_wr)}"
                                            )
                                            showToastMessage(getString(R.string.multiple_participants_left_wr))
                                        }
                                    }
                                    else -> Timber.tag(TAG).i("Unrecognized event")
                                }
                            }, {
                                Log.e(TAG, "${it.message}")
                            }
                        )
                )
            }
        }
    }

    private fun subscribeForVideoDevices() {
        disposable.add(
            videoDeviceService.videoDevices.subscribeOnUI({ videoDevices ->
                if (videoDevices != null) {
                    videoDeviceAdapter?.let {
                        it.clear()
                        it.addAll(videoDevices)
                        it.notifyDataSetChanged()
                    }
                }
            }
            ) {
                Timber.tag(TAG).e("Error in video devices subscription $it")
            })
    }

    private fun subscribeForClosedCaptionText() {
        inMeetingDisposable.add(
            meetingService.closedCaptioningService.closedCaptionText.subscribeOn(
                Schedulers.io()
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    binding.tvClosedCaption.text = it
                }, {
                    Log.e(TAG, "Error closed caption subscription $it")
                })
        )
    }

    private fun subscribeForClosedCaptionState() {
        inMeetingDisposable.add(
            meetingService.closedCaptioningService.closedCaptioningState
                .subscribeOnUI({
                    if (it == ClosedCaptioningService.ClosedCaptioningState.Started) {
                        bottomSheetFragment?.updateClosedCaptionSwitchState(true)
                        binding.tvClosedCaption.visible()
                    } else {
                        bottomSheetFragment?.updateClosedCaptionSwitchState(false)
                        binding.tvClosedCaption.gone()
                    }
                }, { Log.e(TAG, "Exception in subscribeForClosedCaptionState ${it.message}") })
        )
    }

    private fun subscribeForHDCaptureState() {
        inMeetingDisposable.add(
            videoDeviceService.is720pVideoCaptureEnabled
                .subscribeOnUI(
                    {
                        bottomSheetFragment?.updateHDCaptureState(it)
                        zoomSeekBar?.setProgress(1)
                    },
                    {
                        Timber.tag(TAG)
                            .e("Exception in subscribeForHDCaptureState ${it.stackTraceToString()}")
                    })
        )
    }

    private fun subscribeToActiveSpeaker() {
        inMeetingDisposable.add(
            meetingService.participantsService.activeSpeaker.rxObservable.observeOn(
                AndroidSchedulers.mainThread()
            )
                .subscribe({
                    it.value?.let { participant ->
                        Log.i(TAG, "${participant.name} is the active speaker")
                    } ?: kotlin.run {
                        Log.e(TAG, "Participant information is missing")
                    }
                }, {
                    Log.e(TAG, "Exception while subscribing to active speaker: ${it.message}")
                })
        )
    }

    private fun initViews() {
        val btnJoin = findViewById<Button>(R.id.btn_join)
        btnJoin.setOnClickListener(this)

        val btnExitWaitingRoom = findViewById<Button>(R.id.btn_exit_waiting_room)
        btnExitWaitingRoom.setOnClickListener(this)

        binding.ivClose.setOnClickListener(this)
        binding.selfView.ivMic.setOnClickListener(this)
        binding.selfView.ivVideo.setOnClickListener(this)
        binding.selfView.ivCameraSettings.setOnClickListener(this)
        binding.ivMenuOption.setOnClickListener(this)
        binding.ivRoster.setOnClickListener(this)
        binding.ivScreenShare.setOnClickListener(this)
        binding.ivUploadLogs.setOnClickListener(this)

        bottomSheetFragment = MenuFragment(
            mIOptionMenuCallback, isWaitingRoomEnabled, binding.joinInfo.cbShowIsc.isChecked
        )
        bottomSheetFragment?.updateVideoStreamStyle(resources.getStringArray(R.array.stream_styles)[0])
        participantListFragment = ParticipantListFragment()
        iscParticipantListFragment = IscParticipantListFragment(streamConfigUpdatedCallback)
        videoLayoutAdapter = getVideoLayoutAdapter()
        videoDeviceAdapter = getVideoDeviceAdapter(ArrayList())
        audioDeviceAdapter = getAudioDeviceAdapter(ArrayList())
        iscStreamStyleAdapter = getIscAdapter(resources.getStringArray(R.array.stream_styles))
        iscUseCasesAdapter = getIscAdapter(resources.getStringArray(R.array.isc_use_cases))
        binding.tvAppVersion.text = appVersionString
    }

    private fun showInMeetingFragment(showIscFragment: Boolean) {
        if (supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) == null) {
            Timber.tag(TAG)
                .i("${supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer)}")
            iscGalleryFragment = IscGalleryFragment()
            inMeetingFragment = InMeetingFragment()
            val inMeetingFragment =
                if (showIscFragment) iscGalleryFragment!! else inMeetingFragment!!
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.inMeetingFragmentContainer,
                    inMeetingFragment
                ).commit()
        } else {
            Timber.tag(TAG)
                .i("${supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer)}")
        }
    }

    private fun removeInMeetingFragment() {
        val inMeetingFragment =
            supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer)
        inMeetingFragment?.let {
            supportFragmentManager.beginTransaction()
                .remove(it)
                .commit()
        }
    }

    private fun replaceInMeetingFragment(showIscFragment: Boolean) {
        iscGalleryFragment?.onDestroy()
        inMeetingFragment?.onDestroy()
        remoteLearningFragment?.onDestroy()
        remoteAssistFragment?.onDestroy()
        augmentedFacesFragment?.onDestroy()
        iscGalleryFragment = IscGalleryFragment()
        inMeetingFragment = InMeetingFragment()
        val inMeetingFragment = if (showIscFragment) iscGalleryFragment!! else inMeetingFragment!!
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.inMeetingFragmentContainer,
                inMeetingFragment
            )
            .commit()
    }

    private fun showJoiningInProgressView() {
        Timber.tag(TAG).i("showJoiningInProgressView")
        binding.tvAppVersion.gone()
        binding.joinInfo.joinInfo.gone()
        binding.tvProgressMsg.visible()
        binding.tvProgressMsg.text = getString(R.string.connectingState)
        binding.ivClose.visible()
        binding.ivMenuOption.visible()
        binding.controlPanelContainer.setBackgroundResource(R.drawable.meeting_controls_panel_bg)
        binding.ivUploadLogs.gone()
    }

    private fun showInMeetingView() {
        Timber.tag(TAG).i("showInMeetingView")
        if (isInWaitingRoom) {
            isInWaitingRoom = false
            showWaitingRoomUI()
        }
        binding.tvAppVersion.gone()
        binding.tvProgressMsg.gone()
        binding.joinInfo.joinInfo.gone()
        binding.inMeetingFragmentContainer.visible()
        binding.ivClose.visible()
        binding.ivMenuOption.visible()
        binding.ivRoster.visible()
        binding.controlPanelContainer.setBackgroundResource(R.drawable.meeting_controls_panel_bg)
        binding.ivUploadLogs.gone()
        binding.tvClosedCaption.visible()
    }

    private fun showOutOfMeetingView() {
        Timber.tag(TAG).i("showOutOfMeetingView")
        binding.tvAppVersion.visible()
        binding.inMeetingFragmentContainer.gone()
        binding.tvProgressMsg.gone()
        binding.ivClose.gone()
        binding.ivMenuOption.gone()
        binding.joinInfo.joinInfo.visible()
        binding.ivRoster.gone()
        binding.ivScreenShare.gone()
        binding.controlPanelContainer.setBackgroundResource(0)
        binding.ivUploadLogs.visible()
        binding.tvClosedCaption.gone()
        binding.tvClosedCaption.text = null
        if (bottomSheetFragment?.isAdded == true) with(bottomSheetFragment) { this?.dismiss() }
        if (participantListFragment?.isAdded == true && !binding.joinInfo.cbShowIsc.isChecked) {
            supportFragmentManager.beginTransaction().remove(participantListFragment!!).commit()
            val fragment = supportFragmentManager.findFragmentByTag("ParticipantListFragment")
            if (fragment != null) {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }
        }
        if (participantListFragment?.isAdded == true && binding.joinInfo.cbShowIsc.isChecked) {
            supportFragmentManager.beginTransaction().remove(iscParticipantListFragment!!).commit()
            val fragment = supportFragmentManager.findFragmentByTag("IscParticipantListFragment")
            if (fragment != null) {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }
        }
        closeCameraSettings()
        if (audioDeviceDialog?.isShowing == true) audioDeviceDialog?.dismiss()
        if (videoDeviceDialog?.isShowing == true) videoDeviceDialog?.dismiss()
        if (videoLayoutDialog?.isShowing == true) videoLayoutDialog?.dismiss()
        if (iscUseCasesDialog?.isShowing == true) iscUseCasesDialog?.dismiss()
    }

    private fun hideProgress() {
        binding.tvProgressMsg.gone()
        binding.tvProgressMsg.text = ""
    }

    private fun toggleAudioMuteUnMuteView(isMuted: Boolean) {
        val resID = if (isMuted) R.drawable.ic_mic_off_black_24dp else R.drawable.ic_mic_black_24dp
        binding.selfView.ivMic.setImageResource(resID)
    }

    private fun toggleVideoMuteUnMuteView(isMuted: Boolean) {
        val resID = if (isMuted) R.drawable.ic_videocam_off_black_24dp
        else R.drawable.ic_videocam_black_24dp
        binding.selfView.ivVideo.setImageResource(resID)
        if (isMuted) {
            closeCameraSettings()
            binding.selfView.ivCameraSettings.gone()
        } else {
            binding.selfView.ivCameraSettings.visible()
        }
    }

    private fun areInMeetingServicesAvailable(): Boolean {
        return meetingService.meetingState.value is
                MeetingService.MeetingState.Connecting ||
                meetingService.meetingState.value is MeetingService.MeetingState.Connected ||
                meetingService.meetingState.value is MeetingService.MeetingState.Reconnecting
    }

    private fun closeCameraSettings() {
        zoomScaleFactor = 1
        if (cameraSettingsDialog?.isShowing == true) {
            cameraSettingsDialog?.dismiss()
        }
    }

    private fun configurePortraitView() {
        val params = binding.selfView.selfView.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = R.id.parent_layout
        params.topToTop = R.id.parent_layout
        params.endToEnd = R.id.parent_layout
        params.dimensionRatio = resources.getString(R.string.self_view_ratio)
        binding.selfView.selfView.requestLayout()
    }

    private fun configureLandscapeView() {
        val params = binding.selfView.selfView.layoutParams as ConstraintLayout.LayoutParams
        params.startToStart = ConstraintLayout.LayoutParams.UNSET
        params.topToTop = R.id.parent_layout
        params.endToEnd = R.id.parent_layout
        params.dimensionRatio = resources.getString(R.string.self_view_ratio)
        binding.selfView.selfView.requestLayout()
    }

    private fun handleMeetingState(state: MeetingService.MeetingState) {
        when (state) {
            is MeetingService.MeetingState.Connected -> {
                // add this flag to avoid screen shots.
                // This also allows protection of screen during screen casts from 3rd party apps.
                if (binding.joinInfo.cbShowIsc.isChecked) {
                    meetingService.setVideoLayout(MeetingService.VideoLayout.Custom)
                }

                if (!isCallInProgress) {
                    OnGoingMeetingService.startService(this)
                    activateInMeetingSubscriptions()
                }
                showInMeetingFragment(binding.joinInfo.cbShowIsc.isChecked)
                hideProgress()
                if (isReconnecting) {
                    isReconnecting = false
                    showToastMessage(getString(R.string.reconnected))
                }
            }
            is MeetingService.MeetingState.Idle -> {
                endMeeting()
                removeInMeetingFragment()
                isInWaitingRoom = false
                currentPinnedParticipant = null
                showWaitingRoomUI()
//                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            is MeetingService.MeetingState.WaitingRoom -> {
                Timber.tag(TAG).i("Moving to WR")
                removeInMeetingFragment()
                inMeetingDisposable.clear()
                showOutOfMeetingView()
                isInWaitingRoom = true
                isCallInProgress = false
                showWaitingRoomUI()
            }
            is MeetingService.MeetingState.Connecting -> {
                isInWaitingRoom = false
                showWaitingRoomUI()
                showInMeetingView()
                binding.joinInfo.joinInfo.visibility = View.GONE
                binding.tvProgressMsg.visibility = View.VISIBLE
                binding.tvProgressMsg.text = "Connecting..."
            }
            is MeetingService.MeetingState.Reconnecting -> {
                isReconnecting = true
                showToastMessage(getString(R.string.reconnecting))
            }
            else -> Log.i(TAG, "Unknown meeting state: $state")
        }
    }

    private val mIOptionMenuCallback = object : MenuFragment.IMenuCallback {
        override fun showVideoLayoutView(videoLayoutName: String) {
            updateCurrentVideoLayoutForAlertDialog(videoLayoutName)
            showVideoLayoutDialog()
        }

        override fun showAudioDeviceView() {
            showAudioDeviceDialog()
        }

        override fun showVideoDeviceView() {
            showVideoDeviceDialog()
        }

        override fun showIscUseCases() {
            if (meetingService.videoLayout.value != MeetingService.VideoLayout.Custom) {
                meetingService.setVideoLayout(MeetingService.VideoLayout.Custom)
            }
            showIscUseCasesDialog()
        }

        override fun showIscStreamStyleView() {
            if (binding.joinInfo.cbShowIsc.isChecked && supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is IscGalleryFragment) {
                showIscStreamStyleDialog()
            } else {
                showToastMessage(resources.getString(R.string.no_stream_styles))
            }
        }

        override fun handleClosedCaptionSwitchEvent(isChecked: Boolean) {
            if (isChecked) {
                meetingService.closedCaptioningService.startClosedCaptioning()
                binding.tvClosedCaption.visible()
            } else {
                meetingService.closedCaptioningService.stopClosedCaptioning()
                binding.tvClosedCaption.gone()
            }
        }

        override fun handleHDCaptureSwitchEvent(isChecked: Boolean) {
            Timber.tag(TAG).i("Enabling hdCapture: $isChecked")
            videoDeviceService.set720pVideoCaptureEnabled(isChecked)
        }

        override fun handleHDReceiveSwitchEvent(isChecked: Boolean) {
            Timber.tag(TAG).i("Enabling HD receive: $isChecked")
            videoDeviceService.set720pVideoReceiveEnabled(isChecked)
        }

        override fun showWaitingRoom() {
            showWaitingRoomDialog()
        }

        override fun setWaitingRoomEnabled(enabled: Boolean) {
            meetingService.moderatorWaitingRoomService.setWaitingRoomEnabled(enabled)
        }

        override fun setCustomVideoSource(isCustom: Boolean) {
            isVideoSourceCustom = isCustom
            bottomSheetFragment?.updateCustomVideoSwitchState(isVideoSourceCustom)
            if (isCustom) {
                augmentedFacesFragment = null
                augmentedFacesFragment = AugmentedFacesFragment.newInstance(this@MainActivity)

                binding.selfView.root.visibility = View.GONE
                binding.customVideoFragmentContainer.visibility = View.VISIBLE

                supportFragmentManager.beginTransaction()
                    .replace(binding.customVideoFragmentContainer.id, augmentedFacesFragment!!)
                    .commit()
            } else {
                augmentedFacesFragment?.let {
                    supportFragmentManager.beginTransaction()
                        .remove(it)
                        .commit()
                }

                binding.customVideoFragmentContainer.visibility = View.GONE
                binding.selfView.root.visibility = View.VISIBLE
            }
        }
    }

    private fun videoLayoutOptionList(): List<String> {
        return listOf(
            getString(R.string.people_view),
            getString(R.string.speaker_view),
            getString(R.string.gallery_view),
            getString(R.string.custom_view)
        )
    }

    private fun showVideoLayoutDialog() {
        videoLayoutDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.video_layouts))
            .setAdapter(videoLayoutAdapter) { _, which -> selectVideoLayout(which) }.create()
        videoLayoutDialog?.show()
    }

    private fun showAudioDeviceDialog() {
        audioDeviceDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.audio_devices))
            .setAdapter(audioDeviceAdapter) { _, which -> selectAudioDevice(which) }.create()
        audioDeviceDialog?.show()
    }

    private fun showVideoDeviceDialog() {
        videoDeviceDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.video_devices))
            .setAdapter(videoDeviceAdapter) { _, position -> selectVideoDevice(position) }
            .create()
        videoDeviceDialog?.show()
    }

    private fun showIscStreamStyleDialog() {
        videoDeviceDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.stream_style))
            .setAdapter(iscStreamStyleAdapter) { _, position -> selectIscStreamStyle(position) }
            .create()
        videoDeviceDialog?.show()
    }

    private fun showIscUseCasesDialog() {
        iscUseCasesDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.isc_use_case))
            .setAdapter(iscUseCasesAdapter) { _, position -> selectIscUseCase(position) }
            .create()
        iscUseCasesDialog?.show()
    }

    private fun showWaitingRoomDialog() {
        val waitingRoomParticipants =
            meetingService.moderatorWaitingRoomService.waitingRoomParticipants
        if (!waitingRoomParticipants.value.isNullOrEmpty()) {
            waitingRoomParticipants.value?.let {
                WaitingRoomDialog.newInstance(it, meetingService)
                    .show(supportFragmentManager, WaitingRoomDialog.TAG)
            }
        } else {
            showToastMessage(getString(R.string.no_wr_participants))
        }
    }

    private fun updateCurrentVideoDeviceForAlertDialog(videoDevice: VideoDevice) {
        val videoDevices = videoDeviceService.videoDevices.value
        videoDevices?.let {
            videoDeviceAdapter?.updateSelectedPosition(it.indexOf(videoDevice))
        }
    }

    private fun updateCurrentAudioDeviceForAlertDialog(currentAudioDevice: AudioDevice) {
        val audioDevices = meetingService.audioDeviceService.audioDevices.value
        audioDevices?.let {
            audioDeviceAdapter!!.updateSelectedPosition(it.indexOf(currentAudioDevice))
        }
    }

    private fun updateCurrentVideoLayoutForAlertDialog(videoLayoutName: String) {
        videoLayoutAdapter?.updateSelectedPosition(
            videoLayoutOptionList().indexOf(videoLayoutName)
        )
    }

    private fun getVideoLayoutAdapter() =
        MenuItemAdapter(
            this, android.R.layout.select_dialog_singlechoice,
            videoLayoutOptionList()
        ) { view, item, _ ->
            (view as CheckedTextView).text = item
        }

    private fun getVideoDeviceAdapter(videoDevices: List<VideoDevice>) =
        MenuItemAdapter(
            this,
            android.R.layout.select_dialog_singlechoice, videoDevices
        ) { view, item, _ ->
            (view as CheckedTextView).text = item.name
        }

    private fun getAudioDeviceAdapter(audioDevices: List<AudioDevice>) =
        MenuItemAdapter(
            this,
            android.R.layout.select_dialog_singlechoice, audioDevices
        ) { view, item, _ ->
            (view as CheckedTextView).text = getAudioDeviceName(item)
        }

    private fun getIscAdapter(styles: Array<out String>) =
        MenuItemAdapter(
            this,
            android.R.layout.select_dialog_singlechoice, styles.asList()
        ) { view, item, _ ->
            (view as CheckedTextView).text = item
        }

    private fun setVideoLayout(videoLayoutName: String?) {
        val videoLayout = when (videoLayoutName) {
            getString(R.string.people_view) ->
                MeetingService.VideoLayout.People

            getString(R.string.gallery_view) ->
                MeetingService.VideoLayout.Gallery

            getString(R.string.speaker_view) ->
                MeetingService.VideoLayout.Speaker

            getString(R.string.custom_view) ->
                MeetingService.VideoLayout.Custom

            else -> throw IllegalArgumentException("Invalid video layout")
        }

        if (binding.joinInfo.cbShowIsc.isChecked && (supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is IscGalleryFragment ||
                    supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is RemoteLearningFragment) ||
            supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is RemoteAssistFragment
        ) {
            if (videoLayout != MeetingService.VideoLayout.Custom) {
                replaceInMeetingFragment(false)
            }
        } else if (binding.joinInfo.cbShowIsc.isChecked && videoLayout is MeetingService.VideoLayout.Custom) {
            replaceInMeetingFragment(true)
        } else if (!binding.joinInfo.cbShowIsc.isChecked && videoLayout is MeetingService.VideoLayout.Custom) {
            showToastMessage(getString(R.string.no_individual_streams))
            return
        }

        bottomSheetFragment?.updateVideoLayout(videoLayoutName)
        meetingService.setVideoLayout(videoLayout)
        if (videoLayoutName != getString(R.string.custom_view) && binding.selfView.root.visibility == View.GONE) {
            binding.selfView.root.visibility = View.VISIBLE
        }
    }

    private fun selectAudioDevice(position: Int) {
        audioDeviceAdapter?.updateSelectedPosition(position)
        audioDeviceAdapter?.getItem(position)?.let {
            bottomSheetFragment?.updateAudioDevice(getAudioDeviceName(it))
            meetingService.audioDeviceService.selectAudioDevice(it)
        }
    }

    private fun selectVideoDevice(position: Int) {
        videoDeviceAdapter?.updateSelectedPosition(position)
        videoDeviceAdapter?.getItem(position)?.let {
            bottomSheetFragment?.updateVideoDevice(it.name)
            closeCameraSettings()
            videoDeviceService.selectVideoDevice(it)
        }
    }

    private fun selectIscStreamStyle(position: Int) {
        iscStreamStyleAdapter?.updateSelectedPosition(position)
        val currentStyle = when (position) {
            0 -> VideoStreamStyle.FIT_TO_VIEW
            1 -> VideoStreamStyle.SCALE_AND_CROP
            else -> VideoStreamStyle.FIT_TO_VIEW
        }

        meetingService.videoStreamService.setVideoStreamStyle(currentStyle)

        if (supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is IscGalleryFragment) {
            iscGalleryFragment?.let {
                it.setVideoStreamStyle()
            }
        }
        bottomSheetFragment?.updateVideoStreamStyle(resources.getStringArray(R.array.stream_styles)[position])
    }

    private fun selectIscUseCase(position: Int) {
        iscUseCasesAdapter?.updateSelectedPosition(position)
        meetingService.videoStreamService.setVideoStreamStyle(VideoStreamStyle.FIT_TO_VIEW)
        iscStreamStyleAdapter?.updateSelectedPosition(0)
        bottomSheetFragment?.updateVideoStreamStyle(resources.getStringArray(R.array.stream_styles)[0])
        when (position) {
            0 -> {
                iscGalleryFragment?.onDestroy()
                inMeetingFragment?.onDestroy()
                remoteLearningFragment?.onDestroy()
                remoteAssistFragment?.onDestroy()
                iscGalleryFragment = IscGalleryFragment()
                inMeetingFragment = InMeetingFragment()
                remoteLearningFragment = RemoteLearningFragment()
                binding.selfView.root.visibility = View.GONE
                val inMeetingFragment = remoteLearningFragment!!
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.inMeetingFragmentContainer,
                        inMeetingFragment
                    )
                    .commit()
            }
            1 -> {
                iscGalleryFragment?.onDestroy()
                inMeetingFragment?.onDestroy()
                remoteLearningFragment?.onDestroy()
                remoteAssistFragment?.onDestroy()
                iscGalleryFragment = IscGalleryFragment()
                inMeetingFragment = InMeetingFragment()
                remoteAssistFragment = RemoteAssistFragment()
                binding.selfView.root.visibility = View.GONE
                val inMeetingFragment = remoteAssistFragment!!
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.inMeetingFragmentContainer,
                        inMeetingFragment
                    )
                    .commit()
            }
        }
        bottomSheetFragment?.updateIscUseCase(resources.getStringArray(R.array.isc_use_cases)[position])
    }

    private fun selectVideoLayout(position: Int) {
        videoLayoutAdapter?.updateSelectedPosition(position)
        setVideoLayout(videoLayoutAdapter?.getItem(position))
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun showCameraSettingsDialog() {
        zoomSeekBar = SeekBar(this).apply {
            max = 10
            min = 1
            progress = zoomScaleFactor
        }
        zoomSeekBar?.setOnSeekBarChangeListener(zoomSliderChangeListener)
        cameraSettingsDialog = AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.camera_setting_title))
            setView(zoomSeekBar)
        }.create()
        cameraSettingsDialog?.show()
    }

    /**
     * It calculates the new crop region by finding out the delta between active camera region's
     * x and y coordinates and divide by zoom scale factor to get updated camera's region.
     * @param cameraActiveRegion
     * @param zoomFactor
     * @return Rect coordinates of crop region to be zoomed.
     */
    private fun getCropRegionForZoom(
        cameraActiveRegion: Rect,
        zoomFactor: Int
    ): Rect {
        val xCenter = cameraActiveRegion.width() / 2
        val yCenter = cameraActiveRegion.height() / 2
        val xDelta = (0.5f * cameraActiveRegion.width() / zoomFactor).toInt()
        val yDelta = (0.5f * cameraActiveRegion.height() / zoomFactor).toInt()
        return Rect(
            xCenter - xDelta, yCenter - yDelta, xCenter + xDelta,
            yCenter + yDelta
        )
    }

    private fun getVideoLayoutDisplayName(videoLayout: MeetingService.VideoLayout): String {
        return when (videoLayout) {
            MeetingService.VideoLayout.Speaker -> {
                getString(R.string.speaker_view)
            }
            MeetingService.VideoLayout.Gallery -> {
                getString(R.string.gallery_view)
            }
            MeetingService.VideoLayout.People -> {
                getString(R.string.people_view)
            }
            MeetingService.VideoLayout.Custom -> {
                getString(R.string.custom_view)
            }
        }
    }

    private val zoomSliderChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            zoomScaleFactor = progress
            val activeRegion = characteristics
                .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            val cropRegion = activeRegion?.let { getCropRegionForZoom(it, progress) }
            videoDeviceService.setRepeatingCaptureRequest(
                CaptureRequest.SCALER_CROP_REGION, cropRegion
            )
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    }


    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val characteristics: CameraCharacteristics by lazy {
        val cameraId: String = videoDeviceService.currentVideoDevice.value!!.id
        cameraManager.getCameraCharacteristics(cameraId)
    }

    private val userName: String by lazy {
        when {
            binding.joinInfo.etName.text.toString().isEmpty() -> "Guest"
            else -> binding.joinInfo.etName.text.toString()
        }
    }

    private fun showWaitingRoomUI() {
        if (isInWaitingRoom) {
            binding.joinInfo.joinInfo.visibility = View.GONE
            binding.waitingRoomLayout.clWaitingRoom.visibility = View.VISIBLE

            meetingService.meetingInformation.value?.meetingTitle?.let {
                binding.waitingRoomLayout.tvWaitingRoom.text = it
            }
        } else {
            binding.joinInfo.joinInfo.visibility = View.VISIBLE
            binding.waitingRoomLayout.clWaitingRoom.visibility = View.GONE
        }
        hideProgress()
    }

    private fun isVideoStreamRoster(): Boolean {
        return supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is IscGalleryFragment ||
                supportFragmentManager.findFragmentById(R.id.inMeetingFragmentContainer) is RemoteAssistFragment
    }

    companion object {
        const val TAG = "MainActivity"
    }
}