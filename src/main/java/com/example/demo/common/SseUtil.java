package com.example.demo.common;

import cn.hutool.json.JSONUtil;

public final class SseUtil {

    private SseUtil() {
    }

    public static String json(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }
}
