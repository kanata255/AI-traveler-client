package com.example.demo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BizException;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.RegisterDTO;
import com.example.demo.enticy.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import com.example.demo.utils.RedisUtils;
import com.example.demo.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:login:";
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    private static final long RATE_LIMIT_MAX = 10;
    private static final long LOCK_FAIL_MAX = 5;
    private static final long RATE_LIMIT_TTL_SECONDS = 60;
    private static final long LOCK_TTL_SECONDS = 15 * 60;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public String register(RegisterDTO dto) {
        String phone = dto.getPhone();
        String email = dto.getEmail();

        if (count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
            throw new BizException(400, "手机号已被注册");
        }
        if (StringUtils.hasText(email)
                && count(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BizException(400, "邮箱已被注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(email);
        user.setName(dto.getName());
        user.setStatus(1);
        save(user);

        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public String login(LoginDTO dto, String ip) {
        String account = dto.getAccount();

        String rateKey = RATE_LIMIT_KEY_PREFIX + ip;
        if (incrWithTtl(rateKey, RATE_LIMIT_TTL_SECONDS) > RATE_LIMIT_MAX) {
            throw new BizException(429, "操作过于频繁，请稍后再试");
        }

        String failKey = LOGIN_FAIL_KEY_PREFIX + account;
        Object failVal = redisUtils.get(failKey);
        if (failVal != null && Long.parseLong(failVal.toString()) >= LOCK_FAIL_MAX) {
            throw new BizException(423, "账号已锁定，请15分钟后再试");
        }

        User user;
        if (account.matches(PHONE_PATTERN)) {
            user = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, account), false);
        } else {
            user = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, account), false);
        }

        if (user == null) {
            incrFail(failKey);
            throw new BizException(400, "账号或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(400, "账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            incrFail(failKey);
            throw new BizException(400, "账号或密码错误");
        }

        redisUtils.delete(failKey);

        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = getById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    private long incrWithTtl(String key, long ttlSeconds) {
        Long count = redisUtils.increment(key, 1);
        if (count != null && count == 1L) {
            redisUtils.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return count == null ? 0 : count;
    }

    private void incrFail(String failKey) {
        Long count = redisUtils.increment(failKey, 1);
        if (count != null && count == 1L) {
            redisUtils.expire(failKey, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }
}
