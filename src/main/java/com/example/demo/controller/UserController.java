package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.demo.common.Result;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.RegisterDTO;
import com.example.demo.service.UserService;
import com.example.demo.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户认证", description = "注册 / 登录 / 登出 / 获取当前用户")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "注册", description = "手机号必填 + 密码，邮箱/昵称可选；成功自动登录并返回 token")
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @Operation(summary = "登录", description = "手机号或邮箱 + 密码登录，返回 token")
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.success(userService.login(dto, getClientIp(request)));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前用户", description = "返回当前登录用户信息（不含密码）")
    @GetMapping("/me")
    @SaCheckLogin
    public Result<UserVO> me() {
        return Result.success(userService.getCurrentUser());
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
