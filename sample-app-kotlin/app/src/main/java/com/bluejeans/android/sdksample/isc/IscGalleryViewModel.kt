package com.bluejeans.android.sdksample.isc

import android.util.Size
import android.view.TextureView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bjnclientcore.media.individualstream.DetachParticipantStreamResult
import com.bjnclientcore.media.individualstream.StreamPriority
import com.bjnclientcore.media.individualstream.StreamQuality
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult
import com.bluejeans.android.sdksample.SampleApplication
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService
import com.bluejeans.rxextensions.ObservableComputed
import com.bluejeans.rxextensions.ObservableValueWithOptional
import com.bluejeans.rxextensions.ObservableVariable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ParticipantUI(
    val seamGuid: String, var name: String,
    val videoWidth: Int? = 0,
    val videoHeight: Int? = 0,
    val videoResolution: ObservableValueWithOptional<Size>? = null,
    val streamPriority: StreamPriority? = null,
    val streamQuality: StreamQuality? = null,
    val pinned: Boolean = false,
    val isVideo: Boolean
)

class IscGalleryViewModel : ViewModel() {
    private val TAG = "IscGalleryViewModel"

    private val videoStreamService =
        SampleApplication.blueJeansSDK.meetingService.videoStreamService
    private val participantService =
        SampleApplication.blueJeansSDK.meetingService.participantsService

    private val compositeDisposable = CompositeDisposable()

    private val _participantUIObservable = MutableLiveData<List<ParticipantUI>>()

    private val _rosterParticipantsObservable = MutableLiveData<List<ParticipantUI>>()

    private val requestedQuality = ObservableVariable<MutableMap<String, StreamQuality>>(
        mutableMapOf()
    )
    private val pinnedParticipants = ObservableVariable<MutableMap<String, Boolean>>(
        mutableMapOf()
    )

    private val bjnParticipantMap: ObservableVariable<Map<String, ParticipantsService.Participant>> =
        ObservableVariable(
            emptyMap(), false
        )

    private val rosterParticipantsObservable: LiveData<List<ParticipantUI>> =
        _rosterParticipantsObservable

    val participantUIObservable: LiveData<List<ParticipantUI>> = _participantUIObservable

    val schedulers = Executors.newSingleThreadExecutor()

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
            val videoParticipants = mutableListOf<ParticipantUI>()
            val nonVideoParticipants = mutableListOf<ParticipantUI>()
            Timber.tag(TAG).i("Video Streams size ${videoStream.size} ")
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
                    } else {
                        nonVideoParticipants.add(
                            ParticipantUI(
                                value.id,
                                value.name,
                                pinned = false,
                                isVideo = false
                            )
                        )
                    }
                }
            videoParticipants + nonVideoParticipants
        }
            .rxObservable
            .debounce(200, TimeUnit.MILLISECONDS, Schedulers.from(schedulers))
            .subscribe(
                {
                    Timber.tag(TAG)
                        .i("Roster participants fetched size ${it.size}")
                    _participantUIObservable.postValue(it.take(25))
                },
                {
                    Timber.tag(TAG).e("Error observing ${it.stackTraceToString()}")
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

    fun pinParticipant(id: String) {
        val currentParticipants = pinnedParticipants.value
        currentParticipants[id] = true
        pinnedParticipants.value = currentParticipants
    }

    fun unpinParticipant(id: String) {
        val currentParticipants = pinnedParticipants.value
        currentParticipants.remove(id)
        pinnedParticipants.value = currentParticipants
    }


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}