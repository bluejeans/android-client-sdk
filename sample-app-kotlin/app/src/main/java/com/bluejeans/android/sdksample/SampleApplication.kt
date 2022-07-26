/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample

import android.app.Application
import android.util.Log
import com.bluejeans.bluejeanssdk.BlueJeansSDK
import com.bluejeans.bluejeanssdk.BlueJeansSDKInitParams
import com.bluejeans.bluejeanssdk.GalleryLayoutConfiguration
import com.bluejeans.bluejeanssdk.VideoConfiguration

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSDK()
        Log.i(
            TAG, "App VersionName ${BuildConfig.VERSION_NAME} " +
                    "App VersionCode ${BuildConfig.VERSION_CODE} " +
                    "SDK VersionName ${blueJeansSDK.version}"
        )
    }

    private fun initSDK() {
        try {
            blueJeansSDK = BlueJeansSDK(BlueJeansSDKInitParams(this,videoConfiguration = VideoConfiguration(GalleryLayoutConfiguration.FiveByFive)))
        } catch (ex: Exception) {
            throw ex
        }
    }

    companion object {
        const val TAG = "SampleApplication"
        lateinit var blueJeansSDK: BlueJeansSDK
    }
}