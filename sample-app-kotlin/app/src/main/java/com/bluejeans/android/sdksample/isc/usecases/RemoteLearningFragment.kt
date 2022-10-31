package com.bluejeans.android.sdksample.isc.usecases

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bjnclientcore.media.individualstream.StreamPriority
import com.bjnclientcore.media.individualstream.StreamQuality
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult
import com.bjnclientcore.utils.extensions.getInitials
import com.bluejeans.android.sdksample.databinding.FragmentRemoteLearningBinding

class RemoteLearningFragment : Fragment() {

    private var _binding: FragmentRemoteLearningBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RemoteLearningViewModel

    private var moderator: RemoteLearningParticipant? = null
    private val students = mutableListOf<RemoteLearningParticipant>()
    private val studentTextureViews = mutableListOf<TextureView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRemoteLearningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[RemoteLearningViewModel::class.java]

        studentTextureViews.addAll(
            listOf(
                binding.studentOne.participantTextureView,
                binding.studentTwo.participantTextureView,
                binding.studentThree.participantTextureView,
                binding.studentFour.participantTextureView,
                binding.studentFive.participantTextureView,
                binding.studentSix.participantTextureView
            )
        )

        viewModel.participantsObservable.observe(viewLifecycleOwner) { studentsList ->
            studentTextureViews.forEach {
                it.visibility = View.GONE
            }
            moderator = null
            students.clear()
            students.addAll(studentsList)

            val configurationList = mutableListOf<VideoStreamConfiguration>()
            for (remoteLearningParticipant in students.distinctBy { it.participantId }) {
                if (remoteLearningParticipant.isModerator) {
                    moderator = remoteLearningParticipant
                    configurationList.add(
                        VideoStreamConfiguration(
                            remoteLearningParticipant.participantId,
                            StreamQuality.R720p_30fps,
                            StreamPriority.High
                        )
                    )
                } else {
                    configurationList.add(
                        VideoStreamConfiguration(
                            remoteLearningParticipant.participantId,
                            StreamQuality.R90p_15fps,
                            StreamPriority.Low
                        )
                    )
                }
            }

            when (val result = viewModel.setVideoConfiguration(configurationList)) {
                is VideoStreamConfigurationResult.Success -> {
                    updateModeratorUI()
                    updateStudentsUI()
                }
                else -> Log.e(TAG, "Failed to set stream configuration: $result")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        moderator?.let { viewModel.detachParticipantFromView(it.participantId) }
        students.forEach {
            viewModel.detachParticipantFromView(it.participantId)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            configurePortraitView()
        } else configureLandscapeView()
    }

    private fun updateModeratorUI() {
        moderator?.let {
            binding.layoutMainStage.tvParticipantName.text = it.name

            if (!it.isVideo) {
                Log.i(TAG, "Detaching moderator video")
                viewModel.detachParticipantFromView(it.participantId)

                binding.layoutMainStage.participantTextureView.visibility = View.GONE
                binding.layoutMainStage.cnhAudioOnly.visibility = View.VISIBLE
                binding.layoutMainStage.cnhAudioOnly.setText(it.name.getInitials())
            } else {
                binding.layoutMainStage.participantTextureView.visibility = View.VISIBLE
                binding.layoutMainStage.cnhAudioOnly.visibility = View.GONE
                viewModel.attachParticipantToView(
                    it.participantId,
                    binding.layoutMainStage.participantTextureView
                )
                setModeratorTextureViewConstraints()
            }
        } ?: kotlin.run {
            binding.layoutMainStage.tvParticipantName.text = ""
            binding.layoutMainStage.cnhAudioOnly.visibility = View.GONE
        }
    }

    private fun updateStudentsUI() {
        students.filter { !it.isModerator }.forEachIndexed { index, it ->
            viewModel.detachParticipantFromView(it.participantId)
            if (it.isVideo) {
                studentTextureViews[index].visibility = View.VISIBLE
                attachStudentStream(it, studentTextureViews[index])
                if (it.width != null && it.height != null) {
                    setStudentTextureViewConstraints(
                        it.width,
                        it.height,
                        studentTextureViews[index]
                    )
                }
            }
        }
    }

    private fun attachStudentStream(student: RemoteLearningParticipant, textureView: TextureView) {
        viewModel.attachParticipantToView(student.participantId, textureView)
    }

    private fun setModeratorTextureViewConstraints() {
        val set = ConstraintSet()
        val textureView = binding.layoutMainStage.participantTextureView
        set.clone(binding.layoutMainStage.root)
        set.connect(
            textureView.id,
            ConstraintSet.RIGHT,
            ConstraintSet.PARENT_ID,
            ConstraintSet.RIGHT
        )
        set.connect(textureView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        set.connect(
            textureView.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM
        )
        set.connect(textureView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

        moderator?.let {
            if (it.width != null && it.height != null) {
                val ratio = (it.width.toFloat() / it.height)
                Log.i(TAG, "Ratio: $ratio")
                set.setDimensionRatio(textureView.id, ratio.toString())
            }
        }

        set.applyTo(binding.layoutMainStage.root)
    }

    private fun configureLandscapeView() {
        binding.scrollView.visibility = View.GONE
        setModeratorTextureViewConstraints()
    }

    private fun configurePortraitView() {
        binding.scrollView.visibility = View.VISIBLE
        setModeratorTextureViewConstraints()
    }

    private fun setStudentTextureViewConstraints(
        videoWidth: Int,
        videoHeight: Int,
        textureView: TextureView
    ) {
        val set = ConstraintSet()
        set.clone(binding.layoutMainStage.root)
        set.connect(
            textureView.id,
            ConstraintSet.RIGHT,
            ConstraintSet.PARENT_ID,
            ConstraintSet.RIGHT
        )
        set.connect(textureView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        set.connect(
            textureView.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM
        )
        set.connect(textureView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        if (moderator != null) {
            if (videoWidth > 0 && videoHeight > 0) {
                val ratio = videoWidth.toFloat() / videoHeight
                Log.i(TAG, "Ratio: $ratio")
                set.setDimensionRatio(textureView.id, ratio.toString())
            }
        }
        set.applyTo(textureView.parent as ConstraintLayout)
    }

    companion object {
        private val TAG = "RemoteLearningFragment"
    }
}