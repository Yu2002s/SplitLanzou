package com.lanzou.cloud.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.utils.DensityUtils;

public class LinearItemDecoration extends RecyclerView.ItemDecoration {

    private final int size;

    public LinearItemDecoration() {
        this.size = DensityUtils.dp2px(16);
    }

    public LinearItemDecoration(int size) {
        this.size = size;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        outRect.left = size;
        outRect.right = size;
        if (position == 0) {
            outRect.top = size;
        }
        outRect.bottom = size;
    }
}
