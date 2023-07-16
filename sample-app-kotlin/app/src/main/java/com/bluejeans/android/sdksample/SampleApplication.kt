/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample

import android.app.Application
import com.bluejeans.bluejeanssdk.BlueJeansSDK
import com.bluejeans.bluejeanssdk.BlueJeansSDKInitParams

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSDK()
    }

    private fun initSDK() {
        try {
            blueJeansSDK = BlueJeansSDK(BlueJeansSDKInitParams(this))
        } catch (ex: Exception) {
            throw ex
        }
    }

    companion object {
        const val TAG = "SampleApplication"
        lateinit var blueJeansSDK: BlueJeansSDK
    }
}