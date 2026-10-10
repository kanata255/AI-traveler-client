package com.example.demo.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.RegisterDTO;
import com.example.demo.enticy.User;
import com.example.demo.vo.UserVO;

public interface UserService extends IService<User> {

    String register(RegisterDTO dto);

    String login(LoginDTO dto, String ip);

    void logout();

    UserVO getCurrentUser();
}
