package com.bluejeans.android.sdksample.isc;

import com.bjnclientcore.media.individualstream.StreamPriority;
import com.bjnclientcore.media.individualstream.StreamQuality;
import io.reactivex.annotations.NonNull;

public class IscUtils {

    public static String getPriorityName(StreamPriority priority) {
        if (priority == null) {
            return "Cannot get priority at this time";
        }
        switch (priority) {
            case Default:
                return "Priority: Default";
            case Low:
                return "Priority: Low";
            case Medium:
                return "Priority: Medium";
            case High:
                return "Priority: High";
            default:
                return "Priority: Unknown";
        }
    }

    public static String getStreamQualityName(StreamQuality streamQuality) {
        if (streamQuality == null) {
            return "Cannot get resolution at this time";
        }
        switch (streamQuality) {
            case R90p_3fps:
                return "160x90 @ 3fps";
            case R90p_15fps:
                return "160x90 @ 7fps";
            case R90p_7fps:
                return "160x90 @ 15fps";
            case R180p_7fps:
                return "320x180 @ 7fps";
            case R180p_15fps:
                return "320x180 @ 15fps";
            case R180p_30fps:
                return "320x180 @ 30fps";
            case R360p_7fps:
                return "640x360 @ 7fps";
            case R360p_15fps:
                return "640x360 @ 15fps";
            case R360p_30fps:
                return "640x360 @ 30fps";
            case R720p_7fps:
                return "1280x720 @ 7fps";
            case R720p_15fps:
                return "1280x720 @ 15fps";
            case R720p_30fps:
                return "1280x720 @ 30fps";
            default:
                return "Unknown";
        }
    }

    public static StreamPriority getStreamPriorityFromString(String priority) {
        switch (priority) {
            case "Default":
                return StreamPriority.Default;
            case "Low":
                return StreamPriority.Low;
            case "Medium":
                return StreamPriority.Medium;
            case "High":
                return StreamPriority.High;
            default:
                return StreamPriority.Default;
        }
    }

    public static StreamQuality getStreamQualityFromString(String quality) {
        switch (quality) {
            case "R90_3fps":
                return StreamQuality.R90p_3fps;
            case "R90_7fps":
                return StreamQuality.R90p_7fps;
            case "R90_15fps":
                return StreamQuality.R90p_15fps;
            case "R180_7fps":
                return StreamQuality.R180p_7fps;
            case "R180_15fps":
                return StreamQuality.R180p_15fps;
            case "R180_30fps":
                return StreamQuality.R180p_30fps;
            case "R360_7fps":
                return StreamQuality.R360p_7fps;
            case "R360_15fps":
                return StreamQuality.R360p_15fps;
            case "R360_30fps":
                return StreamQuality.R360p_30fps;
            case "R720_7fps":
                return StreamQuality.R720p_7fps;
            case "R720_15fps":
                return StreamQuality.R720p_15fps;
            case "R720_30fps":
                return StreamQuality.R720p_30fps;
            default:
                return StreamQuality.R360p_30fps;
        }
    }

}
