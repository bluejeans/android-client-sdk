package com.bluejeans.android.sdksample.viewpager;

import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluejeans.android.sdksample.R;
import com.bluejeans.android.sdksample.databinding.PageIndicatorBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PageIndicatorAdapter extends RecyclerView.Adapter<PageIndicatorAdapter.PageIndicatorViewHolder> {

    private static final String TAG = "PageIndicatorAdapter";
    private static final int MAX_SIZE = 7;

    private List<Boolean> indicators;

    protected class PageIndicatorViewHolder extends RecyclerView.ViewHolder {
        private PageIndicatorBinding binding;

        public PageIndicatorViewHolder(PageIndicatorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public PageIndicatorAdapter(List<Boolean> indicators) {
        this.indicators = new ArrayList<>(indicators);
    }

    @NonNull
    @Override
    public PageIndicatorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PageIndicatorBinding pageIndicatorBinding = PageIndicatorBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PageIndicatorViewHolder(pageIndicatorBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull PageIndicatorViewHolder holder, int position) {
        Log.i(TAG, "position: " + position + ", value: " + indicators.get(position));
        holder.binding.ivPageIndicator.setSelected(indicators.get(position));
        if (indicators.size() > 1) {
            holder.binding.ivPageIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.binding.ivPageIndicator.setVisibility(View.GONE);
        }

        ViewGroup.LayoutParams params = holder.binding.ivPageIndicator.getLayoutParams();
        Resources resources = holder.binding.ivPageIndicator.getResources();
        int size = resources.getDimensionPixelSize(R.dimen.dimen_6);
        if (indicators.get(position)) {
            size = resources.getDimensionPixelSize(R.dimen.dimen_9);
        }
        params.width = size;
        params.height = size;

        holder.binding.ivPageIndicator.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        if (indicators.size() >= MAX_SIZE) {
            return MAX_SIZE;
        } else {
            return indicators.size();
        }
    }

    public void updateListItem(int count) {
        List<Integer> activeIndicatorIndex = new ArrayList<>();
        int loopLimit = count;

        if (count >= indicators.size()) {
            loopLimit = indicators.size();

        }

        for (int i = 0; i < loopLimit; i++) {
            if (indicators.get(i)) {
                activeIndicatorIndex.add(i);
            }
        }

        indicators.clear();
        List<Boolean> newList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            newList.add(false);
        }
        indicators = newList;
        if (activeIndicatorIndex != null) {
            for (int i = 0; i < activeIndicatorIndex.size(); i++) {
                indicators.set(activeIndicatorIndex.get(i), true);
            }
        }

        notifyDataSetChanged();
    }

    public void shiftIndicator(int position) {
        if (position >= indicators.size()) {
            Log.e(TAG, "position " + position + " is greater or equal to list of size " + indicators.size());
            return;
        }

        if (indicators.isEmpty()) {
            Log.e(TAG, "List is empty");
            return;
        }

        Collections.fill(indicators, false);

        if (position >= MAX_SIZE) {
            indicators.set(MAX_SIZE - 1, true);
        } else {
            indicators.set(position, true);
        }
        notifyDataSetChanged();
    }
}
