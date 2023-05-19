/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample.participantlist;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.bluejeans.android.sdksample.R;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.bluejeanssdk.meeting.MeetingService;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.bluejeanssdk.meeting.chat.PrivateChatService;
import com.bluejeans.bluejeanssdk.meeting.chat.PublicChatService;
import com.bluejeans.rxextensions.ObservableComputed;
import com.bluejeans.rxextensions.ObservableValue;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import kotlin.Unit;

/**
 * Participant display fragment
 */
public class ParticipantListFragment extends Fragment implements ParticipantChatItemListener {
    private static final String TAG = "ParticipantListFragment";

    private enum FragmentView {
        CHAT_PARTICIPANTS, PARTICIPANT_LIST, CHAT_VIEW
    }

    private ParticipantListAdapter participantListAdapter;
    private RecyclerView participantListView;
    private ImageView chatIcon, closeIcon, peopleIcon;
    private TextView unreadChatText, titleText;
    static final String EVERYONE = "Everyone";
    private final ArrayList<ParticipantsService.Participant> participantsList = new ArrayList<>();
    private CompositeDisposable chatDisposable = null;
    private CompositeDisposable participantDisposable = null;
    CompositeDisposable countDisposable = new CompositeDisposable();
    private FragmentView selectedView = FragmentView.PARTICIPANT_LIST;
    private int totalCount = 0;
    private PublicChatService publicChatService = null;
    private PrivateChatService privateChatService = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_participant_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        participantListView = view.findViewById(R.id.rvRosterParticipants);
        participantListView.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL));
        loadParticipantListAdapter();
        chatIcon = view.findViewById(R.id.ivChatBubble);
        peopleIcon = view.findViewById(R.id.ivPeople);
        unreadChatText = view.findViewById(R.id.tvUnreadChatCount);
        unreadChatText.setVisibility(View.GONE);
        titleText = view.findViewById(R.id.tvRosterHeading);
        participantListAdapter.updateMeetingList(participantsList);
        closeIcon = view.findViewById(R.id.closeRoster);
        addClickListeners();
        participantDisposable = new CompositeDisposable();
        subscribeForChatServices();
    }

    @Override
    public void onDestroyView() {
        participantListAdapter = null;
        if (!participantDisposable.isDisposed()) {
            participantDisposable.dispose();
        }
        if (chatDisposable != null && !chatDisposable.isDisposed()) {
            chatDisposable.dispose();
        }
        if (!countDisposable.isDisposed()) {
            countDisposable.dispose();
        }
        // important to dispose the references or avoid using them
        publicChatService = null;
        privateChatService = null;
        selectedView = FragmentView.PARTICIPANT_LIST;
        super.onDestroyView();
    }

    @Override
    public void onMessageCountChange(ParticipantsService.Participant participant, TextView mUnreadCount) {
        if (participant.getId().equals(EVERYONE)) {
            subscribeToPublicChatForCount(mUnreadCount);
        } else {
            subscribeToPrivateChatForParticipantCount(participant, mUnreadCount);
        }
    }

    @Override
    public void onParticipantClick(ParticipantsService.Participant participant) {
        if (participant.getId().equals(EVERYONE) && publicChatService != null) {
            publicChatService.clearUnreadMessagesCount();
            setTitle(EVERYONE);
        } else if (privateChatService != null) {
            privateChatService.clearUnreadMessagesCountByParticipant(participant);
            setTitle(participant.getName());
        }
        participantListView.setVisibility(View.GONE);
        getChildFragmentManager().beginTransaction().
                replace(R.id.chatFragHolder, ChatParticipantFragment.newInstance(participant))
                .addToBackStack("ParticipantChat")
                .commit();
        chatIcon.setVisibility(View.GONE);
        peopleIcon.setVisibility(View.GONE);
        closeIcon.setImageResource(R.drawable.ic_back_icon);
        selectedView = FragmentView.CHAT_VIEW;
    }

    public void updateMeetingList(List<ParticipantsService.Participant> participantList) {
        if (participantListAdapter != null && selectedView == FragmentView.PARTICIPANT_LIST) {
            participantListAdapter.updateMeetingList(participantList);
        }
        participantsList.clear();
        participantsList.addAll(participantList);
    }

    private void addClickListeners() {
        closeIcon.setOnClickListener(v -> {
            if (selectedView == FragmentView.CHAT_VIEW) {
                handleChatViewBack();
            } else {
               requireActivity().onBackPressed();
            }
        });
        chatIcon.setOnClickListener(v -> {
            if (publicChatService != null && selectedView == FragmentView.PARTICIPANT_LIST) {
                if (chatDisposable == null || chatDisposable.isDisposed()) {
                    chatDisposable = new CompositeDisposable();
                }
                selectedView = FragmentView.CHAT_PARTICIPANTS;
                loadChatAdapter();
                setTitle(getString(R.string.chat));
                chatIcon.setVisibility(View.GONE);
                peopleIcon.setVisibility(View.VISIBLE);
                unreadChatText.setVisibility(View.GONE);
                totalCount = 0;
                handleUnreadCountDisplay();
            } else if (selectedView != FragmentView.CHAT_PARTICIPANTS && publicChatService == null) {
                Toast.makeText(getContext(), "Chat Unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        peopleIcon.setOnClickListener(v -> {
            if (chatDisposable != null) {
                chatDisposable.dispose();
            }
            selectedView = FragmentView.PARTICIPANT_LIST;
            loadParticipantListAdapter();
            setTitle(getString(R.string.people));
            chatIcon.setVisibility(View.VISIBLE);
            peopleIcon.setVisibility(View.GONE);
            unreadChatText.setVisibility(View.VISIBLE);
        });
    }

    private void setTitle(String title) {
        titleText.setText(title);
    }

    private void subscribeForChatServices() {
        MeetingService meetingService = SampleApplication.getBlueJeansSDK().getMeetingService();
        participantDisposable.add(meetingService.getPublicChatService().getChatServiceState().subscribeOnUI(obj -> {
            if (obj != null) {
                publicChatService = meetingService.getPublicChatService();
                subscriberForChatServicesMessageCount();
            } else {
                publicChatService = null;
            }
            return Unit.INSTANCE;
        }, err -> {
            Log.e(TAG, "Unable to subscribe for public chat service");
            return Unit.INSTANCE;
        }));
        participantDisposable.add(meetingService.getPrivateChatService().getChatServiceState().subscribeOnUI(obj -> {
            if (obj != null) {
                privateChatService = meetingService.getPrivateChatService();
                subscriberForChatServicesMessageCount();
            } else {
                privateChatService = null;
            }
            return Unit.INSTANCE;
        }, err -> {
            Log.e(TAG, "Unable to subscribe for private chat service");
            return Unit.INSTANCE;
        }));
    }

    private void handleChatViewBack() {
        getChildFragmentManager().popBackStack();
        chatIcon.setVisibility(View.GONE);
        peopleIcon.setVisibility(View.VISIBLE);
        unreadChatText.setVisibility(View.GONE);
        participantListView.setVisibility(View.VISIBLE);
        selectedView = FragmentView.CHAT_PARTICIPANTS;
        closeIcon.setImageResource(R.drawable.ic_close_dark_blue);
        setTitle(getString(R.string.chat));
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.requireView().getWindowToken(), 0);
    }

    private void loadParticipantListAdapter() {
        participantListAdapter = new ParticipantListAdapter(getContext());
        participantListView.setAdapter(participantListAdapter);
        participantListAdapter.updateMeetingList(participantsList);
        subscriberForChatServicesMessageCount();
    }

    private void loadChatAdapter() {
        participantListAdapter = new ParticipantListAdapter(getContext(), true, this);
        participantListView.setAdapter(participantListAdapter);
        if (privateChatService != null) {
            chatDisposable.add(privateChatService.getEligibleParticipants().subscribeOnUI(list -> {
                if (list != null) {
                    Log.i(TAG, "Setting eligible list size is " + list.size());
                    // removing self
                    participantListAdapter.updateMeetingList(list);
                }
                return Unit.INSTANCE;
            }, err -> {
                Log.e(TAG, "Unable to subsribe to private chat service");
                return Unit.INSTANCE;
            }));
        } else {
            Log.i(TAG, "Private chat is null");
            participantListAdapter.updateMeetingList(new ArrayList<>());
        }
    }

    private void handleUnreadCountDisplay() {
        if (this.isVisible() && selectedView == FragmentView.PARTICIPANT_LIST) {
            if (totalCount > 0) {
                String countString = "" + totalCount;
                unreadChatText.setText(countString);
                unreadChatText.setVisibility(View.VISIBLE);
            } else {
                unreadChatText.setVisibility(View.GONE);
            }
        }
    }

    private void subscriberForChatServicesMessageCount() {
        if (publicChatService != null && privateChatService != null) {
            if (!countDisposable.isDisposed()) {
                countDisposable.dispose();
            }
            countDisposable = new CompositeDisposable();
            ObservableValue<Integer> countObserver;
            countObserver = ObservableComputed.Companion.create(publicChatService.getUnreadMessagesCount(), privateChatService.getUnreadMessagesCount(), new CompositeDisposable(),true, (integer, integer2) -> {
                if (integer.getValue() == null && integer2.getValue() == null) return 0;
                else if (integer.getValue() != null && integer2.getValue() == null)
                    return integer.getValue();
                else if (integer.getValue() == null && integer2.getValue() != null)
                    return integer2.getValue();
                else return integer.getValue() + integer2.getValue();
            });
            countDisposable.add(countObserver.subscribeOnUI(c -> {
                totalCount = c;
                handleUnreadCountDisplay();
                return Unit.INSTANCE;
            }, err -> {
                Log.e(TAG, "Unable to get total msg count");
                return Unit.INSTANCE;
            }));
        } else {
            Log.e(TAG, "Value of public chat service = " + publicChatService + " and private chat service" + privateChatService);
        }
    }

    private void subscribeToPublicChatForCount(TextView unreadCount) {
        if (publicChatService != null) {
            chatDisposable.add(publicChatService.getUnreadMessagesCount().subscribeOnUI(
                    count -> {
                        if (count > 0) {
                            unreadCount.setVisibility(View.VISIBLE);
                            String countString = " " + count;
                            unreadCount.setText(countString);
                        } else {
                            unreadCount.setVisibility(View.GONE);
                        }
                        return Unit.INSTANCE;
                    }, err -> {
                        Log.e(TAG, "Error subscribing to public chat count");
                        unreadCount.setVisibility(View.GONE);
                        return Unit.INSTANCE;
                    }
            ));
        }
    }

    private void subscribeToPrivateChatForParticipantCount(ParticipantsService.Participant participant, TextView unreadCount) {
        if (privateChatService != null) {
            chatDisposable.add(privateChatService.getUnreadCountForParticipant().subscribeOnUI(
                    map -> {
                        if (map.containsKey(participant)) {
                            chatDisposable.add(map.get(participant).subscribeOnUI(count -> {
                                if (count > 0) {
                                    unreadCount.setVisibility(View.VISIBLE);
                                    String countString = " " + count;
                                    unreadCount.setText(countString);
                                } else {
                                    unreadCount.setVisibility(View.GONE);
                                }
                                return Unit.INSTANCE;
                            }, err -> {
                                Log.e(TAG, "Error subscribing to private chat count");
                                unreadCount.setVisibility(View.GONE);
                                return Unit.INSTANCE;
                            }));
                        }
                        return Unit.INSTANCE;
                    }, err -> {
                        Log.e(TAG, "Error subscribing to private chat participant count map");
                        unreadCount.setVisibility(View.GONE);
                        return Unit.INSTANCE;
                    }
            ));
        }
    }
}
