package com.bluejeans.android.sdksample.isc

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bjnclientcore.media.individualstream.StreamPriority
import com.bjnclientcore.media.individualstream.StreamQuality
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult
import com.bluejeans.android.sdksample.R
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.android.sdksample.databinding.RowIscParticipantBinding
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService

class IscParticipantListAdapter(
    private val context: Context,
    private val configUpdatedCallback: IStreamConfigUpdatedCallback,
    private val pinnedParticipant: String?
) :
    RecyclerView.Adapter<IscParticipantListAdapter.IscRosterViewHolder>() {
    private val TAG = "IscParticipantListAdapter"
    private val participantsList = ArrayList<ParticipantsService.Participant>()
    private val videoStreamConfigurations = mutableListOf<VideoStreamConfiguration>()
    private val priorities = arrayOf("Default", "High", "Medium", "Low")
    private val resolutions = arrayOf(
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
    )

    private val videoStreamService =
        SampleApplication.blueJeansSDK.meetingService.videoStreamService

    private var currentParticipant: ParticipantsService.Participant? = null

    private val spPriorityAdapter = ArrayAdapter(
        context,
        R.layout.row_isc_config,
        priorities
    )
    private val spResolutionAdapter = ArrayAdapter(
        context,
        R.layout.row_isc_config,
        resolutions
    )

    interface IStreamConfigUpdatedCallback {
        fun onStreamConfigUpdated(
            participantGuid: String,
            streamQuality: StreamQuality,
            streamPriority: StreamPriority
        )

        fun pinParticipant(participantId: String, isPinned: Boolean)
    }

    inner class IscRosterViewHolder(private val bindingView: RowIscParticipantBinding) :
        RecyclerView.ViewHolder(bindingView.root) {
        fun bind(participant: ParticipantsService.Participant) {
            bindingView.tvIscParticipantName.text = participant.name
            bindingView.spParticipantPriority.adapter = spPriorityAdapter
            bindingView.spParticipantResolution.adapter = spResolutionAdapter

            videoStreamConfigurations.clear()
            videoStreamConfigurations.addAll(SampleApplication.blueJeansSDK.meetingService.videoStreamService.videoStreamConfigurations)

            bindingView.spParticipantPriority.setSelection(0, false)
            bindingView.spParticipantResolution.setSelection(0, false)
            bindingView.swPinParticipant.isChecked = pinnedParticipant == participant.id

            val participantConfiguration =
                videoStreamService.videoStreamConfigurations.find { participant.id == it.participantGuid }

            participantConfiguration?.let {
                it.streamPriority?.ordinal?.let { priorityIndex ->
                    bindingView.spParticipantPriority.setSelection(
                        priorities.indexOf(
                            priorities[priorityIndex]
                        ), false
                    )
                }

                bindingView.spParticipantResolution.setSelection(
                    resolutions.indexOf(
                        resolutions[it.streamQuality.ordinal]
                    ), false
                )
            }

            bindingView.swPinParticipant.setOnCheckedChangeListener { compoundButton, b ->
                configUpdatedCallback.pinParticipant(participant.id, compoundButton.isChecked)
            }

            bindingView.btnApplyConfig.setOnClickListener {
                participantConfiguration?.let {
                    if (it.streamPriority != bindingView.spParticipantPriority.selectedItem &&
                        it.streamQuality == getStreamQualityFromString(bindingView.spParticipantResolution.selectedItem.toString())) {
                        Toast.makeText(context, context.resources.getString(R.string.priority_error), Toast.LENGTH_LONG).show()
                        it.streamPriority?.ordinal?.let { priorityIndex ->
                            bindingView.spParticipantPriority.setSelection(
                                priorities.indexOf(
                                    priorities[priorityIndex]
                                ), false
                            )
                        }
                    } else {
                        Log.i(
                            TAG, "Setting resolution to: " +
                                    "${bindingView.spParticipantResolution.selectedItem} and " +
                                    "priority to: ${bindingView.spParticipantPriority.selectedItem} for P ${participant.name} (${participant?.id})"
                        )
                        applyVideoStreamConfiguration(
                            participant.id,
                            getStreamQualityFromString(bindingView.spParticipantResolution.selectedItem.toString()),
                            getStreamPriorityFromString(bindingView.spParticipantPriority.selectedItem.toString())
                        )
                    }
                }
            }
        }

        private fun applyVideoStreamConfiguration(
            participantGuid: String,
            quality: StreamQuality,
            priority: StreamPriority
        ) {
            val currentStreamConfigurations =
                videoStreamService.videoStreamConfigurations
            val updatedConfiguration = mutableListOf<VideoStreamConfiguration>()
            val newVideoStreamConfig = VideoStreamConfiguration(
                participantGuid,
                quality,
                priority
            )
            currentStreamConfigurations.forEach {
                if (it.participantGuid == newVideoStreamConfig.participantGuid) {
                    updatedConfiguration.add(newVideoStreamConfig)
                } else {
                    updatedConfiguration.add(it)
                }
            }


            Log.i(
                TAG,
                "stream config: ${newVideoStreamConfig.streamQuality.name}, ${newVideoStreamConfig.streamPriority?.name} " +
                        "for participant ${newVideoStreamConfig.participantGuid}"
            )

            val result =
                videoStreamService.setVideoStreamConfiguration(
                    updatedConfiguration
                )

            when (result) {
                is VideoStreamConfigurationResult.Success -> {
                    Log.i(
                        TAG,
                        "Stream config applied successfully"
                    )
                    Toast.makeText(context, "Stream config requested successfully", Toast.LENGTH_LONG).show()
                    videoStreamConfigurations.clear()
                    videoStreamConfigurations.addAll(videoStreamService.videoStreamConfigurations)

                    configUpdatedCallback.onStreamConfigUpdated(participantGuid, quality, priority)
                }
                is VideoStreamConfigurationResult.Failure -> Log.i(
                    TAG,
                    "Failed to apply config $result"
                )
                else -> Log.e(TAG, "Some error caused while applying config: $result")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IscRosterViewHolder {
        val itemBinding =
            RowIscParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IscRosterViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: IscRosterViewHolder, position: Int) {
        currentParticipant = this.participantsList[position]
        holder.bind(this.participantsList[position])
    }

    override fun getItemCount(): Int {
        return participantsList.size
    }

    fun updateMeetingList(participantsList: List<ParticipantsService.Participant>) {
        this.participantsList.clear()
        this.participantsList.addAll(participantsList.filter { !it.isSelf })
        notifyDataSetChanged()
    }
}