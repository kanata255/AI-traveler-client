package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.enticy.User;
import com.example.demo.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/save")
    public Result<List<User>> save(@RequestBody User user) {
        userService.save(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateById(user);
        return Result.success();
    }

    @RequestMapping("doLogin")
    public Result<SaTokenInfo> doLogin(String username, String password) {
        if("zhang".equals(username) && "123456".equals(password)) {
            // 第1步，先登录上
            StpUtil.login(10001);
            // 第2步，获取 Token  相关参数
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            // 第3步，返回给前端
            return Result.success(tokenInfo);
        }
        return Result.error(401, "用户名或密码错误");
    }

    @GetMapping("isLogin")
    public Result<Boolean> isLogin() {
        return Result.success(StpUtil.isLogin());
    }

    /**
     * 查询单个用户（路径变量放在最后，避免与 doLogin/isLogin 冲突）
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping("/query")
    public Result<List<User>> query(@RequestParam(required = false) String name,
                                    @RequestParam(required = false) Integer age,
                                    @RequestParam(required = false) String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), "name", name)
               .eq(age != null, "age", age)
               .like(StringUtils.hasText(email), "email", email);
        return Result.success(userService.list(wrapper));
    }

    @GetMapping("/list")
    @SaCheckPermission("user.list")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }
}