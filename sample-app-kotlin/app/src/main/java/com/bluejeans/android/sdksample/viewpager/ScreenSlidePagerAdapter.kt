/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */
package com.bluejeans.android.sdksample.viewpager

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ScreenSlidePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> RemoteViewFragment()
            else -> ContentViewFragment()
        }
    }

    override fun getItemCount(): Int {
        return NUM_PAGES
    }

    companion object {
        private const val NUM_PAGES = 2
    }
}