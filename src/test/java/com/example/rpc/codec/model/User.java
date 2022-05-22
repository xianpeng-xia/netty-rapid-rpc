package com.example.rpc.codec.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author xianpeng.xia
 * on 2022/5/22 15:57
 */
@Data
@Builder
public class User {

    private Integer id;
    private String name;
    private String desc;
    private Integer age;
}
