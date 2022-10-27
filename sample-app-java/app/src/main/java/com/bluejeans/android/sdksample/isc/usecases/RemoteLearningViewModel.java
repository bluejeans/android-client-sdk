package com.bluejeans.android.sdksample.isc.usecases;

import android.util.Log;
import android.view.TextureView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bjnclientcore.media.individualstream.DetachParticipantStreamResult;
import com.bjnclientcore.media.individualstream.VideoStreamConfiguration;
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.bluejeanssdk.meeting.individualstream.VideoStreamService;
import com.bluejeans.rxextensions.ObservableComputed;
import com.bluejeans.rxextensions.ObservableValue;
import com.bluejeans.rxextensions.ObservableVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.function.Function;
import java.util.stream.Collectors;
import kotlin.collections.MapsKt;

public class RemoteLearningViewModel extends ViewModel {
    private final String TAG = "RemoteLearningViewModel";
    private static final int TOTAL_NO_OF_PARTICIPANTS = 7;

    private VideoStreamService videoStreamService =
            SampleApplication.getBlueJeansSDK().getMeetingService().getVideoStreamService();
    private ParticipantsService participantService =
            SampleApplication.getBlueJeansSDK().getMeetingService().getParticipantsService();

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ObservableVariable<Map<String, ParticipantsService.Participant>> bjnParticipantMap =
            new ObservableVariable(
                    MapsKt.emptyMap(), false
            );

    private MutableLiveData<List<RemoteLearningParticipant>> _participantsObservable = new MutableLiveData();

    private ExecutorService schedulers = Executors.newSingleThreadExecutor();

    LiveData<List<RemoteLearningParticipant>> participantsObservable = _participantsObservable;

    private ObservableValue<List<RemoteLearningParticipant>> individualStreamsObserver;

    public RemoteLearningViewModel() {
        compositeDisposable.add(
                participantService.getParticipants().getRxObservable()
                        .filter(f -> f.getValue() != null)
                        .subscribeOn(Schedulers.from(schedulers))
                        .subscribe(participants -> {
                            Map<String, ParticipantsService.Participant> participantMap = participants.getValue().stream().filter(p -> p.getId() != participantService.getSelfParticipant().getValue().getId())
                                    .collect(Collectors.toMap(ParticipantsService.Participant::getId, Function.identity()));
                            bjnParticipantMap.setValue(
                                    participantMap
                            );
                        }, err -> {
                            Log.e(TAG, "Error in getting roster updates: " + err.getMessage());
                        })
        );

        individualStreamsObserver = ObservableComputed.Companion.create(bjnParticipantMap.readonly(), videoStreamService.getVideoStreams(), false, (rosterParticipantMap, videoStream) -> {
            List<RemoteLearningParticipant> remoteLearningParticipants = new ArrayList<>();

            rosterParticipantMap.forEach((key, value) -> {
                if (!Objects.equals(key, participantService.getSelfParticipant().getValue().getId())) {
                    if (videoStream.stream().filter(s -> s.getParticipantGuid().equals(key)).findFirst().isPresent()) {
                        videoStream.forEach(stream -> {
                            if (stream.getParticipantGuid().equals(key)) {
                                Log.i(TAG, "Adding stream for " + value.getId() + " and name " + value.getName());
                                if (remoteLearningParticipants.size() <= TOTAL_NO_OF_PARTICIPANTS && stream.getVideoResolution().getValue() != null) {
                                    remoteLearningParticipants.add(new RemoteLearningParticipant(
                                            value.getId(), value.getName(), value.isModerator(),
                                            stream.getVideoResolution().getValue().getWidth(),
                                            stream.getVideoResolution().getValue().getHeight(),
                                            true
                                    ));
                                }
                            }
                        });

                    } else {
                        Log.i(TAG, "No stream found for " + value.getId() + " and name " + value.getName());
                        if (remoteLearningParticipants.size() <= TOTAL_NO_OF_PARTICIPANTS) {
                            remoteLearningParticipants.add(new RemoteLearningParticipant(
                                    value.getId(), value.getName(), value.isModerator(),
                                    0,
                                    0,
                                    false
                            ));
                        }
                    }
                }
            });

            return remoteLearningParticipants;
        });

        compositeDisposable.add(individualStreamsObserver.getRxObservable()
                .debounce(200, TimeUnit.MILLISECONDS, Schedulers.from(schedulers))
                .subscribe(it -> {
                    Log.i(TAG, "Roster participants fetched size " + it.size());
                    _participantsObservable.postValue(it);
                }, err -> {
                    Log.e(TAG, "Error in getting streams: " + err.getMessage());
                }));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

    public VideoStreamConfigurationResult setVideoConfiguration(List<VideoStreamConfiguration> list) {
        return videoStreamService.setVideoStreamConfiguration(list);
    }

    public void attachParticipantToView(String participantGuid, TextureView textureView) {
        videoStreamService.attachParticipantToView(participantGuid, textureView);
    }

    public DetachParticipantStreamResult detachParticipantFromView(String participantGuid) {
        return videoStreamService.detachParticipantFromView(participantGuid);
    }
}
