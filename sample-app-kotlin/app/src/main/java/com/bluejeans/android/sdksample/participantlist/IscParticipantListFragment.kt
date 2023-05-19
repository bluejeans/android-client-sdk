package com.bluejeans.android.sdksample.participantlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bluejeans.android.sdksample.databinding.FragmentIscParticipantListBinding
import com.bluejeans.android.sdksample.isc.IscParticipantListAdapter
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class IscParticipantListFragment(
    private val streamConfigUpdatedCallback: IscParticipantListAdapter.IStreamConfigUpdatedCallback
    ) : Fragment() {
    private val TAG = "IscParticipantListFragment"

    private lateinit var participantsViewBinding: FragmentIscParticipantListBinding
    private val participantsList = ArrayList<ParticipantsService.Participant>()
    private var iscParticipantListAdapter: IscParticipantListAdapter? = null
    private var pinnedParticipant: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        participantsViewBinding = FragmentIscParticipantListBinding.inflate(
            inflater, container, false
        )
        return participantsViewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pinnedParticipant = null
        iscParticipantListAdapter = null
    }

    override fun onResume() {
        super.onResume()
        participantsViewBinding.rvRosterIscParticipants.adapter = null
        loadIscParticipantsListAdapter()
    }

    fun updateMeetingList(rosterList: List<ParticipantsService.Participant>?) {
        if (rosterList != null) {
            iscParticipantListAdapter?.updateMeetingList(rosterList)
            participantsList.clear()
            participantsList.addAll(rosterList.filter { !it.isSelf })
        } else {
            participantsList.clear()
            iscParticipantListAdapter?.updateMeetingList(participantsList)
        }
    }

    fun setPinnedParticipant(pinnedParticipant: String?) {
        this.pinnedParticipant = pinnedParticipant
    }

    private fun addListeners() {
        participantsViewBinding.closeIscRoster.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun loadIscParticipantsListAdapter() {
        iscParticipantListAdapter = IscParticipantListAdapter(activity!!.applicationContext, streamConfigUpdatedCallback, pinnedParticipant)
        participantsViewBinding.rvRosterIscParticipants.adapter = iscParticipantListAdapter
        iscParticipantListAdapter?.updateMeetingList(participantsList)
    }
}