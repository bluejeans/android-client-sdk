/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample.participantlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.bjnclientcore.inmeeting.chat.model.ChatMessage
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.android.sdksample.databinding.FragmentChatParticipantBinding
import com.bluejeans.bluejeanssdk.meeting.MeetingService
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService
import com.bluejeans.bluejeanssdk.meeting.chat.PrivateChatService
import com.bluejeans.bluejeanssdk.meeting.chat.PublicChatService
import com.bluejeans.rxextensions.ObservableValue
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.ArrayList

class ChatParticipantFragment private constructor() : Fragment() {

    private lateinit var remoteParticipant: ParticipantsService.Participant
    private var disposable: CompositeDisposable? = null
    private var adapter: ChatMessagesAdapter? = null
    private var publicChatService: PublicChatService? = null
    private var privateChatService: PrivateChatService? = null
    private lateinit var chatParticipantBinding: FragmentChatParticipantBinding
    private val callback: DiffUtil.ItemCallback<ChatMessage> =
        object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.id == newItem.id
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        chatParticipantBinding = FragmentChatParticipantBinding.inflate(inflater)
        return chatParticipantBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatParticipantBinding.messagesView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        disposable = CompositeDisposable()
        subscribeForChatServiceStates()
        addSendMessageListener()
    }

    private fun subscribeForChatServiceStates() {
        val meetingService: MeetingService = SampleApplication.blueJeansSDK.meetingService
        if (remoteParticipant.id == EVERYONE) {
            disposable?.add(meetingService.publicChatService.chatServiceState.subscribeOnUI({ state: MeetingService.MeetingChatServiceStatus? ->
                if (state == null) {
                    publicChatService = null
                } else {
                    if (publicChatService == null) {
                        publicChatService = meetingService.publicChatService
                        attachPublicAdapter()
                        loadMsgsFromHistory()
                        subscribePublicChatHistory()
                    }
                }
            }
            ) { err: Throwable? ->
                Log.e(TAG, "Unable to subscribe for public chat service ${err?.message}")
            })
        } else {
            disposable?.add(meetingService.privateChatService.chatServiceState.subscribeOnUI({ state: MeetingService.MeetingChatServiceStatus? ->
                if (state == null) {
                    privateChatService = null
                } else {
                    if (privateChatService == null) {
                        privateChatService = meetingService.privateChatService
                        attachPrivateAdapter()
                        loadMsgsFromHistory()
                        subscribePrivateChatHistory()
                    }
                }
            }
            ) { err: Throwable? ->
                Log.e(TAG, "Unable to subscribe for private chat service ${err?.message}")
            })
        }
    }

    private fun addSendMessageListener() {
        chatParticipantBinding.ivSendMsg.setOnClickListener {
            val message = chatParticipantBinding.etMessage.text.toString()
            if (remoteParticipant.id == EVERYONE) {
                if (publicChatService != null &&
                    publicChatService?.chatServiceState?.value ==
                    MeetingService.MeetingChatServiceStatus.Active
                ) {
                    if (message.isNotEmpty()) {
                        publicChatService?.sendMessage(message)
                        chatParticipantBinding.etMessage.setText("")
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Unable to send message at the moment",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                if (privateChatService != null &&
                    privateChatService?.chatServiceState?.value ==
                    MeetingService.MeetingChatServiceStatus.Active
                ) {
                    if (message.isNotEmpty()) {
                        privateChatService?.sendMessage(message, remoteParticipant)
                        chatParticipantBinding.etMessage.setText("")
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Unable to send message at the moment",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadMsgsFromHistory() {
        if (remoteParticipant.id == EVERYONE) {
            if (publicChatService?.chatHistory?.value != null) {
                adapter?.updateMessages(publicChatService?.chatHistory?.value!!)
            }
        } else {
            if (privateChatService?.chatHistoryByParticipant?.value?.containsKey(remoteParticipant) == true) {
                if (privateChatService?.chatHistoryByParticipant?.value != null && privateChatService?.chatHistoryByParticipant?.value?.containsKey(
                        remoteParticipant
                    ) == true
                ) {
                    adapter?.updateMessages(
                        privateChatService?.chatHistoryByParticipant?.value?.get(
                            remoteParticipant
                        )?.value!!
                    )
                }
            }
        }
    }

    private fun attachPublicAdapter() {
        adapter = ChatMessagesAdapter(true, callback)
        chatParticipantBinding.messagesView.adapter = adapter
    }

    private fun attachPrivateAdapter() {
        adapter = ChatMessagesAdapter(false, callback)
        chatParticipantBinding.messagesView.adapter = adapter
    }

    private fun subscribePublicChatHistory() {
        disposable?.add(publicChatService?.chatHistory?.subscribeOnUI({ list: List<ChatMessage>? ->
            if (adapter != null && list != null) {
                adapter?.updateMessages(list)
                publicChatService?.clearUnreadMessagesCount()
                chatParticipantBinding.messagesView.smoothScrollToPosition(adapter!!.itemCount)
            }
        }
        ) { err: Throwable? ->
            Log.e(TAG, "Unable to subscribe to public chat history ${err?.message}")
        })
    }

    private fun subscribePrivateChatHistory() {
        disposable?.add(privateChatService?.chatHistoryByParticipant?.subscribeOnUI(
            { map: Map<ParticipantsService.Participant, ObservableValue<ArrayList<ChatMessage>>>? ->
                val participant = map?.entries?.find { entry -> entry.key.id == remoteParticipant.id }
                if (map != null && participant != null) {
                    disposable?.add(participant.value?.subscribeOnUI(
                        { msgsList: ArrayList<ChatMessage> ->
                            if (adapter != null) {
                                adapter?.updateMessages(msgsList)
                                privateChatService?.clearUnreadMessagesCountByParticipant(
                                    remoteParticipant
                                )
                                chatParticipantBinding.messagesView.smoothScrollToPosition(
                                    adapter!!.itemCount
                                )
                            }
                        }
                    ) { err: Throwable? ->
                        Log.e(TAG, "Unable to subscribe to private chat history ${err?.message}")
                    })
                }
            }
        ) { err: Throwable? ->
            Log.e(TAG, "Unable to subscribe to private chat history ${err?.message}")
        })
    }

    override fun onDestroyView() {
        // always dispose references
        publicChatService = null
        privateChatService = null
        disposable?.dispose()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(participant: ParticipantsService.Participant): ChatParticipantFragment {
            val fragment = ChatParticipantFragment()
            fragment.remoteParticipant = participant
            return fragment
        }

        private const val TAG = "ChatParticipantFragment"
    }
}