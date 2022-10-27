package com.bluejeans.android.sdksample.participantlist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bluejeans.android.sdksample.databinding.FragmentIscParticipantListBinding;
import com.bluejeans.android.sdksample.isc.IStreamConfigUpdatedCallback;
import com.bluejeans.android.sdksample.isc.IscParticipantListAdapter;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import java.util.ArrayList;
import java.util.List;

public class IscParticipantListFragment extends Fragment {

    private static final String TAG = "IscParticipantListFragment";

    private IStreamConfigUpdatedCallback streamConfigUpdatedCallback;
    private FragmentIscParticipantListBinding participantsViewBinding;
    private IscParticipantListAdapter iscParticipantListAdapter;
    private List<ParticipantsService.Participant> participantsList = new ArrayList<>();
    private String pinnedParticipant = null;
    private Context context;

    public IscParticipantListFragment(IStreamConfigUpdatedCallback streamConfigUpdatedCallback,
                                      Context context) {
        this.streamConfigUpdatedCallback = streamConfigUpdatedCallback;
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        participantsViewBinding = FragmentIscParticipantListBinding.inflate(inflater, container, false);
        return participantsViewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addListeners();
        loadIscParticipantsListAdapter();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        iscParticipantListAdapter = null;
    }

    public void updateMeetingList(List<ParticipantsService.Participant> rosterList) {
        if (rosterList != null) {
            participantsList.clear();
            participantsList.addAll(rosterList);
            if (iscParticipantListAdapter == null) {
                iscParticipantListAdapter = new IscParticipantListAdapter(context, streamConfigUpdatedCallback, pinnedParticipant);
            }
            iscParticipantListAdapter.updateMeetingList(rosterList);
        } else {
            participantsList.clear();
            iscParticipantListAdapter.updateMeetingList(participantsList);
        }
    }

    public void setPinnedParticipant(String pinnedParticipant) {
        this.pinnedParticipant = pinnedParticipant;
    }

    private void addListeners() {
        participantsViewBinding.closeIscRoster.setOnClickListener(view -> {
            getActivity().onBackPressed();
        });
    }

    private void loadIscParticipantsListAdapter() {
        if (iscParticipantListAdapter == null) {
            iscParticipantListAdapter = new IscParticipantListAdapter(getContext(), streamConfigUpdatedCallback, pinnedParticipant);
        }
        participantsViewBinding.rvRosterIscParticipants.setAdapter(iscParticipantListAdapter);
        iscParticipantListAdapter.updateMeetingList(participantsList);
    }
}