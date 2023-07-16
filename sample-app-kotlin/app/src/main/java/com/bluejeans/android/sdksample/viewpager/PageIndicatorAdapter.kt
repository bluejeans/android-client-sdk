package com.bluejeans.android.sdksample.viewpager

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bluejeans.android.sdksample.R
import com.bluejeans.android.sdksample.databinding.PageIndicatorBinding

class PageIndicatorAdapter(var list: MutableList<Boolean> = mutableListOf()) :
    RecyclerView.Adapter<PageIndicatorAdapter.PageIndicatorViewHolder>() {

    class PageIndicatorViewHolder(binding: PageIndicatorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val pageIndicatorBinding = PageIndicatorBinding.bind((binding.root))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageIndicatorViewHolder {
        val binding = PageIndicatorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageIndicatorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageIndicatorViewHolder, position: Int) {
        Log.i(TAG, "position: $position, value: ${list[position]}")
        holder.pageIndicatorBinding.ivPageIndicator.isSelected = list[position]
        holder.pageIndicatorBinding.ivPageIndicator.visibility = when {
            list.size > 1 -> View.VISIBLE
            else -> View.GONE
        }
        val params = holder.pageIndicatorBinding.ivPageIndicator.layoutParams
        val resources = holder.pageIndicatorBinding.ivPageIndicator.resources
        val size = when {
            list[position] -> resources.getDimensionPixelSize(R.dimen.dimen_9)
            else -> resources.getDimensionPixelSize(R.dimen.dimen_6)
        }
        params.height = size
        params.width = size
        holder.pageIndicatorBinding.ivPageIndicator.layoutParams = params
    }

    override fun getItemCount(): Int {
        return when (list.size >= MAX_SIZE) {
            true -> MAX_SIZE
            false -> list.size
        }
    }

    fun updateListItem(count: Int) {
        val loopLimit = if (count >= list.size) {
            list.size
        } else {
            count
        }
        val activeIndicatorIndex = mutableListOf<Int>()
        for (i in 0 until loopLimit) {
            if (list[i]) {
                activeIndicatorIndex.add(i)
            }
        }
        list.clear()
        val newList = mutableListOf<Boolean>()
        for (i in 0 until count) {
            newList.add(false)
        }
        list = newList
        activeIndicatorIndex.forEach {
            list[it] = true
        }
        notifyDataSetChanged()
    }

    fun shiftIndicator(position: Int) {
        if (position >= list.size) {
            Log.e(TAG, "position $position is greater or equal to list of size ${list.size}")
            return
        }
        if (list.isEmpty()) {
            Log.e(TAG, "List is empty")
            return
        }
        list.replaceAll { false }
        val pos = when (position >= MAX_SIZE) {
            true -> MAX_SIZE - 1
            false -> position
        }
        list[pos] = true
        notifyDataSetChanged()
    }

    companion object {
        private const val TAG = "PageIndicatorAdapter"
        private const val MAX_SIZE = 7
    }
}