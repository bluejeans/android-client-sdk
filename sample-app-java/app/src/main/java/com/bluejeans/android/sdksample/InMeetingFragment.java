/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */

package com.bluejeans.android.sdksample;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.bluejeans.android.sdksample.viewpager.ScreenSlidePagerAdapter;
import com.bluejeans.bluejeanssdk.meeting.MeetingService;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import kotlin.Unit;

/**
 * A InMeeting fragment responsible for showing remote video & remote content share.
 */
public class InMeetingFragment extends Fragment {

    private static final String TAG = "InMeetingFragment";

    private final MeetingService mMeetingService = SampleApplication.getBlueJeansSDK().getMeetingService();
    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final PagerChangeCallback mPagerCallBackListener = new PagerChangeCallback();
    private MeetingService.VideoState mVideoState;
    private boolean mIsRemoteContentAvailable;
    private ViewPager2 mViewPager;
    private TabLayout mTabLayout;
    private TextView mTvInMeetingState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inmeeting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        registerForSubscription();
    }

    private void initializeViews(View view) {
        mViewPager = view.findViewById(R.id.vpViewPager);
        mTabLayout = view.findViewById(R.id.tabLayout);
        mTvInMeetingState = view.findViewById(R.id.tvInMeetingState);
        ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(this);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.registerOnPageChangeCallback(mPagerCallBackListener);
        new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) -> {
        }).attach();
    }

    private void registerForSubscription() {
        subscribeForRemoteContentState();
        subscribeForVideoState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
        /* The multi-stream remote video fragment computes size at run-time, when handling config change and using
        viewpager2, we need to make sure video fragment or video fragment's parent is visible on Config change inorder to
        propagate dimensions at runtime.*/
        mViewPager.setCurrentItem(0);
    }

    private class PagerChangeCallback extends ViewPager2.OnPageChangeCallback {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            if (position == 0) {
                handleVideoState();
            } else if (position == 1) {
                handleRemoteContentState();
            }
        }
    }

    private void subscribeForVideoState() {
        mDisposable.add(mMeetingService.getVideoState().subscribeOnUI(
                state -> {
                    mVideoState = state;
                    if (mVideoState instanceof MeetingService.VideoState.Active) {
                        showInMeetingView();
                    } else {
                        handleVideoState();
                    }
                    return Unit.INSTANCE;
                },
                err -> {
                    Log.e(TAG, "Error in video state subscription" + err.getMessage());
                    return Unit.INSTANCE;
                }));
    }


    private void subscribeForRemoteContentState() {
        mDisposable.add(mMeetingService.getContentShareService().getReceivingRemoteContent().subscribeOnUI(isReceivingRemoteContent -> {
            if (isReceivingRemoteContent != null) {
                mIsRemoteContentAvailable = isReceivingRemoteContent;
                if (isReceivingRemoteContent) {
                    showInMeetingView();
                } else {
                    handleRemoteContentState();
                }
            }
            return Unit.INSTANCE;
        }, err -> {
            Log.e(TAG, "Error in remote content subscription " + err.getMessage());
            return Unit.INSTANCE;
        }));
    }


    private void handleVideoState() {
        if (mVideoState instanceof MeetingService.VideoState.Active) {
            hideProgress();
        } else if (mVideoState instanceof MeetingService.VideoState.Inactive.SingleParticipant) {
            showProgress("You are the only participant. Please wait some one to join.");
        } else if (mVideoState instanceof MeetingService.VideoState.Inactive.NoOneHasVideo) {
            showProgress("No one is sharing their video.");
        } else if (mVideoState instanceof MeetingService.VideoState.Inactive.NeedsModerator) {
            showProgress("Need moderator");
        } else {
            hideProgress();
        }
    }

    private void handleRemoteContentState() {
        if (mIsRemoteContentAvailable) {
            hideProgress();
        } else {
            showProgress("No one is sharing the remote content.");
        }
    }

    private void showInMeetingView() {
        mViewPager.setVisibility(View.VISIBLE);
        mTvInMeetingState.setVisibility(View.GONE);
    }

    private void showProgress(String msg) {
        mTvInMeetingState.setVisibility(View.VISIBLE);
        mTvInMeetingState.setText(msg);
    }

    private void hideProgress() {
        mTvInMeetingState.setVisibility(View.GONE);
        mTvInMeetingState.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mViewPager != null) {
            mViewPager.unregisterOnPageChangeCallback(mPagerCallBackListener);
        }
    }
}
