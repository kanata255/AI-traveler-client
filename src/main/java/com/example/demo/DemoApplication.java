package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class DemoApplication {

    static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    /**
     * hello
     * @param person
     * @return
     * */
    @RequestMapping("/hello")
    public String hello(@ModelAttribute Person person){
        log.info("hello {}, 性别：{}，年龄：{}！", person.getName(), person.getGender(), person.getAge());
        return "hello " + person.getName() + "，性别：" + person.getGender() + "，年龄：" + person.getAge() + "！";
    }


    // RestFul请求=> 新增 POST  修改PUT 查询GET 删除DELETE


    /**
     * RESTful GET 接口（路径参数）
     */
    @GetMapping("/person/{id}")
    public String getPerson(@PathVariable String id) {
        log.info("获取人员信息，id={}", id);
        return "GET路径参数请求=========>人员ID：" + id;
    }

    /**
     * GET 接口（查询参数）
     * @param name
     * @param gender
     * @param age
     * @return
     */
    @GetMapping("/query")
    public String query(@RequestParam String name, @RequestParam String gender, @RequestParam int age) {
        log.info("查询人员信息，name={}，gender={}，age={}", name, gender, age);
        return "GET查询参数请求=========>姓名：" + name + "，性别：" + gender + "，年龄：" + age;
    }

    @PostMapping("/postPerson")
    public String post(@ModelAttribute Person person) {
        log.info("POST body数据: name={}，gender={}，age={}", person.getName(), person.getGender(), person.getAge());
        return "POST body数据: name=" + person.getName() + ", gender=" + person.getGender() + ", age=" + person.getAge();
    }

    @PutMapping("/putPerson/{id}")
    public String put(@PathVariable String id, @ModelAttribute Person person) {
        log.info("PUT修改人员，id={}，name={}，gender={}，age={}", id, person.getName(), person.getGender(), person.getAge());
        return "PUT修改成功，id=" + id + "，name=" + person.getName() + "，gender=" + person.getGender() + "，age=" + person.getAge();
    }

    @DeleteMapping("/deletePerson/{id}")
    public String delete(@PathVariable String id) {
        log.info("DELETE删除人员，id={}", id);
        return "DELETE删除成功，id=" + id;
    }

}
