package com.bluejeans.android.sdksample.isc.usecases

import android.view.TextureView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bjnclientcore.media.individualstream.DetachParticipantStreamResult
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService
import com.bluejeans.rxextensions.ObservableComputed
import com.bluejeans.rxextensions.ObservableVariable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import timber.log.Timber

private const val TOTAL_NO_OF_PARTICIPANTS = 7

data class RemoteLearningParticipant(
    val participantId: String,
    val name: String,
    val isModerator: Boolean = false,
    val width: Int? = 0,
    val height: Int? = 0,
    val isVideo: Boolean
)

class RemoteLearningViewModel : ViewModel() {
    private val TAG = "RemoteLearningViewModel"

    private val videoStreamService =
        SampleApplication.blueJeansSDK.meetingService.videoStreamService
    private val participantService =
        SampleApplication.blueJeansSDK.meetingService.participantsService

    private val compositeDisposable = CompositeDisposable()

    private val bjnParticipantMap: ObservableVariable<Map<String, ParticipantsService.Participant>> =
        ObservableVariable(
            emptyMap(), false
        )

    private val _participantsObservable = MutableLiveData<List<RemoteLearningParticipant>>()

    private val schedulers = Executors.newSingleThreadExecutor()

    val participantsObservable: LiveData<List<RemoteLearningParticipant>> = _participantsObservable

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
                Timber.tag(TAG).e("Error observing ${it.stackTraceToString()}")
            }).addTo(compositeDisposable)

        ObservableComputed.create(
            bjnParticipantMap,
            videoStreamService.videoStreams,
            compositeDisposable,
            distinctUntilChanged = false
        ) { rosterParticipants, videoStream ->
            val remoteLearningParticipants = mutableListOf<RemoteLearningParticipant>()
            Timber.tag(TAG).i("Video Streams size ${videoStream.size} ")
            rosterParticipants.filter { it.key != participantService.selfParticipant.value?.id }
                .forEach { (key, value) ->

                    if (videoStream.find { it.participantGuid == key } != null) {
                        videoStream.distinct().forEach { stream ->
                            if (stream.participantGuid == key) {
                                if (remoteLearningParticipants.size < TOTAL_NO_OF_PARTICIPANTS) {
                                    remoteLearningParticipants.add(
                                        RemoteLearningParticipant(
                                            participantId = key,
                                            name = value.name,
                                            isModerator = value.isModerator,
                                            stream.videoResolution.value?.width,
                                            stream.videoResolution.value?.height,
                                            isVideo = true
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        if (remoteLearningParticipants.size <= TOTAL_NO_OF_PARTICIPANTS) {
                            remoteLearningParticipants.add(
                                RemoteLearningParticipant(
                                    participantId = key,
                                    name = value.name,
                                    isModerator = value.isModerator,
                                    isVideo = false
                                )
                            )
                        }
                    }
                }
            remoteLearningParticipants
        }
            .rxObservable
            .debounce(200, TimeUnit.MILLISECONDS, Schedulers.from(schedulers))
            .subscribe(
                {
                    Timber.tag(TAG)
                        .i("Roster participants fetched size ${it.size}")
                    _participantsObservable.postValue(it)
                },
                {
                    Timber.tag(TAG).e("Error observing ${it.stackTraceToString()}")
                }).addTo(compositeDisposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
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
}