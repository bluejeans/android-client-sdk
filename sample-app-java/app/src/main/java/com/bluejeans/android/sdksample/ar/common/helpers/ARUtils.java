package com.bluejeans.android.sdksample.ar.common.helpers;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.exceptions.UnavailableException;

import java.nio.IntBuffer;
import java.util.Arrays;

public class ARUtils {
    private static final String TAG = "ARUtils";

    public static boolean isARCoreSupportedAndUpToDate(@NonNull Context context, Activity activity) {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(context);

        if (availability == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            return true;
        } else if (availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD || availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED) {
            try {
                ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(activity,  true);
                if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    Log.e(TAG, "ARCore installation requested.");
                    return false;
                } else if (installStatus == ArCoreApk.InstallStatus.INSTALLED) {
                    return true;
                } else {
                    return false;
                }
            } catch (UnavailableException e) {
                Log.e(TAG, "ARCore not installed", e);
                return false;
            }
        } else if (availability == ArCoreApk.Availability.UNKNOWN_ERROR || availability == ArCoreApk.Availability.UNKNOWN_CHECKING ||
                availability == ArCoreApk.Availability.UNKNOWN_TIMED_OUT || availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(TAG, "ARCore is not supported on this device, ArCoreApk.checkAvailability() returned " + availability);
            return false;
        } else {
            return false;
        }
    }

    public static Bitmap getBitmapFromSurface(@NonNull int width, @NonNull int height) {
        int[] pixelData = new int[width * height];
        Arrays.fill(pixelData, 0);

        IntBuffer buf = IntBuffer.wrap(pixelData);
        buf.position(0);
        GLES20.glReadPixels(
                0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf
        );

        // Convert the pixel data from RGBA to what Android wants, ARGB.
        int[] bitmapData = new int[pixelData.length];
        Arrays.fill(bitmapData, 0);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int p = pixelData[i * width + j];
                int b = (p >> 16) & 0xff;
                int r = (p << 16) & 0x00ff0000;
                int ga = p & 0xff00ff00;
                bitmapData[(height - i - 1) * width + j] = ga | r | b;
            }
        }

        return Bitmap.createBitmap(
                bitmapData, height, width, Bitmap.Config.ARGB_8888
        );
    }
}
