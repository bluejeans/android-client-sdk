package com.bluejeans.android.sdksample.isc;

import com.bjnclientcore.media.individualstream.StreamPriority;
import com.bjnclientcore.media.individualstream.StreamQuality;

public interface IStreamConfigUpdatedCallback {
    public void onStreamConfigurationUpdated(String seamGuid, StreamQuality streamQuality, StreamPriority streamPriority);

    public void pinParticipant(String participantId, Boolean isPinned);
}
