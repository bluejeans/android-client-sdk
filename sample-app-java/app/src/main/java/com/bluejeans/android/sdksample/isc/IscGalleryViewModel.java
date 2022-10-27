package com.bluejeans.android.sdksample.isc;

import android.util.Log;
import android.view.TextureView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.bjnclientcore.media.individualstream.VideoStreamConfigurationResult;
import com.bluejeans.android.sdksample.SampleApplication;
import com.bluejeans.bluejeanssdk.meeting.ParticipantsService;
import com.bluejeans.bluejeanssdk.meeting.individualstream.VideoStreamService;
import com.bluejeans.rxextensions.ObservableComputed;
import com.bluejeans.rxextensions.ObservableValue;
import com.bluejeans.rxextensions.ObservableVariable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

public class IscGalleryViewModel extends ViewModel {
    private final String TAG = "IscGalleryViewModel";
    private final VideoStreamService videoStreamService;
    private final ParticipantsService participantService;
    private final CompositeDisposable compositeDisposable;
    private final MutableLiveData<List<ParticipantUI>> _participantUIObservable;
    private final MutableLiveData _rosterParticipantsObservable;
    private final ObservableVariable<Map> requestedQuality;
    private final ObservableVariable<Map<String, Boolean>> pinnedParticipants;
    private final ObservableVariable<Map<String, ParticipantsService.Participant>> bjnParticipantMap;
    private final LiveData rosterParticipantsObservable;
    private final LiveData<List<ParticipantUI>> participantUIObservable;
    private final ExecutorService schedulers = Executors.newSingleThreadExecutor();

    private ObservableValue<List<ParticipantUI>> individualStreamsObserver;

    public IscGalleryViewModel() {
        this.videoStreamService = SampleApplication.getBlueJeansSDK().getMeetingService().getVideoStreamService();
        this.participantService = SampleApplication.getBlueJeansSDK().getMeetingService().getParticipantsService();
        compositeDisposable = new CompositeDisposable();
        _participantUIObservable = new MutableLiveData<List<ParticipantUI>>();
        _rosterParticipantsObservable = new MutableLiveData();
        this.requestedQuality = new ObservableVariable<Map>((new LinkedHashMap()), false);
        this.pinnedParticipants = new ObservableVariable<Map<String, Boolean>>((new LinkedHashMap<String, Boolean>()), false);
        this.bjnParticipantMap = new ObservableVariable<>(MapsKt.emptyMap(), false);
        this.rosterParticipantsObservable = this._rosterParticipantsObservable;
        this.participantUIObservable = this._participantUIObservable;
        subscribeToIndividualStreams();
    }

    @NotNull
    public final LiveData<List<ParticipantUI>> getParticipantUIObservable() {
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

    public final void pinParticipant(@NotNull String id) {
        Intrinsics.checkNotNullParameter(id, "id");
        Map<String, Boolean> currentParticipants = this.pinnedParticipants.getValue();
        currentParticipants.put(id, true);
        this.pinnedParticipants.setValue(currentParticipants);
    }

    public final void unpinParticipant(@NotNull String id) {
        Intrinsics.checkNotNullParameter(id, "id");
        Map<String, Boolean> currentParticipants = this.pinnedParticipants.getValue();
        currentParticipants.remove(id);
        this.pinnedParticipants.setValue(currentParticipants);
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

        individualStreamsObserver = ObservableComputed.Companion.create(bjnParticipantMap.readonly(), videoStreamService.getVideoStreams(), false, (rosterParticipantMap, videoStream) -> {
            List<ParticipantUI> videoParticipants = new ArrayList<>();
            List<ParticipantUI> nonVideoParticipants = new ArrayList<>();

            rosterParticipantMap.forEach((key, value) -> {
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

                } else {
                    Log.i(TAG, "No stream found for " + value.getId() + " and name " + value.getName());
                    nonVideoParticipants.add(new ParticipantUI(
                            value.getId(), value.getName(),
                            0,
                            0,
                            null,
                            false,
                            false
                    ));
                }
            });

            ArrayList<ParticipantUI> allParticipants = new ArrayList<>();
            videoParticipants.forEach(p -> allParticipants.add(p));
            nonVideoParticipants.forEach(p -> allParticipants.add(p));

            return allParticipants;
        });

        compositeDisposable.add(individualStreamsObserver.getRxObservable()
                .debounce(200, TimeUnit.MILLISECONDS, Schedulers.from(schedulers))
                .subscribe(it -> {
                    Timber.tag(TAG).i("Roster participants fetched size " + it.size());
                    int finalIndex = Math.min(it.size(), 25);
                    _participantUIObservable.postValue(it.subList(0, finalIndex));
                }, err -> {
                    Log.e(TAG, "Error in getting streams: " + err.getMessage());
                }));
    }
}
