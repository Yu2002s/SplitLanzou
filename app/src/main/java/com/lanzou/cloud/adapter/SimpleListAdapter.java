package com.lanzou.cloud.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.lanzou.cloud.R;
import com.lanzou.cloud.data.SimpleItem;
import com.lanzou.cloud.databinding.ItemListSimpleBinding;

import java.util.List;

public class SimpleListAdapter extends BaseAdapter {

    private final List<SimpleItem> list;

    public SimpleListAdapter(List<SimpleItem> list) {
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            ItemListSimpleBinding binding = ItemListSimpleBinding
                    .inflate(LayoutInflater.from(parent.getContext()));
            convertView = binding.getRoot();
        }
        SimpleItem simpleItem = list.get(position);
        TextView tv = ((TextView) convertView);
        tv.setText(simpleItem.getTitle());
        tv.setCompoundDrawablePadding(30);
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(parent.getContext(), simpleItem.getIcon())
        , null, null, null);
        return convertView;
    }

}
