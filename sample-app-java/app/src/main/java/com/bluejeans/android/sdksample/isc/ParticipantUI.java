package com.bluejeans.android.sdksample.isc;

import android.util.Size;
import com.bjnclientcore.media.individualstream.StreamPriority;
import com.bjnclientcore.media.individualstream.StreamQuality;
import com.bluejeans.rxextensions.ObservableValueWithOptional;

public class ParticipantUI {
    private String seamGuid;
    private String name;
    private Boolean isVideo;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private ObservableValueWithOptional<Size> videoResolution;
    private StreamPriority streamPriority;

    private StreamQuality streamQuality;
    private Boolean pinned;

    public ParticipantUI(String seamGuid, String name, int videoWidth, int videoHeight, ObservableValueWithOptional<Size> videoResolution, boolean isVideo,
                         StreamPriority streamPriority, StreamQuality streamQuality, boolean pinned) {
        this.seamGuid = seamGuid;
        this.name = name;
        this.isVideo = isVideo;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoResolution = videoResolution;
        this.streamPriority = streamPriority;
        this.streamQuality = streamQuality;
        this.pinned = pinned;
    }

    public ParticipantUI(String seamGuid, String name, int videoWidth, int videoHeight, ObservableValueWithOptional<Size> videoResolution, boolean isVideo,
                          boolean pinned) {
        this.seamGuid = seamGuid;
        this.name = name;
        this.isVideo = isVideo;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoResolution = videoResolution;
        this.streamPriority = null;
        this.streamQuality = null;
        this.pinned = pinned;
    }

    public String getSeamGuid() {
        return seamGuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getVideo() {
        return isVideo;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public ObservableValueWithOptional<Size> getVideoResolution() {
        return videoResolution;
    }

    public StreamPriority getStreamPriority() {
        return streamPriority;
    }

    public StreamQuality getStreamQuality() {
        return streamQuality;
    }

    public Boolean getPinned() {
        return pinned;
    }
}
