package com.lanzou.cloud.utils;

// =======================================================
// Class: PatternHelper
// 说明：常用振动节奏生成器
// https://blog.csdn.net/m0_61840987/article/details/147536118
// =======================================================
public class PatternHelper {
    /** 心跳节奏：200ms 休息 100ms 震动 200ms 休息 150ms 震动 */
    public static long[] heartbeatPattern() {
        return new long[]{0, 100, 200, 150};
    }

    /** SOS 节奏：... --- ... */
    public static long[] sosPattern() {
        // S: dot dot dot, O: dash dash dash
        // dot: 200ms on / 200ms off; dash: 600ms on / 200ms off
        return new long[]{
                0,
                200,200, 200,200, 200,600,
                600,200, 600,200, 600,200,
                200,200, 200,200, 200
        };
    }
}

