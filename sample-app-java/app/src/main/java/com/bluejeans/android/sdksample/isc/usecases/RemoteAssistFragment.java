package com.bluejeans.android.sdksample.isc.usecases;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bjnclientcore.media.individualstream.StreamPriority;
import com.bjnclientcore.media.individualstream.StreamQuality;
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration;
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult;
import com.bluejeans.android.sdksample.databinding.FragmentRemoteAssistBinding;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


public class RemoteAssistFragment extends Fragment {
    private static final String TAG = "RemoteAssistFragment";

    private FragmentRemoteAssistBinding binding = null;

    private VideoStreamConfiguration participantConfiguration;

    private RemoteAssistViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentRemoteAssistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RemoteAssistViewModel.class);

        viewModel.getParticipantUIObservable().observe(getViewLifecycleOwner(), participantUI -> {

            if (participantUI == null) {
                if (participantConfiguration != null) {
                    viewModel.detachParticipantFromView(participantConfiguration.getParticipantGuid());
                    binding.tvParticipantName.setText("");
                    binding.remoteAssistTextureView.setVisibility(View.GONE);
                }

                return;
            }

            VideoStreamConfiguration videoStreamConfiguration = new VideoStreamConfiguration(
                    participantUI.getSeamGuid(),
                    StreamQuality.R720p_30fps,
                    StreamPriority.High
            );

            List<VideoStreamConfiguration> list = new ArrayList<>();
            list.add(videoStreamConfiguration);

            VideoStreamConfigurationResult result = viewModel.setVideoConfiguration(list);
            if (result instanceof VideoStreamConfigurationResult.Success) {
                if (participantConfiguration != null) {
                    viewModel.detachParticipantFromView(participantConfiguration.getParticipantGuid());
                }
                Timber.tag(TAG).i("VideoStream successfull");
                participantConfiguration = videoStreamConfiguration;
                binding.remoteAssistTextureView.setVisibility(View.VISIBLE);
                viewModel.attachParticipantToView(
                        videoStreamConfiguration.getParticipantGuid(),
                        binding.remoteAssistTextureView
                );
                binding.tvParticipantName.setText(participantUI.getName());
                setTextureViewConstraints(participantUI.getVideoWidth(), participantUI.getVideoHeight(), binding.remoteAssistTextureView);
            } else {
                Log.e(TAG, "Stream failure " + result);
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (participantConfiguration != null) {
            viewModel.detachParticipantFromView(participantConfiguration.getParticipantGuid());
        }
    }

    private void setTextureViewConstraints(int videoWidth, int videoHeight, TextureView textureView) {
        if (videoHeight > 0 && videoWidth > 0) {
            ConstraintSet set = new ConstraintSet();
            set.clone(binding.getRoot());
            set.connect(textureView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            set.connect(textureView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            set.connect(textureView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            set.connect(textureView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

            float ratio = (float) videoWidth / videoHeight;
            Log.i(TAG, "Ratio: " + ratio);
            set.setDimensionRatio(textureView.getId(), String.valueOf(ratio));

            set.applyTo(binding.getRoot());
        }
    }
}