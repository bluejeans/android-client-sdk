/**
 * Copyright (c) 2022 Blue Jeans Networks, Inc. All rights reserved.
 * Created on 19/09/22
 */
package com.bluejeans.android.sdksample.isc;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import com.bjnclientcore.media.individualstream.StreamPriority;
import com.bjnclientcore.media.individualstream.StreamQuality;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.android.sdksample.databinding.IscParticipantViewBinding;
import com.bluejeans.bluejeanssdk.meeting.individualstream.VideoStreamService;
import static com.bluejeans.android.sdksample.isc.IscUtils.getPriorityName;
import static com.bluejeans.android.sdksample.isc.IscUtils.getStreamQualityName;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ParticipantUIView {

    private static final String TAG = "ParticipantUIView";

    private ConstraintSet constraintSet = new ConstraintSet();
    private CompositeDisposable compositeDisposable;

    private IscParticipantViewBinding viewBinding;

    private VideoStreamService videoStreamService =
            SampleApplication.getBlueJeansSDK().getMeetingService().getVideoStreamService();
    private ParticipantUI participantUi;

    public void bind(LinearLayout viewParent) {
        if (viewParent == null) return;
        viewBinding = IscParticipantViewBinding.inflate(
                (LayoutInflater) viewParent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE),
                viewParent,
                false
        );
        constraintSet.clone(viewBinding.getRoot());
    }

    public ConstraintLayout getParticipantView() {
        return viewBinding.getRoot();
    }

    public void associateParticipantUI(ParticipantUI participantUi) {
        this.participantUi = participantUi;
        viewBinding.tvParticipantName.setText(participantUi.getName());
        Log.i(
                TAG,
                "Requested Quality: " + getStreamQualityName(participantUi.getStreamQuality())
        );
        viewBinding.tvParticipantPriority.setText(getPriorityName(participantUi.getStreamPriority()));
        if (participantUi.getVideo()) {
            viewBinding.cnhAudioOnly.setVisibility(View.GONE);
            videoStreamService.attachParticipantToView(
                    participantUi.getSeamGuid(),
                    viewBinding.participantTextureView
            );

            if (participantUi.getVideoWidth() > 0 && participantUi.getVideoHeight() > 0) {
                updateAspectRatio(participantUi.getVideoWidth(), participantUi.getVideoHeight());
            }
            subscribeToResolution(participantUi);
        } else {
            viewBinding.participantTextureView.setVisibility(View.GONE);
            viewBinding.cnhAudioOnly.setVisibility(View.VISIBLE);

            viewBinding.cnhAudioOnly.setText(getInitials(participantUi.getName()));
        }
    }

    public void updateResolutionAndPriority(StreamPriority streamPriority, StreamQuality streamQuality) {
        viewBinding.tvParticipantPriority.setText(getPriorityName(streamPriority));
        Log.i(
                TAG,
                "Requested Quality: " + getStreamQualityName(streamQuality)
        );
    }


    public void unbind() {
        if (participantUi != null) {
            videoStreamService.detachParticipantFromView(
                    participantUi.getSeamGuid()
            );
        }
        viewBinding = null;
        if (compositeDisposable != null) compositeDisposable.dispose();
        compositeDisposable = null;
    }

    private void subscribeToResolution(ParticipantUI participantUi) {
        compositeDisposable = new CompositeDisposable();

        compositeDisposable.add(participantUi.getVideoResolution().getRxObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe(resolution -> {
                    if (resolution.getValue() != null) {
                        Log.i(
                                TAG,
                                "Received Resolution " + resolution.getValue().getWidth() + "x" + resolution.getValue().getHeight() +
                                        " received for participant (" + participantUi.getName() + ") with ID: " + participantUi.getSeamGuid()
                        );
                        viewBinding.tvParticipantResolution.setText(resolution.getValue().getWidth() + "x" + resolution.getValue().getHeight());
                    } else {
                        viewBinding.tvParticipantResolution.setText("0 x 0");
                    }
                }, err -> {
                    Log.e(TAG, "Error in getting resolution: " + err.getMessage());
                }));
    }

    private String getInitials(String name) {

        String[] splitName = name.split(" ");
        if (splitName.length == 0) {
            return "";
        }
        if (splitName.length == 1) {
            return splitName[0].substring(0, 0);
        }
        StringBuilder finalName = new StringBuilder();
        int length = Math.min(splitName.length, 3);
        for (int i = 0; i < length; i++) {
            finalName.append(splitName[i].substring(0, 0));
        }
        return finalName.toString();
    }

    private void updateAspectRatio(int width, int height) {
        float aspectRatio = ((float) width / height);
        Log.i(TAG, "Aspect ratio of film strip participant: " + aspectRatio);
        constraintSet.setDimensionRatio(viewBinding.participantTextureView.getId(), String.valueOf(aspectRatio));
        constraintSet.applyTo(viewBinding.getRoot());
    }
}
