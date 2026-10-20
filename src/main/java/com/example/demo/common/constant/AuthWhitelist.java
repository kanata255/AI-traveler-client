package com.example.demo.common.constant;

/**
 * Sa-Token 免登录白名单路径（相对 context-path，不含 /api 前缀）。
 */
public final class AuthWhitelist {

    public static final String[] URLS = {
            "/user/login",
            "/user/register",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/chat/stream"
    };

    private AuthWhitelist() {
    }
}
