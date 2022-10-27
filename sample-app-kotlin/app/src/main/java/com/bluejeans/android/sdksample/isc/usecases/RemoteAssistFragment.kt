package com.bluejeans.android.sdksample.isc.usecases

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bjnclientcore.media.individualstream.StreamPriority
import com.bjnclientcore.media.individualstream.StreamQuality
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult
import com.bluejeans.android.sdksample.databinding.RemoteAssistBinding


class RemoteAssistFragment : Fragment() {

    private val TAG = "RemoteAssistFragment"

    private var _binding: RemoteAssistBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RemoteAssistViewModel
    private var participantConfiguration: VideoStreamConfiguration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = RemoteAssistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[RemoteAssistViewModel::class.java]

        viewModel.participantUIObservable.observe(viewLifecycleOwner) {

            if (it == null) {
                participantConfiguration?.let {
                    viewModel.detachParticipantFromView(it.participantGuid!!)
                    binding.tvParticipantName.text = ""
                    binding.remoteAssistTextureView.visibility = View.GONE
                }
                return@observe
            }

            val videoConfiguration = VideoStreamConfiguration(
                it.seamGuid,
                StreamQuality.R720p_30fps,
                StreamPriority.High
            )
            when (val result = viewModel.setVideoConfiguration(listOf(videoConfiguration))) {
                is VideoStreamConfigurationResult.Success -> {
                    participantConfiguration?.let {
                        viewModel.detachParticipantFromView(it.participantGuid!!)
                    }
                    Log.i(TAG, "VideoStream successfull")
                    participantConfiguration = videoConfiguration
                    binding.remoteAssistTextureView.visibility = View.VISIBLE
                    viewModel.attachParticipantToView(
                        videoConfiguration.participantGuid!!,
                        binding.remoteAssistTextureView
                    )
                    binding.tvParticipantName.text = it.name
                }
                is VideoStreamConfigurationResult.Failure -> {
                    Log.i(TAG, "Stream failure ${result.failureReason}")
                }

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        participantConfiguration?.let {
            viewModel.detachParticipantFromView(it.participantGuid!!)
        }
    }
}