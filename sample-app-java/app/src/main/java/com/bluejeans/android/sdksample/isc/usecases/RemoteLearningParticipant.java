package com.bluejeans.android.sdksample.isc.usecases;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RemoteLearningParticipant {
    @NotNull
    private final String participantId;

    @NotNull
    private final String name;

    private final boolean isModerator;

    @Nullable
    private final int width;

    @Nullable
    private final int height;

    private final boolean isVideo;

    public RemoteLearningParticipant(@NotNull String participantId, @NotNull String name, boolean isModerator, @Nullable int width, @Nullable int height, boolean isVideo) {
        this.participantId = participantId;
        this.name = name;
        this.isModerator = isModerator;
        this.width = width;
        this.height = height;
        this.isVideo = isVideo;
    }

    @NotNull
    public final String getParticipantId() {
        return this.participantId;
    }

    @NotNull
    public final String getName() {
        return this.name;
    }

    public final boolean isModerator() {
        return this.isModerator;
    }

    @Nullable
    public final int getWidth() {
        return this.width;
    }

    @Nullable
    public final int getHeight() {
        return this.height;
    }

    public final boolean isVideo() {
        return this.isVideo;
    }
}
