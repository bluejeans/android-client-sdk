package com.bluejeans.android.sdksample.isc.usecases;

import android.content.res.Configuration;
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
import com.bluejeans.android.sdksample.databinding.FragmentRemoteLearningBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteLearningFragment extends Fragment {
    private static final String TAG = "RemoteLearningFragment";
    private FragmentRemoteLearningBinding binding = null;

    private RemoteLearningParticipant moderator = null;
    private List<RemoteLearningParticipant> students = new ArrayList<>();
    private List<TextureView> studentTextureViews = new ArrayList<>();

    private RemoteLearningViewModel viewModel;

    public RemoteLearningFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRemoteLearningBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        studentTextureViews.add(0, binding.studentOne.participantTextureView);
        studentTextureViews.add(1, binding.studentTwo.participantTextureView);
        studentTextureViews.add(2, binding.studentThree.participantTextureView);
        studentTextureViews.add(3, binding.studentFour.participantTextureView);
        studentTextureViews.add(4, binding.studentFive.participantTextureView);
        studentTextureViews.add(5, binding.studentSix.participantTextureView);

        viewModel = new ViewModelProvider(this).get(RemoteLearningViewModel.class);
        viewModel.participantsObservable.observe(getViewLifecycleOwner(), studentsList -> {
            for (TextureView v : studentTextureViews) {
                v.setVisibility(View.GONE);
            }
            moderator = null;
            students.clear();
            students.addAll(studentsList);

            List<VideoStreamConfiguration> configurationList = new ArrayList<>();
            students.forEach(p -> {
                if (p.getParticipantId() != null) {
                    if (p.isModerator()) {
                        moderator = p;
                        configurationList.add(
                                new VideoStreamConfiguration(
                                        p.getParticipantId(),
                                        StreamQuality.R720p_30fps,
                                        StreamPriority.High
                                )
                        );
                    } else {
                        configurationList.add(
                                new VideoStreamConfiguration(
                                        p.getParticipantId(),
                                        StreamQuality.R90p_15fps,
                                        StreamPriority.Low
                                )
                        );
                    }
                }
            });

            VideoStreamConfigurationResult result = viewModel.setVideoConfiguration(configurationList);
            if (result instanceof VideoStreamConfigurationResult.Success) {
                updateModeratorUI();
                updateStudentsUI();
            } else {
                Log.e(TAG, "Failed to set stream configuration: " + result);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (moderator != null) {
            viewModel.detachParticipantFromView(moderator.getParticipantId());
        }

        students.forEach(student -> {
            viewModel.detachParticipantFromView(student.getParticipantId());
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            configurePortraitView();
        } else configureLandscapeView();
    }

    private void updateModeratorUI() {
        if (moderator != null) {
            binding.layoutMainStage.tvParticipantName.setText(moderator.getName());

            if (!moderator.isVideo()) {
                Log.i(TAG, "Detaching moderator video");
                viewModel.detachParticipantFromView(moderator.getParticipantId());

                binding.layoutMainStage.participantTextureView.setVisibility(View.GONE);
                binding.layoutMainStage.cnhAudioOnly.setVisibility(View.VISIBLE);
                binding.layoutMainStage.cnhAudioOnly.setText(getInitials(moderator.getName()));
            } else {
                binding.layoutMainStage.participantTextureView.setVisibility(View.VISIBLE);
                binding.layoutMainStage.cnhAudioOnly.setVisibility(View.GONE);
                viewModel.attachParticipantToView(moderator.getParticipantId(), binding.layoutMainStage.participantTextureView);
                setModeratorTextureViewConstraints();
            }
        } else {
            binding.layoutMainStage.tvParticipantName.setText("");
            binding.layoutMainStage.cnhAudioOnly.setVisibility(View.GONE);
        }
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

    private void setModeratorTextureViewConstraints() {
        ConstraintSet set = new ConstraintSet();
        TextureView textureView = binding.layoutMainStage.participantTextureView;
        set.clone(binding.layoutMainStage.getRoot());
        set.connect(textureView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
        set.connect(textureView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        set.connect(textureView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        set.connect(textureView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

        if (moderator != null) {
            if (moderator.getWidth() > 0 && moderator.getHeight() > 0) {
                float ratio = ((float) moderator.getWidth() / moderator.getHeight());
                Log.i(TAG, "Ratio: " + ratio);
                set.setDimensionRatio(textureView.getId(), String.valueOf(ratio));
            }
        }

        set.applyTo(binding.layoutMainStage.getRoot());
    }

    private void setStudentTextureViewConstraints(int videoWidth, int videoHeight, TextureView textureView) {
        ConstraintSet set = new ConstraintSet();
        set.clone(binding.layoutMainStage.getRoot());
        set.connect(textureView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
        set.connect(textureView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        set.connect(textureView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        set.connect(textureView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

        if (moderator != null) {
            if (videoWidth > 0 && videoHeight > 0) {
                float ratio = ((float) videoWidth / videoHeight);
                Log.i(TAG, "Ratio: " + ratio);
                set.setDimensionRatio(textureView.getId(), String.valueOf(ratio));
            }
        }

        set.applyTo((ConstraintLayout) textureView.getParent());
    }

    private void updateStudentsUI() {
        List<RemoteLearningParticipant> stds = students.stream().filter(it -> !it.isModerator()).collect(Collectors.toList());
        for (int i = 0; i < stds.size(); i++) {
            viewModel.detachParticipantFromView(stds.get(i).getParticipantId());
            if (stds.get(i).isVideo()) {
                studentTextureViews.get(i).setVisibility(View.VISIBLE);
                attachStudentStream(stds.get(i), studentTextureViews.get(i));
                setStudentTextureViewConstraints(
                        stds.get(i).getWidth(),
                        stds.get(i).getHeight(),
                        studentTextureViews.get(i)
                );
            }
        }
    }

    private void attachStudentStream(RemoteLearningParticipant student, TextureView textureView) {
        viewModel.attachParticipantToView(student.getParticipantId(), textureView);
    }

    private void configureLandscapeView() {
        binding.scrollView.setVisibility(View.GONE);
        setModeratorTextureViewConstraints();
    }

    private void configurePortraitView() {
        binding.scrollView.setVisibility(View.VISIBLE);
        setModeratorTextureViewConstraints();
    }
}