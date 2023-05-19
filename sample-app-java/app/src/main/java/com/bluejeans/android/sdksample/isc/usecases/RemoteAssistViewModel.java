/**
 * Copyright (c) 2022 Blue Jeans Networks, Inc. All rights reserved.
 * Created on 10/10/22
 */
package com.bluejeans.android.sdksample.isc.usecases;

import android.util.Log;
import android.view.TextureView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.android.sdksample.isc.ParticipantUI;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.bluejeanssdk.meeting.individualstream.VideoStreamService;
import com.bluejeans.rxextensions.ObservableComputed;
import com.bluejeans.rxextensions.ObservableValue;
import com.bluejeans.rxextensions.ObservableVariable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.Intrinsics;
import timber.log.Timber;

public class RemoteAssistViewModel extends ViewModel {

    private final String TAG = "RemoteAssistViewModel";
    private final VideoStreamService videoStreamService;
    private final ParticipantsService participantService;
    private final CompositeDisposable compositeDisposable;
    private final MutableLiveData<ParticipantUI> _participantUIObservable;
    private final ObservableVariable<Map<String, ParticipantsService.Participant>> bjnParticipantMap;
    private final LiveData<ParticipantUI> participantUIObservable;
    private final ExecutorService schedulers = Executors.newSingleThreadExecutor();

    private ObservableValue<List<ParticipantUI>> individualStreamsObserver;

    public RemoteAssistViewModel() {
        this.videoStreamService = SampleApplication.getBlueJeansSDK().getMeetingService().getVideoStreamService();
        this.participantService = SampleApplication.getBlueJeansSDK().getMeetingService().getParticipantsService();
        compositeDisposable = new CompositeDisposable();
        _participantUIObservable = new MutableLiveData<ParticipantUI>();
        this.bjnParticipantMap = new ObservableVariable(MapsKt.emptyMap(), false);
        this.participantUIObservable = this._participantUIObservable;
        subscribeToIndividualStreams();
    }

    @NotNull
    public final LiveData<ParticipantUI> getParticipantUIObservable() {
        return this.participantUIObservable;
    }

    @NotNull
    public final VideoStreamConfigurationResult setVideoConfiguration(List<com.bjnclientcore.media.individualstream.VideoStreamConfiguration> list) {
        Intrinsics.checkNotNullParameter(list, "list");
        return this.videoStreamService.setVideoStreamConfiguration(list);
    }

    public final void attachParticipantToView(@NotNull String participantGuid, @NotNull TextureView textureView) {
        Intrinsics.checkNotNullParameter(participantGuid, "participantGuid");
        Intrinsics.checkNotNullParameter(textureView, "textureView");
        this.videoStreamService.attachParticipantToView(participantGuid, textureView);
    }

    public final void detachParticipantFromView(@NotNull String participantGuid) {
        Intrinsics.checkNotNullParameter(participantGuid, "participantGuid");
        this.videoStreamService.detachParticipantFromView(participantGuid);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

    private void subscribeToIndividualStreams() {
        compositeDisposable.add(
                participantService.getParticipants().getRxObservable()
                        .filter(f -> f.getValue() != null)
                        .subscribeOn(Schedulers.from(schedulers))
                        .subscribe(participants -> {
                            Map<String, ParticipantsService.Participant> participantMap = participants.getValue().stream().filter(p -> !p.getId().equals(participantService.getSelfParticipant().getValue().getId()))
                                    .collect(Collectors.toMap(ParticipantsService.Participant::getId, Function.identity()));
                            bjnParticipantMap.setValue(
                                    participantMap
                            );
                        }, err -> {
                            Log.e(TAG, "Error in getting roster updates: " + err.getMessage());
                        })
        );

        individualStreamsObserver = ObservableComputed.Companion.create(
                bjnParticipantMap.readonly(),
                videoStreamService.getVideoStreams(),
                compositeDisposable,
                false,
                (rosterParticipants, videoStream) -> {
            List<ParticipantUI> videoParticipants = new ArrayList<>();


            rosterParticipants.forEach((key, value) -> {
                if (videoStream.stream().anyMatch(s -> s.getParticipantGuid().equals(key))) {
                    videoStream.forEach(stream -> {
                        if (stream.getParticipantGuid().equals(key)) {
                            Log.i(TAG, "Adding stream for " + value.getId() + " and name " + value.getName());
                            int width = 0, height = 0;
                            if (stream.getVideoResolution().getValue() != null) {
                                width = stream.getVideoResolution().getValue().getWidth();
                                height = stream.getVideoResolution().getValue().getHeight();
                            }
                            videoParticipants.add(new ParticipantUI(
                                    value.getId(), value.getName(),
                                    width,
                                    height,
                                    stream.getVideoResolution(),
                                    true,
                                    false
                            ));
                        }
                    });

                }
            });

            return videoParticipants;
        });

        compositeDisposable.add(individualStreamsObserver.getRxObservable()
                .debounce(200, TimeUnit.MILLISECONDS, Schedulers.from(schedulers))
                .subscribe(it -> {
                    Timber.tag(TAG).i("Roster participants fetched size " + it.size());
                    if (it.size() >= 1) {
                        _participantUIObservable.postValue(it.get(0));
                    } else {
                        _participantUIObservable.postValue(null);
                    }
                }, err -> {
                    Timber.tag(TAG).e("Error in getting streams: " + err.getMessage());
                }));
    }
}
