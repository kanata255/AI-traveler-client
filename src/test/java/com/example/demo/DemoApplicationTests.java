package com.example.demo;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.example.demo.enticy.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.example.demo.DemoApplication.log;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Test
    void contextLoads() {
        System.out.println("----- selectAll method test ------");
        List<User> userList = userMapper.selectList(null);
        Assert.isTrue(5 == userList.size(), "");
        userList.forEach(System.out::println);
    }

    @Test
    void contentService(){
        List<User> userList = userService.list();
        userList.forEach(user -> log.info("User: id={}, name={}, age={}, email={}", user.getId(), user.getName(), user.getAge(), user.getEmail()));
    }


}
