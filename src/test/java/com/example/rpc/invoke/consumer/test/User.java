package com.example.rpc.invoke.consumer.test;

import lombok.Data;

/**
 * @author xianpeng.xia
 * on 2022/5/22 22:22
 */
@Data
public class User {

    private String id;
    private String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
