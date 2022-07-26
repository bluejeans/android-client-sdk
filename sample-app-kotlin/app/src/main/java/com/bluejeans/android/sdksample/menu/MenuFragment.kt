/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bjnclientcore.media.sequin.Sequin
import com.bjnclientcore.ui.util.extensions.gone
import com.bjnclientcore.ui.util.extensions.visible
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.android.sdksample.databinding.FragmentOptionMenuDialogBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class MenuFragment(
    private val menuCallBack: IMenuCallback,
    private val isWaitingRoomEnabled: Boolean
    ) : BottomSheetDialogFragment() {

    private val TAG = "MenuFragment"

    private var videoLayout = ""
    private var currentAudioDevice = ""
    private var currentVideoDevice = ""
    private var closedCaptionState = false
    private var hdCaptureState = false
    private var menuFragmentBinding: FragmentOptionMenuDialogBinding? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private lateinit var disposable: Disposable

    interface IMenuCallback {
        fun showVideoLayoutView(videoLayoutName: String)
        fun showAudioDeviceView()
        fun showVideoDeviceView()
        fun handleClosedCaptionSwitchEvent(isChecked: Boolean)
        fun handleHDCaptureSwitchEvent(isChecked: Boolean)
        fun handleHDReceiveSwitchEvent(isChecked: Boolean)
        fun showWaitingRoom()
        fun setWaitingRoomEnabled(enabled: Boolean)
    }

    override fun onStart() {
        super.onStart()
        bottomSheetBehavior = BottomSheetBehavior.from(requireView().parent as View)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        menuFragmentBinding = FragmentOptionMenuDialogBinding.inflate(inflater, container, false)

        if (isWaitingRoomEnabled) {
            menuFragmentBinding!!.swWaitingRoom.isChecked = isWaitingRoomEnabled
        }

        disposable = SampleApplication.blueJeansSDK.meetingService.moderatorWaitingRoomService.isWaitingRoomEnabled
            .rxObservable.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    it.value?.let { isChecked ->
                        menuFragmentBinding!!.swWaitingRoom.isChecked = isChecked
                    }
                },
                {
                    Timber.tag(TAG).e(it.message)
                })

        return menuFragmentBinding!!.root
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    fun updateVideoLayout(videoLayout: String?) {
        if (videoLayout != null) {
            this.videoLayout = videoLayout
        }
        updateView()
    }

    fun updateAudioDevice(currentAudioDevice: String?) {
        this.currentAudioDevice = currentAudioDevice!!
        updateView()
    }

    fun updateVideoDevice(currentVideoDevice: String?) {
        this.currentVideoDevice = currentVideoDevice!!
        updateView()
    }

    fun updateClosedCaptionSwitchState(isClosedCaptionActive: Boolean) {
        closedCaptionState = isClosedCaptionActive
    }

    fun updateHDCaptureState(captureHD: Boolean) {
        hdCaptureState = captureHD
        menuFragmentBinding?.swHDCapture?.isChecked = hdCaptureState
    }

    private fun initViews() {
        menuFragmentBinding?.mbVideoLayout?.setOnClickListener {
            menuCallBack.showVideoLayoutView(menuFragmentBinding!!.mbVideoLayout.text as String)
            dismiss()
        }
        menuFragmentBinding?.mbAudioDevice?.setOnClickListener {
            menuCallBack.showAudioDeviceView()
            dismiss()
        }

        menuFragmentBinding?.mbVideoDevice?.setOnClickListener {
            menuCallBack.showVideoDeviceView()
            dismiss()
        }

        val closedCaptionFeatureObservable =
            SampleApplication.blueJeansSDK.meetingService.closedCaptioningService.isClosedCaptioningAvailable
        if (closedCaptionFeatureObservable.value == true) {
            menuFragmentBinding?.swClosedCaption?.isChecked = closedCaptionState
            menuFragmentBinding?.swClosedCaption?.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    menuCallBack.handleClosedCaptionSwitchEvent(isChecked)
                    closedCaptionState = isChecked
                    dismiss()
                }
            }
            menuFragmentBinding?.swClosedCaption?.visible()
        } else {
            menuFragmentBinding?.swClosedCaption?.gone()
        }


        hdCaptureState = SampleApplication.blueJeansSDK.videoDeviceService.is720pVideoCaptureEnabled.value
        menuFragmentBinding?.swHDCapture?.isChecked = hdCaptureState
        menuFragmentBinding?.swHDCapture?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                menuCallBack.handleHDCaptureSwitchEvent(isChecked)
            }
        }

        menuFragmentBinding?.swHDReceive?.isChecked = SampleApplication.blueJeansSDK.videoDeviceService.is720pVideoReceiveEnabled.value == true
        menuFragmentBinding?.swHDReceive?.setOnCheckedChangeListener { buttonView, isChecked ->
            menuCallBack.handleHDReceiveSwitchEvent(isChecked)
        }

        if (SampleApplication.blueJeansSDK.meetingService.moderatorControlsService.isModeratorControlsAvailable.value == true) {
            menuFragmentBinding?.llWaitingRoom?.visibility = View.VISIBLE

            if (SampleApplication.blueJeansSDK.meetingService.moderatorWaitingRoomService.isWaitingRoomCapable.value == true) {
                menuFragmentBinding?.btnShowWaitingRoom?.setOnClickListener {
                    menuCallBack.showWaitingRoom()
                    dismiss()
                }

                menuFragmentBinding?.swWaitingRoom?.setOnCheckedChangeListener { compoundButton, b ->
                    menuCallBack.setWaitingRoomEnabled(b)
                }
            } else if (SampleApplication.blueJeansSDK.meetingService.moderatorWaitingRoomService.isWaitingRoomCapable.value == false) {
                menuFragmentBinding?.btnShowWaitingRoom?.isEnabled = false
                menuFragmentBinding?.swWaitingRoom?.isEnabled = false
            }
        }

        updateView()
    }

    private fun updateView() {
        menuFragmentBinding?.let {
            it.mbVideoLayout.text = videoLayout
            it.mbAudioDevice.text = currentAudioDevice
            it.mbVideoDevice.text = currentVideoDevice
        }
    }

    override fun onResume() {
        super.onResume()
        menuFragmentBinding?.swClosedCaption?.isChecked = closedCaptionState
        menuFragmentBinding?.swHDCapture?.isChecked = hdCaptureState
    }
}