package com.lanzou.split.adapter;

import android.content.Intent;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.split.R;
import com.lanzou.split.data.SimpleItem;
import com.lanzou.split.databinding.ItemListSimpleBinding;

import java.util.List;
import java.util.Map;

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
