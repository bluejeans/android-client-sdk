/**
 * Copyright (c) 2022 Blue Jeans Networks, Inc. All rights reserved.
 * Created on 06/10/22
 */
package com.bluejeans.android.sdksample.isc.usecases

import android.util.Log
import android.view.TextureView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bjnclientcore.media.individualstream.DetachParticipantStreamResult
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.android.sdksample.isc.ParticipantUI
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService
import com.bluejeans.rxextensions.ObservableComputed
import com.bluejeans.rxextensions.ObservableVariable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class RemoteAssistViewModel : ViewModel() {

    private val TAG = "RemoteAssistViewModel"

    private val REMOTE_ASSIST_PARTICIPANT_COUNT = 1

    private val videoStreamService =
        SampleApplication.blueJeansSDK.meetingService.videoStreamService
    private val participantService =
        SampleApplication.blueJeansSDK.meetingService.participantsService

    private val compositeDisposable = CompositeDisposable()

    private val _participantUIObservable = MutableLiveData<ParticipantUI?>()

    private val bjnParticipantMap: ObservableVariable<Map<String, ParticipantsService.Participant>> =
        ObservableVariable(
            emptyMap(), false
        )


    val participantUIObservable: LiveData<ParticipantUI?> = _participantUIObservable

    private val schedulers = Executors.newSingleThreadExecutor()

    init {
        participantService.participants
            .rxObservable
            .filter {
                it.value != null
            }
            .subscribeOn(Schedulers.from(schedulers))
            .subscribe({
                val map =
                    it.value!!.filter { it1 -> it1.id != participantService.selfParticipant.value?.id }
                        .associateBy({ it1 -> it1.id }, { it2 -> it2 })
                bjnParticipantMap.value = map
            }, {
                Log.e(TAG, "Error observing ${it.stackTraceToString()}")
            }).addTo(compositeDisposable)

        ObservableComputed.create(
            bjnParticipantMap,
            videoStreamService.videoStreams,
            distinctUntilChanged = false
        ) { rosterParticipants, videoStream ->
            val videoParticipants = mutableListOf<ParticipantUI>()
            Timber.tag(TAG)
                .i("Video Streams size ${videoStream.size}  and roster size ${rosterParticipants.size}")
            rosterParticipants.filter { it.key != participantService.selfParticipant.value?.id }
                .forEach { (key, value) ->
                    if (videoStream.find { it.participantGuid == key } != null) {
                        videoStream.distinct().forEach { stream ->
                            if (stream.participantGuid == key) {
                                videoParticipants.add(
                                    ParticipantUI(
                                        value.id,
                                        value.name,
                                        stream.videoResolution.value?.width,
                                        stream.videoResolution.value?.height,
                                        stream.videoResolution,
                                        isVideo = true,
                                        pinned = true
                                    )
                                )
                            }
                        }
                    }
                }
            videoParticipants
        }
            .rxObservable
            .debounce(200, TimeUnit.MILLISECONDS, Schedulers.from(schedulers))
            .subscribe(
                {
                    if (it.size >= REMOTE_ASSIST_PARTICIPANT_COUNT) {
                        _participantUIObservable.postValue(it[0])
                    } else {
                        _participantUIObservable.postValue(null)
                    }
                },
                {
                    Log.e(TAG, "Error observing ${it.stackTraceToString()}")
                }).addTo(compositeDisposable)
    }


    fun setVideoConfiguration(list: List<VideoStreamConfiguration>): VideoStreamConfigurationResult {
        return videoStreamService.setVideoStreamConfiguration(list)
    }

    fun attachParticipantToView(participantGuid: String, textureView: TextureView) {
        videoStreamService.attachParticipantToView(participantGuid, textureView)
    }

    fun detachParticipantFromView(participantGuid: String): DetachParticipantStreamResult {
        return videoStreamService.detachParticipantFromView(participantGuid)
    }


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}