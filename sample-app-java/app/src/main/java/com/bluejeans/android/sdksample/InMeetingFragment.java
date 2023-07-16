/*
 * Copyright (c) 2021 Blue Jeans Network, Inc. All rights reserved.
 */

package com.bluejeans.android.sdksample;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bluejeans.android.sdksample.viewpager.PageIndicatorAdapter;
import com.bluejeans.android.sdksample.viewpager.RemoteViewFragment;
import com.bluejeans.android.sdksample.viewpager.ScreenSlidePagerAdapter;
import com.bluejeans.bluejeanssdk.meeting.MeetingService;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import kotlin.Unit;

/**
 * A InMeeting fragment responsible for showing remote video & remote content share.
 */
public class InMeetingFragment extends Fragment {

    private static final String TAG = "InMeetingFragment";
    private static final int TOTAL_VIEW_PAGER_PAGES = 2;
    private Boolean isTouchEventConsumed = false;

    private final MeetingService mMeetingService = SampleApplication.getBlueJeansSDK().getMeetingService();
    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final PagerChangeCallback mPagerCallBackListener = new PagerChangeCallback();
    private MeetingService.VideoState mVideoState;
    private boolean mIsRemoteContentAvailable;
    private ViewPager2 mViewPager;
    private RecyclerView mPageIndicator;
    private TextView mTvInMeetingState;
    private PageIndicatorAdapter mPageIndicatorAdapter = null;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
    }

    private void initializeViews(View view) {
        mViewPager = view.findViewById(R.id.vpViewPager);
        mPageIndicator = view.findViewById(R.id.recyclerPageIndicator);
        mTvInMeetingState = view.findViewById(R.id.tvInMeetingState);
        ScreenSlidePagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(this);
        mPageIndicatorAdapter = new PageIndicatorAdapter(new ArrayList<>());
        mPageIndicatorAdapter.updateListItem(TOTAL_VIEW_PAGER_PAGES);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.registerOnPageChangeCallback(mPagerCallBackListener);
        mPageIndicator.setAdapter(mPageIndicatorAdapter);
        mViewPager.setCurrentItem(1, false);

        mViewPager.getChildAt(0).setOnTouchListener((v, event) -> {
            Fragment currentFragment = getChildFragmentManager().findFragmentByTag("f" + mViewPager.getCurrentItem());
            if (currentFragment != null && currentFragment instanceof RemoteViewFragment) {
                mMeetingService.dispatchTouchEvent(event);
            }
            return isTouchEventConsumed;
        });
    }

    private void registerForSubscription() {
        subscribeForRemoteContentState();
        subscribeForVideoState();
        observePagination();
        subscribeForVideoLayout();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private class PagerChangeCallback extends ViewPager2.OnPageChangeCallback {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            if (position == 0 && mMeetingService.getCurrentRemoteVideoPage().getValue() == 1) {
                handleRemoteContentState();
            } else if (position == 1) {
                handleVideoState();
            }
            if (mPageIndicatorAdapter != null) {
                Log.i("PageIndicator", "Shifting to position: " + position);
                mPageIndicatorAdapter.shiftIndicator(position);
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

    private void observePagination() {
        mDisposable.add(mMeetingService.getCurrentRemoteVideoPage().getRxObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(currentPage -> {
                isTouchEventConsumed = currentPage != 1;
                Log.i(TAG, "Current page: " + currentPage);
                if (currentPage > 0 && mPageIndicatorAdapter != null) {
                    mPageIndicatorAdapter.shiftIndicator(currentPage);
                }
            }, err -> {
                Log.e(TAG, "Error in getting current page number: " + err.getMessage());
            })
        );

        mDisposable.add(mMeetingService.getTotalRemoteVideoPages().getRxObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(totalPages -> {
                    Log.i(TAG, "Total pages: " + totalPages);
                    if (mPageIndicatorAdapter != null) {
                        if (totalPages > 0) {
                            mPageIndicatorAdapter.updateListItem(totalPages + 1);
                        } else {
                            mPageIndicatorAdapter.updateListItem(TOTAL_VIEW_PAGER_PAGES);
                        }

                        if (totalPages == 1) {
                            mPageIndicatorAdapter.shiftIndicator(totalPages);
                        }
                    }
                }, err -> {
                    Log.e(TAG, "Error in getting total pages: " + err.getMessage());
                })
        );

    }

    private void subscribeForVideoLayout() {
        mDisposable.add(
                mMeetingService.getVideoLayout().getRxObservable().observeOn(AndroidSchedulers.mainThread())
                        .skip(1)
                        .filter(layout -> layout.getValue() != null)
                        .subscribe(layout -> {
                            Log.i(TAG, "Inside video layout sub: " + layout.getValue());
                            if (mPageIndicatorAdapter != null) {
                                if (layout.getValue() instanceof MeetingService.VideoLayout.Speaker) {
                                    mPageIndicatorAdapter.updateListItem(TOTAL_VIEW_PAGER_PAGES);
                                }

                                mPageIndicatorAdapter.shiftIndicator(mViewPager.getCurrentItem());
                            }
                        }, err -> {
                            Log.e(TAG, "Error while subscribing to video layouts: " + err.getMessage());
                        })
        );
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
