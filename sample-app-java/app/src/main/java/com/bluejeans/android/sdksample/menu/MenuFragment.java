/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample.menu;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import com.bluejeans.android.sdksample.R;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.bluejeanssdk.meeting.MeetingService;
import com.bluejeans.rxextensions.ObservableValueWithOptional;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

public class MenuFragment extends BottomSheetDialogFragment {
    private static final String TAG = "MenuFragment";

    private IMenuCallback mIMenuCallback;
    private boolean mIsWaitingRoomEnabled;
    private MaterialButton mMbVideoLayout, mMbAudioDevice, mMbVideoDevice, mMbIscStreamStyle, mMbIscUseCases;
    private Button mViewWaitingRoom;
    private String mVideoLayout = "";
    private String mCurrentAudioDevice = "";
    private String mCurrentVideoDevice = "";
    private String mCurrentStreamStyle = "";
    private boolean mClosedCaptionState = false;
    private boolean mHDCaptureState = false;
    private boolean mHDReceiveState = false;
    private SwitchCompat mSwitchClosedCaption, mSwitchWaitingRoom, mSwitchHDCapture, mSwitch720Receive;
    private TextView mTvIscUseCase;
    private LinearLayout mWaitingRoomLayout;

    private Disposable mWaitingRoomEnablementDisposable;

    public boolean isIscEnabled = false;

    private Disposable disposable;

    public interface IMenuCallback {
        void showVideoLayoutView(String videoLayoutName);

        void showAudioDeviceView();

        void showVideoDeviceView();

        void showIscStreamStyleView();

        void showVideoStreamUseCases();

        void handleClosedCaptionSwitchEvent(Boolean enabled);

        void handleHDCaptureSwitchEvent(Boolean isChecked);

        void handleHDReceiveSwitchEvent(Boolean isChecked);

        void showWaitingRoom();

        void setWaitingRoomEnabled(boolean enabled);
    }

    public MenuFragment(IMenuCallback iMenuCallback, boolean isWaitingRoomEnabled) {
        mIMenuCallback = iMenuCallback;
        mIsWaitingRoomEnabled = isWaitingRoomEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_option_menu_dialog, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) requireView().getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);

        disposable = SampleApplication.getBlueJeansSDK().getMeetingService()
                .getVideoLayout()
                .getRxObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(videoLayoutOptional -> {
                    if (videoLayoutOptional.getValue() != null && videoLayoutOptional.getValue() instanceof MeetingService.VideoLayout.Custom) {
                        mTvIscUseCase.setVisibility(View.VISIBLE);
                        mMbIscUseCases.setVisibility(View.VISIBLE);
                        mMbIscStreamStyle.setEnabled(true);

                        mMbIscStreamStyle.setOnClickListener(view1 -> {
                            mIMenuCallback.showIscStreamStyleView();
                            dismiss();
                        });

                        mMbIscUseCases.setOnClickListener(view1 -> {
                            mIMenuCallback.showVideoStreamUseCases();
                            dismiss();
                        });
                    } else {
                        mTvIscUseCase.setVisibility(View.GONE);
                        mMbIscUseCases.setVisibility(View.GONE);
                        mMbIscStreamStyle.setEnabled(false);
                    }
                }, err -> {
                    Log.e(TAG, "Error: " + err.getLocalizedMessage());
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWaitingRoomEnablementDisposable != null && !mWaitingRoomEnablementDisposable.isDisposed())
            mWaitingRoomEnablementDisposable.dispose();
    }

    public void updateVideoLayout(String videoLayout) {
        this.mVideoLayout = videoLayout;
        updateView();
    }

    public void updateAudioDevice(String currentAudioDevice) {
        this.mCurrentAudioDevice = currentAudioDevice;
        updateView();
    }

    public void updateVideoDevice(String currentVideoDevice) {
        this.mCurrentVideoDevice = currentVideoDevice;
        updateView();
    }

    public void updateVideoStreamStyle(String currentStreamStyle) {
        this.mCurrentStreamStyle = currentStreamStyle;
        updateView();
    }

    public void updateClosedCaptionSwitchState(boolean isClosedCaptionActive) {
        mClosedCaptionState = isClosedCaptionActive;
    }

    public void updateHDCaptureState(boolean captureHD) {
        mHDCaptureState = captureHD;
        mSwitchHDCapture.setChecked(mHDCaptureState);
    }

    private void initViews(View view) {
        mMbVideoLayout = view.findViewById(R.id.mbVideoLayout);
        mMbAudioDevice = view.findViewById(R.id.mbAudioDevice);
        mMbVideoDevice = view.findViewById(R.id.mbVideoDevice);
        mSwitchClosedCaption = view.findViewById(R.id.swClosedCaption);
        mSwitchHDCapture = view.findViewById(R.id.swHDCapture);
        mSwitch720Receive = view.findViewById(R.id.swHDReceive);
        mMbIscStreamStyle = view.findViewById(R.id.mbVideoStreamStyle);
        mMbIscUseCases = view.findViewById(R.id.mbIscUseCases);
        mTvIscUseCase = view.findViewById(R.id.tvIscUseCases);

        if (SampleApplication.getBlueJeansSDK().getBlueJeansClient().getMeetingSession().isModerator()) {
            mWaitingRoomLayout = view.findViewById(R.id.llWaitingRoom);
            mWaitingRoomLayout.setVisibility(View.VISIBLE);

            mViewWaitingRoom = view.findViewById(R.id.btnShowWaitingRoom);
            mSwitchWaitingRoom = view.findViewById(R.id.swWaitingRoom);

            boolean isWaitingRoomCapable = SampleApplication.getBlueJeansSDK().getMeetingService().getModeratorWaitingRoomService().isWaitingRoomCapable().getValue();
            if (isWaitingRoomCapable == true) {

                mViewWaitingRoom.setOnClickListener(view1 -> {
                    mIMenuCallback.showWaitingRoom();
                    dismiss();
                });

                mWaitingRoomEnablementDisposable = SampleApplication.getBlueJeansSDK().getMeetingService().getModeratorWaitingRoomService().isWaitingRoomEnabled()
                        .getRxObservable().observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                isEnabled -> {
                                    if (isEnabled.getValue()) {
                                        mSwitchWaitingRoom.setChecked(true);
                                    } else {
                                        mSwitchWaitingRoom.setChecked(false);
                                    }
                                }, err -> {
                                    Log.e(TAG, "Error occured while getting isWaitingRoomEnabled value");
                                }
                        );

                mSwitchWaitingRoom.setOnCheckedChangeListener((buttonView, isChecked) -> mIMenuCallback.setWaitingRoomEnabled(isChecked));
            } else if (isWaitingRoomCapable == false) {
                mViewWaitingRoom.setEnabled(false);
                mSwitchWaitingRoom.setEnabled(false);
            }
        }

        mMbVideoLayout.setOnClickListener(view1 -> {
            mIMenuCallback.showVideoLayoutView(mMbVideoLayout.getText().toString());
            dismiss();
        });

        mMbAudioDevice.setOnClickListener(view1 -> {
            mIMenuCallback.showAudioDeviceView();
            dismiss();
        });
        mMbVideoDevice.setOnClickListener(view1 -> {
            mIMenuCallback.showVideoDeviceView();
            dismiss();
        });

        if (this.mIsWaitingRoomEnabled) {
            mSwitchWaitingRoom.setChecked(this.mIsWaitingRoomEnabled);
        }

        if (isIscEnabled) {
            mTvIscUseCase.setVisibility(View.VISIBLE);
            mMbIscUseCases.setVisibility(View.VISIBLE);
            mMbIscStreamStyle.setEnabled(true);

            mMbIscStreamStyle.setOnClickListener(view1 -> {
                mIMenuCallback.showIscStreamStyleView();
                dismiss();
            });

            mMbIscUseCases.setOnClickListener(view1 -> {
                mIMenuCallback.showVideoStreamUseCases();
                dismiss();
            });
        } else {
            mTvIscUseCase.setVisibility(View.GONE);
            mMbIscUseCases.setVisibility(View.GONE);
            mMbIscStreamStyle.setEnabled(false);
        }

        ObservableValueWithOptional<Boolean> closedCaptionFeatureObservable = SampleApplication.getBlueJeansSDK().getMeetingService()
                .getClosedCaptioningService().isClosedCaptioningAvailable();
        if (closedCaptionFeatureObservable.getValue() == Boolean.TRUE) {
            mSwitchClosedCaption.setChecked(mClosedCaptionState);
            mSwitchClosedCaption.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    mIMenuCallback.handleClosedCaptionSwitchEvent(isChecked);
                    mClosedCaptionState = isChecked;
                    dismiss();
                }
            });
            mSwitchClosedCaption.setVisibility(View.VISIBLE);
        } else {
            mSwitchClosedCaption.setVisibility(View.GONE);
        }

        mHDCaptureState = SampleApplication.getBlueJeansSDK().getVideoDeviceService().is720pVideoCaptureEnabled().getValue();
        mSwitchHDCapture.setChecked(mHDCaptureState);
        mSwitchHDCapture.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                mIMenuCallback.handleHDCaptureSwitchEvent(isChecked);
            }
        });

        mHDReceiveState = SampleApplication.getBlueJeansSDK().getVideoDeviceService().is720pVideoReceiveEnabled().getValue();
        mSwitch720Receive.setChecked(mHDReceiveState);
        mSwitch720Receive.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                mIMenuCallback.handleHDReceiveSwitchEvent(isChecked);
            }
        }));
        updateView();
    }

    private void updateView() {
        if (mMbVideoLayout != null) {
            mMbVideoLayout.setText(mVideoLayout);
        }
        if (mMbAudioDevice != null) {
            mMbAudioDevice.setText(mCurrentAudioDevice);
        }
        if (mMbVideoDevice != null) {
            mMbVideoDevice.setText(mCurrentVideoDevice);
        }
        if (mMbIscStreamStyle != null) {
            mMbIscStreamStyle.setText(mCurrentStreamStyle);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchClosedCaption.setChecked(mClosedCaptionState);
        mSwitchHDCapture.setChecked(mHDCaptureState);
    }
}
