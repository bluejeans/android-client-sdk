package com.bluejeans.android.sdksample.ar.common.helpers

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.os.Handler
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException
import java.nio.IntBuffer

object ARCoreUtils {
    private val TAG = "ARUtils"

    fun isARCoreSupportedAndUpToDate(context: Context, activity: Activity): Boolean {
        // Make sure ARCore is installed and supported on this device.
        when (val availability = ArCoreApk.getInstance().checkAvailability(context)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {}
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> try {
                // Request ARCore installation or update if needed.
                when (ArCoreApk.getInstance().requestInstall(activity,  true)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        Log.e(TAG, "ARCore installation requested.")
                        return false
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {}
                }
            } catch (e: UnavailableException) {
                Log.e(TAG, "ARCore not installed", e)
                return false
            }
            ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_CHECKING, ArCoreApk.Availability.UNKNOWN_TIMED_OUT, ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                Log.e(TAG, "ARCore is not supported on this device, ArCoreApk.checkAvailability() returned $availability")
                return false
            }
        }
        return true
    }

    fun getBitmapFromSurface(width: Int, height: Int): Bitmap {
        val pixelData = IntArray(width * height)

        // Read the pixels from the current GL frame.
        val buf: IntBuffer = IntBuffer.wrap(pixelData)
        buf.position(0)
        GLES20.glReadPixels(
            0, 0, width, height,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf
        )

        // Convert the pixel data from RGBA to what Android wants, ARGB.
        val bitmapData = IntArray(pixelData.size)
        for (i in 0 until height) {
            for (j in 0 until width) {
                val p = pixelData[i * width + j]
                val b = p and 0x00ff0000 shr 16
                val r = p and 0x000000ff shl 16
                val ga = p and -0xff0100
                bitmapData[(height - i - 1) * width + j] = ga or r or b
            }
        }
        // Create a bitmap.

        return Bitmap.createBitmap(
            bitmapData,
            height, width, Bitmap.Config.ARGB_8888
        )
    }
}