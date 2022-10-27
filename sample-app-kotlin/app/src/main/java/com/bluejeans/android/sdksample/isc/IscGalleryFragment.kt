package com.bluejeans.android.sdksample.isc

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bjnclientcore.media.individualstream.StreamPriority
import com.bjnclientcore.media.individualstream.StreamQuality
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult
import com.bjnclientcore.media.individualstream.VideoStreamStyle
import com.bjnclientcore.ui.util.extensions.dpToPx
import com.bjnclientcore.utils.extensions.getInitials
import com.bluejeans.android.sdksample.R
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.android.sdksample.databinding.FragmentIscGalleryBinding
import com.bluejeans.rxextensions.ObservableValueWithOptional
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber

class IscGalleryFragment : Fragment() {

    private val TAG = "IscGalleryFragment"

    private var _binding: FragmentIscGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: IscGalleryViewModel

    private var mainStageParticipant: ParticipantUI? = null

    private val participantViewManagerMap = mutableMapOf<String, ParticipantUIView>()
    private val compositeDisposable = CompositeDisposable()
    private val participantsService =
        SampleApplication.blueJeansSDK.meetingService.participantsService
    private val participantsConfigMap = mutableMapOf<String, VideoStreamConfiguration>()
    private val iscParticipants = mutableListOf<ParticipantUI>()

    private var pinnedParticipant: String? = null

    private val constraintSet = ConstraintSet()

    private var resolutionDisposable: CompositeDisposable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIscGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        constraintSet.clone(binding.iscRoot)
        setMainStageConstraints()
        viewModel = ViewModelProvider(this)[IscGalleryViewModel::class.java]


        viewModel.participantUIObservable.observe(viewLifecycleOwner) { participantUi ->
            Timber.tag(TAG).i("Participant list size updated: ${participantUi.size}")

            if (participantUi.isEmpty()) {
                // all participants have left the meeting
                // explicitly detach stream
                mainStageParticipant?.let {
                    viewModel.detachParticipantFromView(it.seamGuid)
                    resetUI()
                }
                return@observe
            }

            val streamConfiguration = participantUi.distinctBy { it.seamGuid }.map {
                VideoStreamConfiguration(
                    it.seamGuid,
                    participantsConfigMap[it.seamGuid]?.streamQuality
                        ?: kotlin.run { StreamQuality.R360p_30fps },
                    participantsConfigMap[it.seamGuid]?.streamPriority
                        ?: kotlin.run { StreamPriority.Medium }
                )
            }

            when (val result = viewModel.setVideoConfiguration(streamConfiguration)) {
                is VideoStreamConfigurationResult.Success -> {
                    streamConfiguration.forEach { config ->
                        config.participantGuid?.let { guid ->
                            participantsConfigMap[guid] = config
                        }
                    }

                    iscParticipants.clear()
                    iscParticipants.addAll(participantUi)
                    updateUI(participantUi)
                }
                is VideoStreamConfigurationResult.Failure -> {
                    Timber.tag(TAG).i("Stream failure ${result.failureReason}")
                }
            }
        }

        subscribeToRosterUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainStageParticipant?.let { viewModel.detachParticipantFromView(it.seamGuid) }
        resetUI()
        _binding = null
        compositeDisposable.dispose()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> configurePortraitView()
            else -> configureLandscapeView()
        }
    }

    private fun showMainStage() {
        binding.layoutMainStage?.let { mainStage ->
            mainStageParticipant?.let {
                if (it.isVideo) {
                    mainStage.cnhAudioOnly.visibility = View.GONE
                    mainStage.participantTextureView.visibility = View.VISIBLE
                    Log.i(
                        TAG,
                        "Requested quality for main stage participant is ${
                            getStreamQualityName(participantsConfigMap[it.seamGuid]?.streamQuality)
                        }"
                    )
                    updateResolutionTextView(it.videoResolution)
                    updatePriorityTextView(participantsConfigMap[it.seamGuid]?.streamPriority)

                    viewModel.attachParticipantToView(it.seamGuid, mainStage.participantTextureView)
                    Log.i(TAG, "main stage name: ${mainStageParticipant?.name}, width: ${mainStageParticipant?.videoWidth}, " +
                            "height: ${mainStageParticipant?.videoHeight}")
                    setMainStageTextureViewConstraints()
                } else {
                    mainStage.cnhAudioOnly.visibility = View.VISIBLE
                    mainStage.participantTextureView.visibility = View.GONE
                    mainStage.tvPriority.text = ""

                    mainStage.cnhAudioOnly.setText(it.name.getInitials())
                }

                mainStage.tvParticipantName.text = it.name
            }

        }
    }

    private fun setMainStageConstraints() {
        binding.layoutMainStage?.let {
            val set = ConstraintSet()
            set.clone(it.root)
            set.connect(
                it.vTranslucentBar.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
            )
            set.applyTo(it.root)

        }
    }

    private fun updateUI(participantUi: List<ParticipantUI>) {
        resolutionDisposable?.dispose()
        binding.layoutMainStage.let {
            binding.rvIscParticipantTilesList.let { rv ->
                mainStageParticipant?.let { it1 -> viewModel.detachParticipantFromView(it1.seamGuid) }
                participantViewManagerMap.forEach { (_, value) ->
                    value.unbind()
                }
                participantViewManagerMap.clear()
                if (pinnedParticipant == null) {
                    mainStageParticipant = participantUi[0]
                } else {
                    val currentPinnedParticipant =
                        participantUi.find { it.seamGuid == pinnedParticipant }
                    currentPinnedParticipant?.let { p ->
                        mainStageParticipant = p
                    } ?: kotlin.run { mainStageParticipant = participantUi[0] }
                }
                showMainStage()

                val filmStripParticipants = if (pinnedParticipant == null) {
                    participantUi.takeLast(participantUi.size - 1)
                } else {
                    participantUi.filter { it.seamGuid != mainStageParticipant?.seamGuid }
                }

                addParticipantToStrip(filmStripParticipants)
            }
        }
    }

    private fun resetUI() {
        participantViewManagerMap.forEach { (_, value) ->
            value.unbind()
        }
        participantViewManagerMap.clear()
        binding.stripContainer?.removeAllViews()
        resolutionDisposable?.dispose()
        binding.layoutMainStage.cnhAudioOnly.visibility = View.GONE
        binding.layoutMainStage.participantTextureView.visibility = View.GONE
        binding.layoutMainStage.tvParticipantName.text = ""
        binding.layoutMainStage.tvParticipantResolution.text = ""
        binding.layoutMainStage.tvPriority.text = ""
    }

    private fun updateResolutionTextView(videoResolution: ObservableValueWithOptional<Size>?) {
        resolutionDisposable = CompositeDisposable()
        videoResolution?.rxObservable
            ?.subscribeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ resolution ->
                resolution.value?.let {
                    Log.i(
                        TAG,
                        "Received Resolution ${it.width}x${it.height} received for main stage participant"
                    )
                    binding.layoutMainStage.let { mainStage ->
                        mainStage.tvParticipantResolution.text = "${it.width}x${it.height}"
                    }
                } ?: kotlin.run {
                    Log.i(TAG, "Resolution is null for main stage participant")
                    binding.layoutMainStage.let { mainStage ->
                        mainStage.tvParticipantResolution.text = "0x0"
                    }
                }
            }, {
                Log.e(TAG, "Error in getting resolution: ${it.stackTraceToString()}")
            })?.let { resolutionDisposable!!.add(it) }
    }

    private fun updatePriorityTextView(priority: StreamPriority?) {
        binding.layoutMainStage.tvPriority?.text = getPriorityName(priority)
    }

    private fun subscribeToRosterUpdates() {
        compositeDisposable.add(
            participantsService.participants.rxObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.tag(TAG).i("received roster updates with size ${it.value?.size}")
                    it.value?.let { participants ->
                        mainStageParticipant?.let { mainStage ->
                            val p = participants.find { it.id == mainStage.seamGuid }
                            binding.layoutMainStage.tvParticipantName?.text = p?.name
                        }
                    }
                }, {
                    Timber.tag(TAG).i("Error while getting roster updates: ${it.message}")
                })
        )
    }

    fun updateStreamConfigurations(
        participantGuid: String,
        streamQuality: StreamQuality,
        streamPriority: StreamPriority
    ) {
        participantsConfigMap[participantGuid] =
            VideoStreamConfiguration(participantGuid, streamQuality, streamPriority)
        mainStageParticipant?.let {
            if (participantGuid == it.seamGuid) {
                Log.i(
                    TAG,
                    "Main stage participant resolution updated to ${
                        getStreamQualityName(streamQuality)
                    }"
                )
                updatePriorityTextView(streamPriority)
                return
            }
        }

        participantViewManagerMap.forEach { (key, value) ->
            if (key == participantGuid) {
                value.updateResolutionAndPriority(streamPriority, streamQuality)
                return@forEach
            }
        }
    }

    fun pinParticipant(participantId: String, isPinned: Boolean) {
        pinnedParticipant = if (isPinned) {
            mainStageParticipant?.let {
                val result = viewModel.detachParticipantFromView(it.seamGuid)
                Log.i(TAG, "Detachment result: $result")
            }
            participantId
        } else {
            null
        }

        updateUI(iscParticipants)
    }

    fun setVideoStreamStyle() {
        setMainStageTextureViewConstraints()
        for (i in 0 until binding.stripContainer.childCount) {
            val tileStripParticipant = binding.stripContainer.getChildAt(i) as ConstraintLayout
            setTextureViewConstraints(tileStripParticipant.findViewById(R.id.participantTextureView) as TextureView)
        }
    }


    private fun addParticipantToStrip(participantUi: List<ParticipantUI>) {
        binding.stripContainer?.removeAllViews()

        var participantView: View
        var tileStripParticipantViewManager: ParticipantUIView
        participantUi.forEach {
            tileStripParticipantViewManager = ParticipantUIView()
            val params = LinearLayout.LayoutParams(
                resources.getDimension(R.dimen.isc_tile_width).toInt().dpToPx,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            params.setMargins(10, 10, 10, 10)
            tileStripParticipantViewManager.bind(
                binding.stripContainer
            )
            participantView = tileStripParticipantViewManager.getParticipantView()
            Log.i(TAG, "Setting dims to: ${params.width}x${params.height}")
            participantView.layoutParams = params

            binding.stripContainer?.addView(participantView)
            participantViewManagerMap[it.seamGuid] = tileStripParticipantViewManager
            tileStripParticipantViewManager.associateParticipantUI(
                it.copy(
                    streamPriority = participantsConfigMap[it.seamGuid]?.streamPriority,
                    streamQuality = participantsConfigMap[it.seamGuid]?.streamQuality
                )
            )
        }
    }

    private fun configurePortraitView() {
        constraintSet.clear(R.id.layoutMainStage)
        constraintSet.clear(binding.rvIscParticipantTilesList.id)
        constraintSet.constrainWidth(
            R.id.layoutMainStage,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )

        constraintSet.constrainHeight(
            R.id.layoutMainStage,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        constraintSet.connect(
            R.id.layoutMainStage,
            ConstraintSet.TOP,
            binding.iscRoot.id,
            ConstraintSet.TOP
        )
        constraintSet.connect(
            R.id.layoutMainStage,
            ConstraintSet.BOTTOM,
            binding.iscRoot.id,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            R.id.layoutMainStage,
            ConstraintSet.START,
            binding.iscRoot.id,
            ConstraintSet.START
        )
        constraintSet.connect(
            R.id.layoutMainStage,
            ConstraintSet.END,
            binding.iscRoot.id,
            ConstraintSet.END
        )

        constraintSet.setDimensionRatio(R.id.layoutMainStage, "1.5:1")


        constraintSet.constrainWidth(
            binding.rvIscParticipantTilesList.id,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )

        constraintSet.constrainHeight(
            binding.rvIscParticipantTilesList.id,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        constraintSet.connect(
            binding.rvIscParticipantTilesList.id,
            ConstraintSet.TOP, R.id.layoutMainStage, ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.rvIscParticipantTilesList.id,
            ConstraintSet.BOTTOM, binding.iscRoot.id, ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.rvIscParticipantTilesList.id,
            ConstraintSet.START, binding.iscRoot.id, ConstraintSet.START
        )
        constraintSet.connect(
            binding.rvIscParticipantTilesList.id,
            ConstraintSet.END, binding.iscRoot.id, ConstraintSet.END
        )

        participantViewManagerMap.forEach { (_, value) ->
            val params = LinearLayout.LayoutParams(
                resources.getDimension(R.dimen.isc_tile_width).toInt().dpToPx,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            params.setMargins(10, 10, 10, 10)
            value.getParticipantView().layoutParams = params
            value.getParticipantView().requestLayout()
        }

        constraintSet.applyTo(binding.iscRoot)

    }

    private fun configureLandscapeView() {

        constraintSet.clear(R.id.layoutMainStage)
        constraintSet.clear(binding.rvIscParticipantTilesList.id)
        constraintSet.constrainWidth(
            R.id.layoutMainStage,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )

        constraintSet.constrainHeight(
            R.id.layoutMainStage,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        constraintSet.connect(
            R.id.layoutMainStage,
            ConstraintSet.TOP,
            binding.iscRoot.id,
            ConstraintSet.TOP
        )
        constraintSet.connect(
            R.id.layoutMainStage,
            ConstraintSet.START,
            binding.iscRoot.id,
            ConstraintSet.START
        )
        constraintSet.connect(
            R.id.layoutMainStage,
            ConstraintSet.END,
            binding.iscRoot.id,
            ConstraintSet.END
        )
        constraintSet.constrainPercentHeight(R.id.layoutMainStage, 0.55f)
        constraintSet.setDimensionRatio(R.id.layoutMainStage, "1.5:1")
        constraintSet.constrainWidth(
            binding.rvIscParticipantTilesList.id,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )

        constraintSet.constrainHeight(
            binding.rvIscParticipantTilesList.id,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        constraintSet.connect(
            binding.rvIscParticipantTilesList.id,
            ConstraintSet.BOTTOM,
            binding.iscRoot.id,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.rvIscParticipantTilesList.id,
            ConstraintSet.TOP,
            R.id.layoutMainStage,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            binding.rvIscParticipantTilesList.id,
            ConstraintSet.END,
            binding.iscRoot.id,
            ConstraintSet.END
        )
        constraintSet.connect(
            binding.rvIscParticipantTilesList.id,
            ConstraintSet.START,
            binding.iscRoot.id,
            ConstraintSet.START
        )

        participantViewManagerMap.forEach { (_, value) ->
            val params = LinearLayout.LayoutParams(
                resources.getDimension(R.dimen.isc_tile_width).toInt().dpToPx,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            params.setMargins(5, 5, 5, 5)
            value.getParticipantView().layoutParams = params
            value.getParticipantView().requestLayout()
        }

        constraintSet.applyTo(binding.iscRoot)
    }

    private fun setMainStageTextureViewConstraints() {
        val set = ConstraintSet()
        val textureView = binding.layoutMainStage.participantTextureView
        set.clone(binding.layoutMainStage.root)
        set.connect(textureView.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        set.connect(textureView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        set.connect(textureView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.connect(textureView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

        mainStageParticipant?.let {
            if (it.videoWidth != null && it.videoHeight != null) {
                val ratio = (it.videoWidth.toFloat() / it.videoHeight)
                Log.i(TAG, "Ratio: $ratio")
                set.setDimensionRatio(textureView.id, ratio.toString())
            }
        }

        set.applyTo(binding.layoutMainStage.root)
    }

    private fun setTextureViewConstraints(textureView: TextureView) {
        val set = ConstraintSet()
        set.clone(textureView.parent as ConstraintLayout)
        set.connect(textureView.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        set.connect(textureView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        set.connect(textureView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.connect(textureView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

        set.applyTo(textureView.parent as ConstraintLayout)
    }
}