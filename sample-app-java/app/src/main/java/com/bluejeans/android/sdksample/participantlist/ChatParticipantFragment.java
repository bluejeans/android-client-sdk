/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample.participantlist;

import static com.bluejeans.android.sdksample.participantlist.ParticipantListFragment.EVERYONE;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bjnclientcore.inmeeting.chat.model.ChatMessage;
import com.bluejeans.android.sdksample.R;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.bluejeanssdk.meeting.MeetingService;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.bluejeanssdk.meeting.chat.PrivateChatService;
import com.bluejeans.bluejeanssdk.meeting.chat.PublicChatService;
import com.bluejeans.rxextensions.ObservableValue;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import kotlin.Unit;

public class ChatParticipantFragment extends Fragment {
    private ChatParticipantFragment() {
    }

    private static final String TAG = "ChatParticipantFragment";
    private ParticipantsService.Participant remoteParticipant;
    private CompositeDisposable disposable = null;
    private ImageView sendMessage;
    private RecyclerView msgList;
    private ChatMessagesAdapter adapter;
    private EditText msgEditText;
    private PublicChatService publicChatService;
    private PrivateChatService privateChatService;
    private final DiffUtil.ItemCallback<ChatMessage> callback = new DiffUtil.ItemCallback<ChatMessage>() {

        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };

    public static ChatParticipantFragment newInstance(ParticipantsService.Participant participant) {
        ChatParticipantFragment fragment = new ChatParticipantFragment();
        fragment.remoteParticipant = participant;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_participant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        disposable = new CompositeDisposable();
        msgList = view.findViewById(R.id.messagesView);
        msgList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        msgEditText = view.findViewById(R.id.etMessage);
        sendMessage = view.findViewById(R.id.ivSendMsg);
        subscribeForChatServiceStates();
        addSendMessageListener();
    }

    private void subscribeForChatServiceStates() {
        MeetingService meetingService = SampleApplication.getBlueJeansSDK().getMeetingService();
        if (remoteParticipant.getId().equals(EVERYONE)) {
            disposable.add(meetingService.getPublicChatService().getChatServiceState().subscribeOnUI(state -> {
                        if (state == null) {
                            publicChatService = null;
                        } else {
                            if (publicChatService == null) {
                                publicChatService = meetingService.getPublicChatService();
                                attachPublicAdapter();
                                loadMsgsFromHistory();
                                subscribePublicChatHistory();
                            }
                        }
                        return Unit.INSTANCE;
                    },
                    err -> {
                        Log.e(TAG, "Unable to subscribe for public chat service");
                        return Unit.INSTANCE;
                    }));
        } else {
            disposable.add(meetingService.getPrivateChatService().getChatServiceState().subscribeOnUI(state -> {
                        if (state == null) {
                            privateChatService = null;
                        } else {
                            if (privateChatService == null) {
                                privateChatService = meetingService.getPrivateChatService();
                                attachPrivateAdapter();
                                loadMsgsFromHistory();
                                subscribePrivateChatHistory();
                            }
                        }
                        return Unit.INSTANCE;
                    },
                    err -> {
                        Log.e(TAG, "Unable to subscribe for private chat service");
                        return Unit.INSTANCE;
                    }));
        }
    }

    private void addSendMessageListener() {
        sendMessage.setOnClickListener(v -> {
            String message = msgEditText.getText().toString();
            if (remoteParticipant.getId().equals(EVERYONE)) {
                if (publicChatService != null &&
                        publicChatService.getChatServiceState().getValue() ==
                                MeetingService.MeetingChatServiceStatus.Active.INSTANCE) {
                    if (!message.isEmpty()) {
                        publicChatService.sendMessage(message);
                        msgEditText.setText("");
                    }
                } else {
                    Toast.makeText(getContext(), "Unable to send message at the moment", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (privateChatService != null &&
                        privateChatService.getChatServiceState().getValue() ==
                                MeetingService.MeetingChatServiceStatus.Active.INSTANCE) {
                    if (!message.isEmpty()) {
                        privateChatService.sendMessage(message, remoteParticipant);
                        msgEditText.setText("");
                    }
                } else {
                    Toast.makeText(getContext(), "Unable to send message at the moment", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadMsgsFromHistory() {
        if (remoteParticipant.getId().equals(EVERYONE)) {
            if (publicChatService.getChatHistory().getValue() != null) {
                adapter.updateMessages(publicChatService.getChatHistory().getValue());
            }
        } else {
            if (Objects.requireNonNull(privateChatService.getChatHistoryByParticipant().getValue()).containsKey(remoteParticipant)) {
                if (privateChatService.getChatHistoryByParticipant().getValue() != null && privateChatService.getChatHistoryByParticipant().getValue().get(remoteParticipant).getValue() != null) {
                    adapter.updateMessages(privateChatService.getChatHistoryByParticipant().getValue().get(remoteParticipant).getValue());
                }
            }
        }
    }

    private void attachPublicAdapter() {
        adapter = new ChatMessagesAdapter(true, callback);
        msgList.setAdapter(adapter);
    }

    private void attachPrivateAdapter() {
        adapter = new ChatMessagesAdapter(false, callback);
        msgList.setAdapter(adapter);
    }

    private void subscribePublicChatHistory() {
        disposable.add(publicChatService.getChatHistory().subscribeOnUI(list -> {
                    if (adapter != null && list != null) {
                        adapter.updateMessages(list);
                        publicChatService.clearUnreadMessagesCount();
                        msgList.smoothScrollToPosition(adapter.getItemCount());
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Unable to subscribe to public chat history");
                    return Unit.INSTANCE;
                }));
    }

    private void subscribePrivateChatHistory() {
        disposable.add(privateChatService.getChatHistoryByParticipant().subscribeOnUI(
                map -> {
                    if (map != null) {
                        Optional<Map.Entry<ParticipantsService.Participant, ObservableValue<ArrayList<ChatMessage>>>> participant =
                                map.entrySet().stream().filter(p -> p.getKey().getId().equals(remoteParticipant.getId())).findFirst();
                        if (participant.isPresent()) {
                            disposable.add(participant.get().getValue().readonly().subscribeOnUI(
                                    msgsList -> {
                                        if (adapter != null) {
                                            adapter.updateMessages(msgsList);
                                            privateChatService.clearUnreadMessagesCountByParticipant(remoteParticipant);
                                            msgList.smoothScrollToPosition(adapter.getItemCount());
                                        }
                                        return Unit.INSTANCE;
                                    },
                                    err -> {
                                        Log.e(TAG, "Unable to subscribe to private chat history");
                                        return Unit.INSTANCE;
                                    }
                            ));
                        }
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Unable to subscribe to private chat history");
                    return Unit.INSTANCE;
                }
        ));
    }

    @Override
    public void onDestroyView() {
        // always dispose references
        publicChatService = null;
        privateChatService = null;
        disposable.dispose();
        super.onDestroyView();
    }
}
