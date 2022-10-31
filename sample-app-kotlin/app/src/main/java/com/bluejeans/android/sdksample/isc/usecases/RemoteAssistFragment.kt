package com.bluejeans.android.sdksample.isc.usecases

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
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
                    if (it.videoWidth != null && it.videoHeight!= null) {
                        setTextureViewConstraints(
                            it.videoWidth,
                            it.videoHeight,
                            binding.remoteAssistTextureView
                        )
                    }
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

    private fun setTextureViewConstraints(
        videoWidth: Int,
        videoHeight: Int,
        textureView: TextureView
    ) {
        if (videoHeight > 0 && videoWidth > 0) {
            val set = ConstraintSet()
            set.clone(binding.root)
            set.connect(
                textureView.id,
                ConstraintSet.RIGHT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.RIGHT
            )
            set.connect(
                textureView.id,
                ConstraintSet.LEFT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT
            )
            set.connect(
                textureView.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
            )
            set.connect(
                textureView.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP
            )
            val ratio = videoWidth.toFloat() / videoHeight
            Log.i(TAG, "Ratio: $ratio")
            set.setDimensionRatio(textureView.id, ratio.toString())
            set.applyTo(binding.root)
        }
    }
}