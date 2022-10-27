package com.bluejeans.android.sdksample.isc

import com.bjnclientcore.media.individualstream.StreamPriority
import com.bjnclientcore.media.individualstream.StreamQuality
import timber.log.Timber

private const val TAG = "IscUtils"

fun getPriorityName(priority: StreamPriority?): String {
    return when (priority) {
        StreamPriority.Default -> "Priority: Default"
        StreamPriority.Low -> "Priority: Low"
        StreamPriority.Medium -> "Priority: Medium"
        StreamPriority.High -> "Priority: High"
        else -> {
            Timber.tag(TAG).i("Unrecognized priority: ${priority?.name}")
            "Priority: Unknown"
        }
    }
}

fun getStreamQualityName(streamQuality: StreamQuality?): String {
    return when (streamQuality) {
        StreamQuality.R90p_3fps -> "160x90 @ 3fps"
        StreamQuality.R90p_7fps -> "160x90 @ 7fps"
        StreamQuality.R90p_15fps -> "160x90 @ 15fps"
        StreamQuality.R180p_7fps -> "320x180 @ 7fps"
        StreamQuality.R180p_15fps -> "320x180 @ 15fps"
        StreamQuality.R180p_30fps -> "320x180 @ 30fps"
        StreamQuality.R360p_7fps -> "640x360 @ 7fps"
        StreamQuality.R360p_15fps -> "640x360 @ 15fps"
        StreamQuality.R360p_30fps -> "640x360 @ 30fps"
        StreamQuality.R720p_7fps -> "1280x720 @ 7fps"
        StreamQuality.R720p_15fps -> "1280x720 @ 15fps"
        StreamQuality.R720p_30fps -> "1280x720 @ 30fps"
        else -> "Unknown: ${streamQuality?.name}"
    }
}

fun getStreamPriorityFromString(priority: String): StreamPriority {
    return when (priority) {
        "Default" -> StreamPriority.Default
        "Low" -> StreamPriority.Low
        "Medium" -> StreamPriority.Medium
        "High" -> StreamPriority.High
        else -> {
            StreamPriority.Default
        }
    }
}

fun getStreamQualityFromString(quality: String): StreamQuality {
    return when (quality) {
        "R90_3fps" -> StreamQuality.R90p_3fps
        "R90_7fps" -> StreamQuality.R90p_7fps
        "R90_15fps" -> StreamQuality.R90p_15fps
        "R180_7fps" -> StreamQuality.R180p_7fps
        "R180_15fps" -> StreamQuality.R180p_15fps
        "R180_30fps" -> StreamQuality.R180p_30fps
        "R360_7fps" -> StreamQuality.R360p_7fps
        "R360_15fps" -> StreamQuality.R360p_15fps
        "R360_30fps" -> StreamQuality.R360p_30fps
        "R720_7fps" -> StreamQuality.R720p_7fps
        "R720_15fps" -> StreamQuality.R720p_15fps
        "R720_30fps" -> StreamQuality.R720p_30fps
        else -> {
            StreamQuality.R360p_30fps
        }
    }
}