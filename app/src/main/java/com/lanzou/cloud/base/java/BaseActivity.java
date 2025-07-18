package com.lanzou.cloud.base.java;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        getRecyclerView(view);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            if (recyclerView != null) {
                recyclerView.setClipToPadding(false);
                recyclerView.setPadding(recyclerView.getPaddingStart(), recyclerView.getPaddingTop(),
                        recyclerView.getPaddingEnd(), recyclerView.getPaddingBottom() + bottom);
            }
            onApplyInsertBottom(bottom);
            return insets;
        });
    }

    protected void onApplyInsertBottom(int bottom) {
    }

    private RecyclerView recyclerView;

    private void getRecyclerView(View view) {
         if (view instanceof ViewGroup) {
             ViewGroup viewGroup = (ViewGroup) view;
             for (int i = 0; i < viewGroup.getChildCount(); i++) {
                 View child = viewGroup.getChildAt(i);
                 if (child instanceof RecyclerView) {
                      recyclerView = (RecyclerView) child;
                 } else {
                     getRecyclerView(child);
                 }
             }
         }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
