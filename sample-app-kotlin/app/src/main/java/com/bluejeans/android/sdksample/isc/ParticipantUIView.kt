/**
 * Copyright (c) 2022 Blue Jeans Networks, Inc. All rights reserved.
 * Created on 09/08/22
 */
package com.bluejeans.android.sdksample.isc

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.bjnclientcore.media.individualstream.StreamPriority
import com.bjnclientcore.media.individualstream.StreamQuality
import com.bjnclientcore.ui.util.extensions.gone
import com.bjnclientcore.ui.util.extensions.visible
import com.bjnclientcore.utils.extensions.getInitials
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.android.sdksample.databinding.IscParticipantViewBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

/**
 * Class representing individual participant tile. This corresponds to the  [ParticipantViewManager]
 * class in the client core
 */
class ParticipantUIView {

    private val TAG = "ParticipantUIView"

    private val constraintSet = ConstraintSet()
    private var compositeDisposable: CompositeDisposable? = null

    private var viewBindingInternal: IscParticipantViewBinding? = null
    private val viewBinding get() = viewBindingInternal!!
    private val videoStreamService =
        SampleApplication.blueJeansSDK.meetingService.videoStreamService
    private var participantUi: ParticipantUI? = null

    fun bind(viewParent: LinearLayout?) {
        viewParent ?: return
        viewBindingInternal =
            IscParticipantViewBinding.inflate(
                viewParent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater,
                viewParent,
                false
            )
        constraintSet.clone(viewBinding.root)
    }

    fun getParticipantView(): ConstraintLayout {
        return viewBinding.root
    }

    fun associateParticipantUI(participantUi: ParticipantUI) {
        this.participantUi = participantUi
        viewBinding.tvParticipantName.text = participantUi.name
        Log.i(
            TAG,
            "Requested Quality: ${getStreamQualityName(participantUi.streamQuality)}"
        )
        viewBinding.tvParticipantPriority?.text = getPriorityName(participantUi.streamPriority)
        if (participantUi.isVideo) {
            viewBinding.cnhAudioOnly.gone()
            videoStreamService.attachParticipantToView(
                participantUi.seamGuid,
                viewBinding.participantTextureView
            )

            Log.d(TAG, "TextureView size: " +
                    "${viewBinding.participantTextureView.width}x${viewBinding.participantTextureView.height}")

            if (participantUi.videoWidth != null && participantUi.videoWidth > 0 &&
                participantUi.videoHeight != null && participantUi.videoHeight > 0) {
                updateAspectRatio(participantUi.videoWidth, participantUi.videoHeight)
            }
            subscribeToResolution(participantUi)
        } else {
            viewBinding.participantTextureView.gone()
            viewBinding.cnhAudioOnly.visible()
            viewBinding.cnhAudioOnly.setText(participantUi.name.getInitials())
        }

    }

    fun updateResolutionAndPriority(streamPriority: StreamPriority, streamQuality: StreamQuality) {
        viewBinding.tvParticipantPriority?.text = getPriorityName(streamPriority)
        Log.i(
            TAG,
            "Requested Quality: ${getStreamQualityName(streamQuality)}"
        )
    }


    fun unbind() {
        participantUi?.let {
            videoStreamService.detachParticipantFromView(
                it.seamGuid
            )
        }
        viewBindingInternal = null
        compositeDisposable?.dispose()
        compositeDisposable = null
    }

    private fun subscribeToResolution(participantUi: ParticipantUI) {
        compositeDisposable = CompositeDisposable()
        compositeDisposable?.add(
            participantUi.videoResolution?.rxObservable?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({ resolution ->
                    resolution.value?.let {
                        Log.i(
                            TAG,
                            "Received Resolution ${it.width}x${it.height} received for participant (${participantUi.name})"
                        )
                        viewBinding.tvParticipantResolution.text =
                            "${it.width}x${it.height}"
                    } ?: kotlin.run {
                        Log.i(TAG, "Resolution is null")
                        viewBinding.tvParticipantResolution.text =
                            "0 x 0"
                    }
                }, {
                    Log.e(TAG, "Error in getting resolution: ${it.stackTraceToString()}")
                })
        )
    }

    private fun updateAspectRatio(width: Int, height: Int) {
        val aspectRatio = (width.toFloat() / height)
        Log.i(TAG, "Aspect ratio of film strip participant: $aspectRatio")
        constraintSet.setDimensionRatio(viewBinding.participantTextureView.id, aspectRatio.toString())
        constraintSet.applyTo(viewBinding.root)
    }
}