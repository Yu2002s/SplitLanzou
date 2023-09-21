package com.lanzou.cloud.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.lanzou.cloud.R;
import com.lanzou.cloud.databinding.WindowDownloadFileBinding;

public class DownloadFileService extends Service {

    private WindowManager windowManager;

    private InputMethodManager inputMethodManager;

    private final int baseFlags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        int flags = baseFlags | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        int width = getWindowWidth() / 2;
        int height = (int) (getWindowHeight() * 0.4);

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(width, -2, type, flags, PixelFormat.TRANSPARENT);
        // params.gravity = Gravity.START| Gravity.TOP;
        WindowDownloadFileBinding binding = WindowDownloadFileBinding.inflate(LayoutInflater.from(this));
        windowManager.addView(binding.getRoot(), params);

        EditText edit = binding.edit;

        // binding.toolBar.setNavigationIcon(R.drawable.baseline_add_24);

        binding.getRoot().setOnTouchListener(new View.OnTouchListener() {

            private float downX;
            private float downY;

            private float moveX;
            private float moveY;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX() - params.x;
                        downY = event.getRawY() - params.y;
                        float editX = edit.getX();
                        float editY = edit.getY();
                        if (downX < editX || downX > editX + edit.getWidth()
                                || downY < editY || downY > editY + edit.getHeight()) {
                            edit.clearFocus();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (event.getX() > params.width|| event.getY() > v.getHeight()) {
                            return true;
                        }
                        moveX = event.getRawX() - downX;
                        moveY = event.getRawY() - downY;
                        params.x = (int) moveX;
                        params.y = (int) moveY;
                        windowManager.updateViewLayout(v, params);
                        break;
                }
                return true;
            }
        });

        binding.edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d("jdy", "focus: " + hasFocus);
                if (hasFocus) {
                    if (baseFlags != params.flags) {
                        params.flags = baseFlags;
                        windowManager.updateViewLayout(binding.getRoot(), params);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
                            }
                        }, 400);
                    }
                } else {
                    params.flags = baseFlags | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    windowManager.updateViewLayout(binding.getRoot(), params);
                }
            }
        });
    }

    private int getWindowWidth() {
        return windowManager.getDefaultDisplay().getWidth();
    }

    private int getWindowHeight() {
        return windowManager.getDefaultDisplay().getHeight();
    }
}
