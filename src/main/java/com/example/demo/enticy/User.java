package com.example.demo.enticy;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("`user`")
public class User {
    private Long id;
    private String name;
    private Integer age;
    private String email;
    // 映射数据库中的字段，什么时候进行填充
    @TableField(value = "create_time",fill = FieldFill.INSERT)
    // 返回时间格式化
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(value = "update_time",fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}