package com.bluejeans.android.sdksample.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class VideoStreamStyleAdapter extends ArrayAdapter<String> {

    private Context context;
    private int itemLayoutId;
    private int selectedPosition = -1;

    public VideoStreamStyleAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        this.context = context;
        this.itemLayoutId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final IscStreamStyleViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(itemLayoutId,
                    parent, false);
            viewHolder = new IscStreamStyleViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (IscStreamStyleViewHolder) convertView.getTag();
        }

        String streamStyle = getItem(position);
        viewHolder.rbItemMenu.setText(streamStyle);
        viewHolder.rbItemMenu.setChecked(position == selectedPosition);
        return convertView;
    }

    static class IscStreamStyleViewHolder {
        CheckedTextView rbItemMenu;

        public IscStreamStyleViewHolder(View view) {
            rbItemMenu = view.findViewById(android.R.id.text1);
        }
    }

    public void updateSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
        notifyDataSetChanged();
    }
}
