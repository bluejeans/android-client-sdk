/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */

package com.bluejeans.android.sdksample

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bjnclientcore.ui.util.extensions.gone
import com.bjnclientcore.ui.util.extensions.visible
import com.bluejeans.android.sdksample.databinding.FragmentInmeetingBinding
import com.bluejeans.android.sdksample.viewpager.PageIndicatorAdapter
import com.bluejeans.android.sdksample.viewpager.RemoteViewFragment
import com.bluejeans.android.sdksample.viewpager.ScreenSlidePagerAdapter
import com.bluejeans.bluejeanssdk.meeting.MeetingService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

/**
 * A InMeeting fragment responsible for showing remote video & remote content.
 */
class InMeetingFragment : Fragment() {

    private val meetingService = SampleApplication.blueJeansSDK.meetingService
    private var videoState: MeetingService.VideoState? = null
    private var inMeetingFragmentBinding: FragmentInmeetingBinding? = null
    private var pagerAdapter: ScreenSlidePagerAdapter? = null
    private val disposable = CompositeDisposable()
    private var isRemoteContentAvailable = false
    private var isViewPagerScrollable = false
    private var pageIndicatorAdapter: PageIndicatorAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inMeetingFragmentBinding = FragmentInmeetingBinding.inflate(inflater, container, false)
        return inMeetingFragmentBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        registerForSubscription()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    private fun registerForSubscription() {
        subscribeForRemoteContentState()
        subscribeForVideoState()
        observePagination()
        subscribeForVideoLayout()
    }

    private fun initializeViews() {
        pagerAdapter = ScreenSlidePagerAdapter(this)
        inMeetingFragmentBinding?.vpViewPager?.adapter = pagerAdapter
        pageIndicatorAdapter = PageIndicatorAdapter()
        pageIndicatorAdapter?.updateListItem(TOTAL_VIEW_PAGER_PAGES)

        inMeetingFragmentBinding?.recyclerPageIndicator?.adapter = pageIndicatorAdapter
        inMeetingFragmentBinding?.vpViewPager?.registerOnPageChangeCallback(PagerChangeCallback())
        inMeetingFragmentBinding?.vpViewPager?.offscreenPageLimit = 1
        inMeetingFragmentBinding?.vpViewPager?.setCurrentItem(1, false)

        inMeetingFragmentBinding?.vpViewPager?.getChildAt(0)?.setOnTouchListener { _, event ->
            val currentFragment = childFragmentManager.findFragmentByTag("f" + inMeetingFragmentBinding?.vpViewPager?.currentItem)
            if (currentFragment is RemoteViewFragment) {
                SampleApplication.blueJeansSDK.meetingService.dispatchTouchEvent(event)
            }
            isViewPagerScrollable
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private inner class PagerChangeCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            when (position) {
                0 -> {
                    if (meetingService.currentRemoteVideoPage.value == 1) {
                        handleRemoteContentState()
                    }
                }
                1 -> {
                    handleVideoState()
                }
            }
            pageIndicatorAdapter?.shiftIndicator(position)
        }
    }

    private fun subscribeForVideoState() {
        disposable.add(meetingService.videoState.subscribeOnUI(
            { state: MeetingService.VideoState ->
                videoState = state
                when (videoState) {
                    is MeetingService.VideoState.Active -> showInMeetingView()
                    else -> handleVideoState()
                }
            }
        ) {
            Log.e(TAG, "Error in video state subscription")
        })
    }

    private fun observePagination() {
        meetingService.currentRemoteVideoPage.rxObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                inMeetingFragmentBinding?.let { binding ->
                    isViewPagerScrollable = it != 1
                    if (it > 0) {
                        pageIndicatorAdapter?.shiftIndicator(it)
                    }
                    Log.i(TAG, "Current page: $it")
                } ?: kotlin.run { Log.e(TAG, "Cannot find meeting binding") }
            }, {
                Log.e(TAG, "Error in getting current page number: ${it.message}")
            }).addTo(disposable)

        meetingService.totalRemoteVideoPages.rxObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.i(TAG, "Total pages: $it")
                if (it > 0) {
                    pageIndicatorAdapter?.updateListItem(it + 1)
                } else {
                    pageIndicatorAdapter?.updateListItem(TOTAL_VIEW_PAGER_PAGES)
                }

                if (it == 1) {
                    pageIndicatorAdapter?.shiftIndicator(1)
                }
            }, {
                Log.e(TAG, "Error in getting total pages: ${it.stackTraceToString()}")
            }).addTo(disposable)
    }

    private fun subscribeForVideoLayout() {
        disposable.add(
            meetingService.videoLayout.rxObservable
                .observeOn(AndroidSchedulers.mainThread())
                .skip(1)
                .filter { it.value != null }
                .subscribe({
                    Log.i(TAG, "Inside video layout sub: ${it.value}")
                    if (it.value == MeetingService.VideoLayout.Speaker) {
                        pageIndicatorAdapter?.updateListItem(TOTAL_VIEW_PAGER_PAGES)
                    }

                    inMeetingFragmentBinding?.vpViewPager?.let { viewPager ->
                        pageIndicatorAdapter?.shiftIndicator(viewPager.currentItem)
                    }
                }, {
                    Log.e(TAG, "Error while subscribing to video layouts: ${it.message}")
                })
        )
    }

    private fun subscribeForRemoteContentState() {
        disposable.add(meetingService.contentShareService.receivingRemoteContent.subscribeOnUI({ isReceivingRemoteContent ->
            if (isReceivingRemoteContent != null) {
                isRemoteContentAvailable = isReceivingRemoteContent
            }
            when (isReceivingRemoteContent) {
                true -> showInMeetingView()
                else -> handleRemoteContentState()
            }
        }) {
            Log.e(TAG, "Error in remote content subscription")
        })
    }

    private fun handleVideoState() {
        when (videoState) {
            is MeetingService.VideoState.Active -> hideProgress()
            is MeetingService.VideoState.Inactive.SingleParticipant ->
                showProgress("You are the only participant. Please wait some one to join.")
            is MeetingService.VideoState.Inactive.NoOneHasVideo ->
                showProgress("No one is sharing their video")
            is MeetingService.VideoState.Inactive.NeedsModerator ->
                showProgress("Need moderator")
            else -> hideProgress()
        }
    }

    private fun handleRemoteContentState() {
        when {
            isRemoteContentAvailable -> hideProgress()
            else -> showProgress("No one is sharing the remote content.")
        }
    }

    private fun showInMeetingView() {
        inMeetingFragmentBinding?.vpViewPager?.visible()
        inMeetingFragmentBinding?.tvInMeetingState?.gone()
    }

    private fun showProgress(msg: String) {
        inMeetingFragmentBinding?.tvInMeetingState?.visible()
        inMeetingFragmentBinding?.tvInMeetingState?.text = msg
    }

    private fun hideProgress() {
        inMeetingFragmentBinding?.tvInMeetingState?.gone()
        inMeetingFragmentBinding?.tvInMeetingState?.text = ""
    }

    companion object {
        const val TAG = "InMeetingFragment"
        const val TOTAL_VIEW_PAGER_PAGES = 2
    }
}