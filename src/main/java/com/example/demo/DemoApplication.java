package com.example.demo;

import cn.dev33.satoken.SaManager;
import com.example.demo.llm.LlmProperties;
import com.example.demo.utils.DotenvLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
// 访问路径匹配
@RequestMapping("/index")
@MapperScan("com.example.demo.mapper")
@EnableConfigurationProperties(LlmProperties.class)
public class DemoApplication {

    static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {

        DotenvLoader.load();
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("启动成功，Sa-Token配置如下："+ SaManager.getConfig());
    }

}
