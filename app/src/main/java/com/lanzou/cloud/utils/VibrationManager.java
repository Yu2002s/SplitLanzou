package com.lanzou.cloud.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.RequiresApi;

import java.util.Arrays;

// =======================================================
// Class: VibrationManager
// 说明：振动管理单例，API 兼容调度
// https://blog.csdn.net/m0_61840987/article/details/147536118
// =======================================================
public class VibrationManager {
    private static VibrationManager instance;
    private Vibrator vibrator;
    private Context ctx;

    /** 初始化，建议在 Application.onCreate() 调用 */
    public static void init(Context context) {
        if (instance == null) {
            instance = new VibrationManager(context.getApplicationContext());
        }
    }
    public static VibrationManager get() {
        if (instance == null) {
            throw new IllegalStateException("Must call init() first");
        }
        return instance;
    }

    private VibrationManager(Context context) {
        this.ctx = context;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /** 单次振动 ms 毫秒，最大强度 */
    public void vibrateOneShot(long ms) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, 1/*VibrationEffect.DEFAULT_AMPLITUDE*/));
        } else {
            vibrator.vibrate(ms);
        }
    }

    /** 持续振动，直到 cancel() */
    public void vibrateLong(long ms) {
        vibrateOneShot(ms);
    }

    /** 按 pattern 执行振动，repeat=-1 不循环 */
    public void vibratePattern(long[] pattern, int repeat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int[] amps = new int[pattern.length];
            Arrays.fill(amps, VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amps, repeat));
        } else {
            vibrator.vibrate(pattern, repeat);
        }
    }

    /** Android 12+ 预定义触觉效果 */
    @RequiresApi(Build.VERSION_CODES.S)
    public void vibratePredefined(int effectId) {
        vibrator.vibrate(VibrationEffect.createPredefined(effectId));
    }

    /** 取消所有振动 */
    public void cancel() {
        vibrator.cancel();
    }
}

