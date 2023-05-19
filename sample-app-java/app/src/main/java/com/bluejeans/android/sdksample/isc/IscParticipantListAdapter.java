package com.bluejeans.android.sdksample.isc;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bjnclientcore.media.individualstream.StreamPriority;
import com.bjnclientcore.media.individualstream.StreamQuality;
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration;
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult;
import com.bluejeans.android.sdksample.R;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.android.sdksample.databinding.RowIscParticipantBinding;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.bluejeanssdk.meeting.individualstream.VideoStreamService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IscParticipantListAdapter extends RecyclerView.Adapter<IscParticipantListAdapter.IscRosterViewHolder> {
    private static final String TAG = "IscParticipantListAdapter";

    private Context context;
    private IStreamConfigUpdatedCallback configUpdatedCallback;
    private final String[] priorities = {"Default", "High", "Medium", "Low"};
    private final String[] resolutions = {
            "R90_3fps",
            "R90_7fps",
            "R90_15fps",
            "R180_7fps",
            "R180_15fps",
            "R180_30fps",
            "R360_7fps",
            "R360_15fps",
            "R360_30fps",
            "R720_7fps",
            "R720_15fps",
            "R720_30fps"
    };

    private ArrayAdapter<String> spPriorityAdapter;
    private ArrayAdapter<String> spResolutionAdapter;

    private List<ParticipantsService.Participant> participantsList = new ArrayList<>();
    private List<VideoStreamConfiguration> videoStreamConfigurations = new ArrayList<>();
    private VideoStreamService iscService = SampleApplication.getBlueJeansSDK().getMeetingService().getVideoStreamService();
    private ParticipantsService.Participant currentParticipant = null;
    private String pinnedParticipant = null;

    public IscParticipantListAdapter(Context context, IStreamConfigUpdatedCallback configUpdatedCallback, String pinnedParticipant) {
        this.context = context;
        this.configUpdatedCallback = configUpdatedCallback;
        this.pinnedParticipant = pinnedParticipant;
        spPriorityAdapter = new ArrayAdapter(
                context,
                R.layout.row_isc_config,
                priorities
        );
        spResolutionAdapter = new ArrayAdapter(
                context,
                R.layout.row_isc_config,
                resolutions
        );
    }

    @NonNull
    @Override
    public IscRosterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowIscParticipantBinding itemBinding =
                RowIscParticipantBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new IscRosterViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull IscRosterViewHolder holder, int position) {
        currentParticipant = this.participantsList.get(position);
        holder.bind(currentParticipant);
    }

    @Override
    public int getItemCount() {
        return participantsList.size();
    }

    public void updateMeetingList(List<ParticipantsService.Participant> participantsList) {
        this.participantsList.clear();
        this.participantsList.addAll(participantsList.stream().filter(p -> !p.isSelf()).collect(Collectors.toList()));
        notifyDataSetChanged();
    }

    protected class IscRosterViewHolder extends RecyclerView.ViewHolder {
        private RowIscParticipantBinding bindingView;

        public IscRosterViewHolder(RowIscParticipantBinding bindingView) {
            super(bindingView.getRoot());
            this.bindingView = bindingView;
        }

        public void bind(ParticipantsService.Participant participant) {
            bindingView.tvIscParticipantName.setText(participant.getName());
            bindingView.spParticipantPriority.setAdapter(spPriorityAdapter);
            bindingView.spParticipantResolution.setAdapter(spResolutionAdapter);

            videoStreamConfigurations.clear();
            videoStreamConfigurations.addAll(iscService.getVideoStreamConfigurations());

            if (currentParticipant != null) {
                bindingView.spParticipantPriority.setSelection(0, false);
                bindingView.spParticipantResolution.setSelection(0, false);
                bindingView.swPinParticipant.setChecked(Objects.equals(pinnedParticipant, participant.getId()));
            }

            Optional<VideoStreamConfiguration> participantConfiguration = iscService.getVideoStreamConfigurations().stream()
                    .filter(p -> currentParticipant.getId().equals(p.getParticipantGuid())).findFirst();

            if (participantConfiguration.isPresent()) {
                bindingView.spParticipantResolution.setSelection(
                        Arrays.asList(resolutions).indexOf(
                                resolutions[participantConfiguration.get().getStreamQuality().ordinal()]
                        )
                );

                if (participantConfiguration.get().getStreamPriority() != null) {
                    bindingView.spParticipantPriority.setSelection(
                            Arrays.asList(priorities).indexOf(
                                    priorities[participantConfiguration.get().getStreamPriority().ordinal()]
                            )
                    );
                }
            }

            if (pinnedParticipant != null && pinnedParticipant.equals(participant.getId())) {
                bindingView.swPinParticipant.setChecked(true);
            }

            bindingView.swPinParticipant.setOnCheckedChangeListener((compoundButton, b) -> {
                configUpdatedCallback.pinParticipant(participant.getId(), b);
            });

            bindingView.btnApplyConfig.setOnClickListener(view -> {
                if (participantConfiguration.isPresent() && participantConfiguration.get().getStreamPriority() !=
                        IscUtils.getStreamPriorityFromString(bindingView.spParticipantPriority.getSelectedItem().toString()) &&
                participantConfiguration.get().getStreamQuality() == IscUtils.getStreamQualityFromString(bindingView.spParticipantResolution.getSelectedItem().toString())) {
                    Toast.makeText(context, context.getString(R.string.priority_error), Toast.LENGTH_LONG).show();
                } else {
                    Log.i(
                            TAG, "Setting resolution to: " +
                                    bindingView.spParticipantResolution.getSelectedItem().toString() + " and priority to: " +
                                    bindingView.spParticipantPriority.getSelectedItem().toString() + " for P "
                                    + participant.getName() + " (" + participant.getId() + ")"
                    );
                    applyVideoStreamConfiguration(
                            participant.getId(),
                            IscUtils.getStreamQualityFromString(bindingView.spParticipantResolution.getSelectedItem().toString()),
                            IscUtils.getStreamPriorityFromString(bindingView.spParticipantPriority.getSelectedItem().toString())
                    );
                }
            });
        }

        private void applyVideoStreamConfiguration(
                String participantGuid,
                StreamQuality quality,
                StreamPriority priority
        ) {
            List<VideoStreamConfiguration> currentStreamConfigurations =
                    iscService.getVideoStreamConfigurations();
            List<VideoStreamConfiguration> updatedConfiguration = new ArrayList<>();
            VideoStreamConfiguration newVideoStreamConfig = new VideoStreamConfiguration(
                    participantGuid,
                    quality,
                    priority
            );

            currentStreamConfigurations.forEach(config -> {
                if (config.getParticipantGuid() == newVideoStreamConfig.getParticipantGuid()) {
                    updatedConfiguration.add(newVideoStreamConfig);
                } else {
                    updatedConfiguration.add(config);
                }
            });


            Log.i(
                    TAG,
                    "stream config: " + newVideoStreamConfig.getStreamQuality() + ", " + newVideoStreamConfig.getStreamPriority() + " " +
                            "for participant " + newVideoStreamConfig.getParticipantGuid()
            );

            VideoStreamConfigurationResult result =
                    iscService.setVideoStreamConfiguration(
                            updatedConfiguration
                    );

            if (result.equals(VideoStreamConfigurationResult.Success.INSTANCE)) {
                Log.i(
                        TAG,
                        "Stream config applied successfully"
                );
                Toast.makeText(context, "Stream config requested successfully", Toast.LENGTH_LONG).show();
                videoStreamConfigurations.clear();
                videoStreamConfigurations.addAll(iscService.getVideoStreamConfigurations());

                configUpdatedCallback.onStreamConfigurationUpdated(participantGuid, quality, priority);
            } else if (result.equals(VideoStreamConfigurationResult.Failure.class)) {
                Log.i(
                        TAG,
                        "Failed to apply config $result");
            } else {
                Log.e(TAG, "Some error caused while applying config: $result");
            }
        }
    }
}
