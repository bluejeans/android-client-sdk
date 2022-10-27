package com.bluejeans.android.sdksample.isc;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.android.sdksample.databinding.IscParticipantBinding;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.bluejeanssdk.meeting.individualstream.VideoStreamService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static com.bluejeans.android.sdksample.isc.IscUtils.getPriorityName;
import static com.bluejeans.android.sdksample.isc.IscUtils.getStreamQualityName;

import timber.log.Timber;

public class IscTileAdapter extends RecyclerView.Adapter<IscTileAdapter.IscTileHolder> {
    private static final String TAG = "IscTileAdapter";
    private static final String PR = "PR--";

    private List<ParticipantUI> participantsList = new ArrayList<>();
    private ParticipantUI currentParticipant = null;
    private Map<String, VideoStreamConfiguration> participantsConfigMap = new HashMap<>();

    private final VideoStreamService iscService;

    public IscTileAdapter() {
        iscService = SampleApplication.getBlueJeansSDK()
                .getMeetingService().getVideoStreamService();
    }

    class IscTileHolder extends RecyclerView.ViewHolder {

        private final IscParticipantBinding bindingView;

        public IscTileHolder(@NonNull IscParticipantBinding bindingView) {
            super(bindingView.getRoot());
            this.bindingView = bindingView;
        }

        public void bind(ParticipantUI participant) {
            bindingView.tvParticipantName.setText(participant.getName());

            if (participant.getVideo()) {
                bindingView.cnhAudioOnly.setVisibility(View.GONE);
                bindingView.participantTextureView.setVisibility(View.VISIBLE);
                iscService.detachParticipantFromView(participant.getSeamGuid());
                iscService.attachParticipantToView(participant.getSeamGuid(), bindingView.participantTextureView);

                setStreamPriority(participant.getSeamGuid());
                applyConstraintSet(participant.getVideoWidth(), participant.getVideoHeight());
            } else {
                bindingView.cnhAudioOnly.setVisibility(View.VISIBLE);
                bindingView.participantTextureView.setVisibility(View.GONE);
//                bindingView.cnhAudioOnly.setText(participant.getName().getInitials());
                bindingView.tvPriority.setText("");
                bindingView.tvParticipantResolution.setText("0x0");
            }
        }

        private void setStreamPriority(String seamGuid) {
            bindingView.tvParticipantResolution.setText(getStreamQualityName(participantsConfigMap.get(seamGuid).getStreamQuality()));
            bindingView.tvPriority.setText(getPriorityName(participantsConfigMap.get(seamGuid).getStreamPriority()));
        }

        private void applyConstraintSet(int videoWidth, int videoHeight) {
            try {
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(bindingView.getRoot());
                float ratio = videoWidth / (float) videoHeight;
                constraintSet.setDimensionRatio(
                        bindingView.participantTextureView.getId(),
                        String.valueOf(ratio)
                );
                constraintSet.applyTo(bindingView.getRoot());
            } catch (ArithmeticException e) {
                Log.e(TAG, "${e.message}");
            }
        }
    }

    @NonNull
    @Override
    public IscTileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        IscParticipantBinding binding = IscParticipantBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        setConstraints(binding);
        return new IscTileHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull IscTileHolder holder, int position) {
        currentParticipant = participantsList.get(position);
        if (currentParticipant != null) {
            holder.bind(currentParticipant);
        } else {
            Timber.tag(PR).i("Current participant is null");
        }
    }

    @Override
    public int getItemCount() {
        return participantsList.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        participantsList.forEach(p -> {
            iscService.detachParticipantFromView(p.getSeamGuid());
        });
    }

    public void updateParticipantList(List<ParticipantUI> participants, Map<String, VideoStreamConfiguration> participantsMap) {
        this.participantsConfigMap = participantsMap;
        participantsList = participants;
        notifyDataSetChanged();
    }

    public void updateParticipantMap(Map<String, VideoStreamConfiguration> participantsMap) {
        this.participantsConfigMap = participantsMap;
        notifyDataSetChanged();
    }

    public void updateNames(@NonNull List<ParticipantsService.Participant> participants) {
        participants.forEach(participant -> {
            participantsList.forEach(participantUI -> {
                if (Objects.equals(participantUI.getSeamGuid(), participant.getId())) {
                    participantsList.get(participantsList.indexOf(participantUI)).setName(participant.getName());
                }
            });
        });

        notifyDataSetChanged();
    }

    private void setConstraints(@NonNull IscParticipantBinding bindingView) {
        ConstraintSet set = new ConstraintSet();
        set.clone(bindingView.getRoot());
        set.connect(bindingView.vTranslucentBar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        set.applyTo(bindingView.getRoot());
    }
}
