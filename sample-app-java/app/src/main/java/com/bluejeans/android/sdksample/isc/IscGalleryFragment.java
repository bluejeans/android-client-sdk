package com.bluejeans.android.sdksample.isc;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.bluejeans.android.sdksample.R;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.android.sdksample.databinding.FragmentIscGalleryBinding;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.rxextensions.ObservableValueWithOptional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static com.bluejeans.android.sdksample.isc.IscUtils.getPriorityName;
import static com.bluejeans.android.sdksample.isc.IscUtils.getStreamQualityName;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import timber.log.Timber;

public class IscGalleryFragment extends Fragment {

    private static final String TAG = "IscGalleryFragment";

    private IscGalleryViewModel viewModel;

    private FragmentIscGalleryBinding binding;
    private final Map<String, VideoStreamConfiguration> participantsConfigMap = new HashMap<>();
    private ParticipantUI mainStageParticipant = null;
    private String pinnedParticipant = null;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final ParticipantsService participantsService = SampleApplication.getBlueJeansSDK().getMeetingService().getParticipantsService();
    private final List<ParticipantUI> iscParticipants = new ArrayList<>();

    private final ConstraintSet constraintSet = new ConstraintSet();

    private final HashMap<String, ParticipantUIView> participantViewManagerMap = new HashMap<>();

    private CompositeDisposable resolutionDisposable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentIscGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        constraintSet.clone(binding.getRoot());
        setMainStageConstraints();

        viewModel = new ViewModelProvider(this).get(IscGalleryViewModel.class);
        viewModel.getParticipantUIObservable().observe(getViewLifecycleOwner(), participantUi -> {
            if (participantUi.isEmpty()) {
                resetUI();
                return;
            }

            Timber.tag(TAG).i("participantUi: " + participantUi.size());

            List<VideoStreamConfiguration> streamConfiguration = new ArrayList<>();
            participantUi.forEach(p -> {
                if (p.getSeamGuid() != null) {
                    streamConfiguration.add(
                            new VideoStreamConfiguration(
                                    p.getSeamGuid(),
                                    participantsConfigMap.containsValue(p.getSeamGuid()) ?
                                            participantsConfigMap.get(p.getSeamGuid()).getStreamQuality() : StreamQuality.R360p_30fps,
                                    participantsConfigMap.containsValue(p.getSeamGuid()) ?
                                            participantsConfigMap.get(p.getSeamGuid()).getStreamPriority() : StreamPriority.Medium
                            )
                    );
                }
            });


            VideoStreamConfigurationResult result = viewModel.setVideoConfiguration(streamConfiguration);
            if (VideoStreamConfigurationResult.Success.INSTANCE.equals(result)) {
                streamConfiguration.forEach(config -> {
                    if (config.getParticipantGuid() != null) {
                        participantsConfigMap.put(config.getParticipantGuid(), config);
                    }
                });

                iscParticipants.clear();
                iscParticipants.addAll(participantUi);
                updateUI(participantUi);

            } else if (VideoStreamConfigurationResult.Failure.class.equals(result)) {
                Timber.tag(TAG).i("Stream failure ${result.failureReason}");
                streamConfiguration.forEach(config -> {
                    Timber.tag(TAG)
                            .e("Participant ID: ${config.participantGuid} stream config failure");
                });
            }
        });

        subscribeToRosterUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mainStageParticipant != null) {
            viewModel.detachParticipantFromView(mainStageParticipant.getSeamGuid());
        }
        compositeDisposable.dispose();
        resetUI();
        binding = null;
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            configurePortraitView();
        } else configureLandscapeView();
    }

    public void updateStreamConfigurations(String participantGuid,
                                           StreamQuality streamQuality,
                                           StreamPriority streamPriority) {
        participantsConfigMap.put(participantGuid, new VideoStreamConfiguration(participantGuid, streamQuality, streamPriority));
        if (mainStageParticipant != null) {
            if (participantGuid.equals(mainStageParticipant.getSeamGuid())) {
                Log.i(TAG, " Main stage participant resolution updated to " +
                        getStreamQualityName(streamQuality));
                updatePriorityTextView(streamPriority);
                return;
            }
        }

        participantViewManagerMap.forEach((key, value) -> {
                    if (key.equals(participantGuid)) {
                        value.updateResolutionAndPriority(streamPriority, streamQuality);
                        return;
                    }
                }
        );
    }

    public void pinParticipant(String participantId, Boolean isPinned) {
        if (isPinned) {
            pinnedParticipant = participantId;
            if (mainStageParticipant != null) {
                viewModel.detachParticipantFromView(mainStageParticipant.getSeamGuid());
            }
        } else {
            pinnedParticipant = null;
        }

        updateUI(iscParticipants);
    }

    public void setVideoStreamStyle() {
        setMainStageTextureViewConstraints();
        for (int i = 0; i < binding.stripContainer.getChildCount(); i++) {
            ConstraintLayout tileStripParticipant = (ConstraintLayout) binding.stripContainer.getChildAt(i);
            setTextureViewConstraints((TextureView) tileStripParticipant.findViewById(R.id.participantTextureView), tileStripParticipant);
        }
    }

    private void showMainStage() {
        if (mainStageParticipant != null) {
            binding.layoutMainStage.tvParticipantName.setText(mainStageParticipant.getName());
            viewModel.detachParticipantFromView(mainStageParticipant.getSeamGuid());
            if (mainStageParticipant.getVideo()) {
                binding.layoutMainStage.cnhAudioOnly.setVisibility(View.GONE);
                binding.layoutMainStage.participantTextureView.setVisibility(View.VISIBLE);

                updatePriorityTextView(participantsConfigMap.get(mainStageParticipant.getSeamGuid()).getStreamPriority());
                updateResolutionTextView(mainStageParticipant.getVideoResolution());

                viewModel.attachParticipantToView(mainStageParticipant.getSeamGuid(), binding.layoutMainStage.participantTextureView);
                setMainStageConstraints();
                setMainStageTextureViewConstraints();
            } else {
                binding.layoutMainStage.cnhAudioOnly.setVisibility(View.VISIBLE);
                binding.layoutMainStage.participantTextureView.setVisibility(View.GONE);
                binding.layoutMainStage.tvPriority.setText("");
            }
        }
    }

    private void updateUI(List<ParticipantUI> participantUi) {
        if (resolutionDisposable != null) resolutionDisposable.dispose();
        if (mainStageParticipant != null)
            viewModel.detachParticipantFromView(mainStageParticipant.getSeamGuid());
        participantViewManagerMap.forEach(
                (key, value) -> value.unbind());
        participantViewManagerMap.clear();
        if (pinnedParticipant == null) {
            mainStageParticipant = participantUi.get(0);
        } else {
            Optional<ParticipantUI> currentPinnedParticipant = participantUi.stream()
                    .filter(p -> p.getSeamGuid().equals(pinnedParticipant)).findFirst();

            mainStageParticipant = currentPinnedParticipant.orElseGet(() -> participantUi.get(0));
        }
        showMainStage();
        List<ParticipantUI> filmStripParticipants = new ArrayList<>();
        participantUi.forEach(p -> {
            if (!p.getSeamGuid().equals(mainStageParticipant.getSeamGuid())) {
                filmStripParticipants.add(p);
            }
        });

        addParticipantToStrip(filmStripParticipants);
    }

    private void resetUI() {
        participantViewManagerMap.forEach((s, participantUIView) -> participantUIView.unbind());
        participantViewManagerMap.clear();
        if (binding != null) {
            binding.stripContainer.removeAllViews();
        }
        if (resolutionDisposable != null) resolutionDisposable.dispose();
        if (binding != null) {
            binding.layoutMainStage.cnhAudioOnly.setVisibility(View.GONE);
            binding.layoutMainStage.participantTextureView.setVisibility(View.GONE);
            binding.layoutMainStage.tvParticipantName.setText("");
            binding.layoutMainStage.tvParticipantResolution.setText("");
            binding.layoutMainStage.tvPriority.setText("");
        }
    }

    private void updateResolutionTextView(ObservableValueWithOptional<Size> videoResolution) {
        resolutionDisposable = new CompositeDisposable();
        resolutionDisposable.add(videoResolution.getRxObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(size -> {
                    if (size.getValue() != null) {
                        Log.i(
                                TAG,
                                "Received Resolution " + size.getValue().getWidth() + " x " + size.getValue().getHeight() + " for main stage participant"
                        );
                        binding.layoutMainStage.tvParticipantResolution.setText(size.getValue().getWidth() + " x " + size.getValue().getHeight());
                    } else {
                        binding.layoutMainStage.tvParticipantResolution.setText("0 x 0");
                    }
                }, err -> {
                    Timber.tag(TAG).e("Error observing main stage resolution: " + err.getMessage());
                }));
    }

    private void updatePriorityTextView(StreamPriority priority) {
        binding.layoutMainStage.tvPriority.setText(getPriorityName(priority));
    }

    private void subscribeToRosterUpdates() {
        compositeDisposable.add(
                participantsService.getParticipants().getRxObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(participants -> {
                            if (participants.getValue() != null) {
                                participants.getValue().forEach(p -> {
                                    if (mainStageParticipant != null && p.getId().equals(mainStageParticipant.getSeamGuid())) {
                                        binding.layoutMainStage.tvParticipantName.setText(p.getName());
                                    }
                                });
                            }
                        }, err -> {
                            Timber.tag(TAG).e("Error in getting roster updates: " + err.getMessage());
                        })
        );
    }

    private void addParticipantToStrip(List<ParticipantUI> participantUi) {
        binding.stripContainer.removeAllViews();

        final View[] participantView = new View[1];
        final ParticipantUIView[] tileStripParticipantViewManager = new ParticipantUIView[1];
        participantUi.forEach(ui -> {
                    tileStripParticipantViewManager[0] = new ParticipantUIView();
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            (int) (((int) getResources().getDimension(R.dimen.isc_tile_width)) * Resources.getSystem().getDisplayMetrics().density),
                            LinearLayout.LayoutParams.MATCH_PARENT
                    );
                    params.setMargins(10, 10, 10, 10);
                    tileStripParticipantViewManager[0].bind(
                        binding.stripContainer
                    );
                    participantView[0] = tileStripParticipantViewManager[0].getParticipantView();
                    participantView[0].setLayoutParams(params);
                    binding.stripContainer.addView(participantView[0]);
                    participantViewManagerMap.put(ui.getSeamGuid(), tileStripParticipantViewManager[0]);
                    tileStripParticipantViewManager[0].associateParticipantUI(
                        new ParticipantUI(
                            ui.getSeamGuid(),
                            ui.getName(),
                            ui.getVideoWidth(),
                            ui.getVideoHeight(),
                            ui.getVideoResolution(),
                            ui.getVideo(),
                            participantsConfigMap.get(ui.getSeamGuid()).getStreamPriority(),
                            participantsConfigMap.get(ui.getSeamGuid()).getStreamQuality(),
                            ui.getPinned()
                        )
                    );
                }
        );
    }


    private void configurePortraitView() {
        constraintSet.clear(R.id.layoutMainStage);
        constraintSet.clear(binding.rvIscParticipantTilesList.getId());
        constraintSet.constrainWidth(
                R.id.layoutMainStage,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        );
        constraintSet.constrainHeight(
                R.id.layoutMainStage,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        );
        constraintSet.connect(
                R.id.layoutMainStage,
                ConstraintSet.TOP,
                binding.iscRoot.getId(),
                ConstraintSet.TOP
        );
        constraintSet.connect(
                R.id.layoutMainStage,
                ConstraintSet.BOTTOM,
                binding.iscRoot.getId(),
                ConstraintSet.BOTTOM
        );
        constraintSet.connect(
                R.id.layoutMainStage,
                ConstraintSet.START,
                binding.iscRoot.getId(),
                ConstraintSet.START
        );
        constraintSet.connect(
                R.id.layoutMainStage,
                ConstraintSet.END,
                binding.iscRoot.getId(),
                ConstraintSet.END
        );

        constraintSet.setDimensionRatio(R.id.layoutMainStage, "1.5:1");

        constraintSet.constrainWidth(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        );

        constraintSet.constrainHeight(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        );
        constraintSet.connect(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintSet.TOP, R.id.layoutMainStage, ConstraintSet.BOTTOM
        );
        constraintSet.connect(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintSet.BOTTOM, binding.iscRoot.getId(), ConstraintSet.BOTTOM
        );
        constraintSet.connect(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintSet.START, binding.iscRoot.getId(), ConstraintSet.START
        );
        constraintSet.connect(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintSet.END, binding.iscRoot.getId(), ConstraintSet.END
        );

        participantViewManagerMap.forEach((key, value) -> {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (((int) getResources().getDimension(R.dimen.isc_tile_width)) * Resources.getSystem().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            params.setMargins(10, 10, 10, 10);
            value.getParticipantView().setLayoutParams(params);
            value.getParticipantView().requestLayout();
        });
        constraintSet.applyTo(binding.iscRoot);
    }

    private void configureLandscapeView() {

        constraintSet.clear(R.id.layoutMainStage);
        constraintSet.clear(binding.rvIscParticipantTilesList.getId());
        constraintSet.constrainWidth(
                R.id.layoutMainStage,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        );

        constraintSet.constrainHeight(
                R.id.layoutMainStage,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        );
        constraintSet.connect(
                R.id.layoutMainStage,
                ConstraintSet.TOP,
                binding.iscRoot.getId(),
                ConstraintSet.TOP
        );
        constraintSet.connect(
                R.id.layoutMainStage,
                ConstraintSet.START,
                binding.iscRoot.getId(),
                ConstraintSet.START
        );
        constraintSet.connect(
                R.id.layoutMainStage,
                ConstraintSet.END,
                binding.iscRoot.getId(),
                ConstraintSet.END
        );
        constraintSet.constrainPercentHeight(R.id.layoutMainStage, 0.55f);
        constraintSet.setDimensionRatio(R.id.layoutMainStage, "1.5:1");
        constraintSet.constrainWidth(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );

        constraintSet.constrainHeight(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        );
        constraintSet.connect(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintSet.BOTTOM,
                binding.iscRoot.getId(),
                ConstraintSet.BOTTOM
        );
        constraintSet.connect(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintSet.TOP,
                R.id.layoutMainStage,
                ConstraintSet.BOTTOM
        );
        constraintSet.connect(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintSet.END,
                binding.iscRoot.getId(),
                ConstraintSet.END
        );
        constraintSet.connect(
                binding.rvIscParticipantTilesList.getId(),
                ConstraintSet.START,
                binding.iscRoot.getId(),
                ConstraintSet.START
        );

        participantViewManagerMap.forEach((key, value) -> {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (((int) getResources().getDimension(R.dimen.isc_tile_width)) * Resources.getSystem().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            params.setMargins(5, 5, 5, 5);
            value.getParticipantView().setLayoutParams(params);
            value.getParticipantView().requestLayout();
        });

        constraintSet.applyTo(binding.iscRoot);
    }

    private void setTextureViewConstraints(@NonNull TextureView textureView, @NonNull ConstraintLayout parent) {
        ConstraintSet set = new ConstraintSet();
        set.clone(parent);
        set.connect(textureView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
        set.connect(textureView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        set.connect(textureView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        set.connect(textureView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

        set.applyTo(parent);
    }

    private void setMainStageTextureViewConstraints() {
        ConstraintSet set = new ConstraintSet();
        TextureView textureView = binding.layoutMainStage.participantTextureView;
        set.clone((ConstraintLayout) textureView.getParent());
        set.connect(textureView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
        set.connect(textureView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        set.connect(textureView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        set.connect(textureView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

        if (mainStageParticipant != null) {
            if (mainStageParticipant.getVideoWidth() != 0 && mainStageParticipant.getVideoHeight() != 0) {
                float ratio = (float) mainStageParticipant.getVideoWidth() / mainStageParticipant.getVideoHeight();
                Log.i(TAG, "Ratio: " + ratio);
                set.setDimensionRatio(textureView.getId(), String.valueOf(ratio));
            }
        }

        set.applyTo((ConstraintLayout) textureView.getParent());
    }

    private void setMainStageConstraints() {
        ConstraintSet set = new ConstraintSet();
        set.clone(binding.layoutMainStage.getRoot());
        set.connect(
                binding.layoutMainStage.vTranslucentBar.getId(),
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
        );
        set.applyTo(binding.layoutMainStage.getRoot());
    }
}