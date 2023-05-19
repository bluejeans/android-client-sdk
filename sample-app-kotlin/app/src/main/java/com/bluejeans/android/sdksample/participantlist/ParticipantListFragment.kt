/*
 * Copyright (c) 2020 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample.participantlist

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import com.bjnclientcore.ui.util.extensions.gone
import com.bjnclientcore.ui.util.extensions.visible
import com.bluejeans.android.sdksample.R
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.android.sdksample.databinding.FragmentParticipantViewBinding
import com.bluejeans.bluejeanssdk.meeting.MeetingService
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService
import com.bluejeans.bluejeanssdk.meeting.chat.PrivateChatService
import com.bluejeans.bluejeanssdk.meeting.chat.PublicChatService
import com.bluejeans.rxextensions.ObservableComputed
import com.bluejeans.rxextensions.ObservableValue
import com.bluejeans.rxextensions.utils.Optional
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.disposables.CompositeDisposable

const val EVERYONE = "Everyone"

class ParticipantListFragment : BottomSheetDialogFragment(), ParticipantChatItemListener {

    sealed class FragmentView {
        object CHAT_PARTICIPANTS : FragmentView()
        object PARTICIPANT_LIST : FragmentView()
        object CHAT_VIEW : FragmentView()
    }

    private val participantsList = ArrayList<ParticipantsService.Participant>()
    private var participantListAdapter: ParticipantListAdapter? = null
    private lateinit var participantsViewBinding: FragmentParticipantViewBinding
    private var chatDisposable: CompositeDisposable? = null
    private var participantDisposable: CompositeDisposable? = null
    private var countDisposable = CompositeDisposable()
    private var selectedView: FragmentView = FragmentView.PARTICIPANT_LIST
    private var totalCount = 0
    private var publicChatService: PublicChatService? = null
    private var privateChatService: PrivateChatService? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        participantsViewBinding = FragmentParticipantViewBinding.inflate(
            inflater,
            container, false
        )
        return participantsViewBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        participantsViewBinding.rvRosterParticipants.addItemDecoration(
            DividerItemDecoration(
                activity,
                DividerItemDecoration.VERTICAL
            )
        )
        participantsViewBinding.tvUnreadChatCount.gone()
        loadParticipantListAdapter()
        participantListAdapter?.updateMeetingList(participantsList)
        addClickListeners()
        participantDisposable = CompositeDisposable()
        subscribeForChatServices()
    }

    fun updateMeetingList(rosterList: List<ParticipantsService.Participant>?) {
        if (rosterList != null) {
            participantListAdapter?.updateMeetingList(rosterList)
            participantsList.clear()
            participantsList.addAll(rosterList)
        } else {
            participantsList.clear()
            participantListAdapter?.updateMeetingList(participantsList)
        }
    }

    override fun onDestroyView() {
        participantListAdapter = null
        chatDisposable?.dispose()
        countDisposable.dispose()
        // important to dispose the references or avoid using them
        publicChatService = null
        privateChatService = null
        selectedView = FragmentView.PARTICIPANT_LIST
        super.onDestroyView()
    }

    override fun onMessageCountChange(
        participant: ParticipantsService.Participant,
        mUnreadCount: TextView
    ) {
        if (participant.id == EVERYONE) {
            subscribeToPublicChatForCount(mUnreadCount)
        } else {
            subscribeToPrivateChatForParticipantCount(participant, mUnreadCount)
        }
    }

    override fun onParticipantClick(participant: ParticipantsService.Participant) {
        if (participant.id == EVERYONE && publicChatService != null) {
            publicChatService?.clearUnreadMessagesCount()
            setTitle(EVERYONE)
        } else if (privateChatService != null) {
            privateChatService?.clearUnreadMessagesCountByParticipant(participant)
            setTitle(participant.name)
        }
        participantsViewBinding.rvRosterParticipants.gone()
        childFragmentManager.beginTransaction()
            .replace(R.id.chatFragHolder, ChatParticipantFragment.newInstance(participant))
            .addToBackStack("ParticipantChat")
            .commit()
        participantsViewBinding.ivChatBubble.gone()
        participantsViewBinding.ivPeople.gone()
        participantsViewBinding.closeRoster.setImageResource(R.drawable.ic_back_icon)
        selectedView = FragmentView.CHAT_VIEW
    }


    private fun loadParticipantListAdapter() {
        participantListAdapter = ParticipantListAdapter()
        participantsViewBinding.rvRosterParticipants.adapter = participantListAdapter
        participantListAdapter?.updateMeetingList(participantsList)
        subscribeForChatServicesMessageCount()
    }

    private fun subscribeForChatServicesMessageCount() {
        if (publicChatService != null && privateChatService != null) {
            if (!countDisposable.isDisposed) {
                countDisposable.dispose()
            }
            countDisposable = CompositeDisposable()
            val countObserver: ObservableValue<Int> =
                ObservableComputed.create(publicChatService!!.unreadMessagesCount,
                    privateChatService!!.unreadMessagesCount,
                    countDisposable,
                    true,
                    calculator =
                    { integer: Optional<Int>, integer2: Optional<Int> ->
                        if (integer.value == null && integer2.value == null) return@create 0
                        else if (integer.value != null && integer2.value == null) return@create integer.value!!
                        else if (integer.value == null && integer2.value != null) return@create integer2.value!!
                        else return@create integer.value!! + integer2.value!!
                    })
            countDisposable.add(countObserver.subscribeOnUI({ c: Int ->
                totalCount = c
                handleUnreadCountDisplay()
            }) { err: Throwable? ->
                Log.e(TAG, "Unable to get total msg count ${err?.message}")
            })
        } else {
            Log.e(
                TAG,
                "Value of public chat service = $publicChatService and private chat service$privateChatService"
            )
        }
    }

    private fun handleUnreadCountDisplay() {
        if (this.isVisible && selectedView == FragmentView.PARTICIPANT_LIST) {
            if (totalCount > 0) {
                val countString = "" + totalCount
                participantsViewBinding.tvUnreadChatCount.text = countString
                participantsViewBinding.tvUnreadChatCount.visible()
            } else {
                participantsViewBinding.tvUnreadChatCount.gone()
            }
        }
    }

    private fun addClickListeners() {
        participantsViewBinding.closeRoster.setOnClickListener {
            if (selectedView == FragmentView.CHAT_VIEW) {
                handleChatViewBack()
            } else {
                activity?.onBackPressed()
            }
        }
        participantsViewBinding.ivChatBubble.setOnClickListener { v: View ->
            if (publicChatService != null && selectedView == FragmentView.PARTICIPANT_LIST) {
                if (chatDisposable == null || chatDisposable?.isDisposed == true) {
                    chatDisposable = CompositeDisposable()
                }
                selectedView = FragmentView.CHAT_PARTICIPANTS
                loadChatAdapter()
                setTitle(getString(R.string.chat))
                participantsViewBinding.ivChatBubble.gone()
                participantsViewBinding.ivPeople.visible()
                participantsViewBinding.tvUnreadChatCount.gone()
                totalCount = 0
                handleUnreadCountDisplay()
            } else if (selectedView != FragmentView.CHAT_PARTICIPANTS && publicChatService == null) {
                Toast.makeText(context, "Chat Unavailable", Toast.LENGTH_SHORT).show()
            }
        }

        participantsViewBinding.ivPeople.setOnClickListener {
            chatDisposable?.dispose()
            selectedView = FragmentView.PARTICIPANT_LIST
            loadParticipantListAdapter()
            setTitle(getString(R.string.people))
            participantsViewBinding.ivChatBubble.visible()
            participantsViewBinding.ivPeople.gone()
            participantsViewBinding.tvUnreadChatCount.visible()
        }

    }

    private fun loadChatAdapter() {
        participantListAdapter = ParticipantListAdapter(true, this)
        participantsViewBinding.rvRosterParticipants.adapter = participantListAdapter
        if (privateChatService != null) {
            chatDisposable?.add(privateChatService?.eligibleParticipants?.subscribeOnUI({ list: List<ParticipantsService.Participant>? ->
                if (list != null) {
                    Log.i(TAG, "Setting eligible list size is " + list.size)
                    // removing self
                    participantListAdapter?.updateMeetingList(list)
                }
            }) { err: Throwable? ->
                Log.e(TAG, "Unable to subscribe to private chat service ${err?.message}")
            })
        } else {
            Log.i(TAG, "Private chat is null")
            participantListAdapter?.updateMeetingList(arrayListOf())
        }
    }

    private fun handleChatViewBack() {
        childFragmentManager.popBackStack()
        with(participantsViewBinding) {
            ivChatBubble.gone()
            ivPeople.visible()
            tvUnreadChatCount.gone()
            rvRosterParticipants.visible()
            closeRoster.setImageResource(R.drawable.ic_close_dark_blue)
        }
        setTitle(getString(R.string.chat))
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.view?.windowToken, 0)
        selectedView = FragmentView.CHAT_PARTICIPANTS
    }

    private fun setTitle(title: String) {
        participantsViewBinding.tvRosterHeading.text = title
    }

    private fun subscribeForChatServices() {
        val meetingService: MeetingService = SampleApplication.blueJeansSDK.meetingService
        participantDisposable?.add(meetingService.publicChatService.chatServiceState.subscribeOnUI({ obj: MeetingService.MeetingChatServiceStatus? ->
            if (obj != null) {
                publicChatService = meetingService.publicChatService
                subscribeForChatServicesMessageCount()
            } else {
                publicChatService = null
            }
        }) { err: Throwable? ->
            Log.e(TAG, "Unable to subscribe for public chat service ${err?.message}")
        })
        participantDisposable?.add(meetingService.privateChatService.chatServiceState.subscribeOnUI(
            { obj: MeetingService.MeetingChatServiceStatus? ->
                if (obj != null) {
                    privateChatService = meetingService.privateChatService
                    subscribeForChatServicesMessageCount()
                } else {
                    privateChatService = null
                }
            }) { err: Throwable? ->
            Log.e(TAG, "Unable to subscribe for private chat service ${err?.message}")
        })
    }

    private fun subscribeToPublicChatForCount(unreadCount: TextView) {
        if (publicChatService != null) {
            chatDisposable?.add(publicChatService?.unreadMessagesCount?.subscribeOnUI(
                { count: Int? ->
                    if (count != null && count > 0) {
                        unreadCount.visibility = View.VISIBLE
                        val countString = " $count"
                        unreadCount.text = countString
                    } else {
                        unreadCount.visibility = View.GONE
                    }
                }
            ) { err: Throwable? ->
                Log.e(TAG, "Error subscribing to public chat count ${err?.message}")
                unreadCount.visibility = View.GONE
            })
        }
    }

    private fun subscribeToPrivateChatForParticipantCount(
        participant: ParticipantsService.Participant,
        unreadCount: TextView
    ) {
        if (privateChatService != null) {
            chatDisposable?.add(privateChatService?.unreadCountForParticipant?.subscribeOnUI(
                { map: Map<ParticipantsService.Participant, ObservableValue<Int>>? ->
                    if (map != null && map.containsKey(participant)) {
                        chatDisposable?.add(map[participant]?.subscribeOnUI({ count: Int ->
                            if (count > 0) {
                                unreadCount.visibility = View.VISIBLE
                                val countString = " $count"
                                unreadCount.text = countString
                            } else {
                                unreadCount.visibility = View.GONE
                            }
                        }) { err: Throwable? ->
                            Log.e(TAG, "Error subscribing to private chat count ${err?.message}")
                            unreadCount.visibility = View.GONE
                        })
                    }
                }
            ) { err: Throwable? ->
                Log.e(TAG, "Error subscribing to private chat participant count map ${err?.message}")
                unreadCount.visibility = View.GONE
            })
        }
    }

    companion object {
        private const val TAG = "ParticipantListFragment"
    }
}