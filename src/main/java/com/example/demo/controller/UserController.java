package com.example.demo.controller;

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

    /**
     * 新增用户
     * */
    @PostMapping("/save")
    public Result<List<User>> save(@RequestBody User user){
        userService.save(user);
        return Result.success();
    }

    /**
     * 删除用户
     * */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    /**
     * 修改用户
     * */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateById(user);
        return Result.success();
    }

    /**
     * 查询单个用户
     * */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * 按条件查询用户列表
     * name / age / email 均可选，非空才作为过滤条件
     * */
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

    /**
     * 查询用户列表
     * */
    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }
}
