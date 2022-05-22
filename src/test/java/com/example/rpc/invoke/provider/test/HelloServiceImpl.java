package com.example.rpc.invoke.provider.test;

import com.example.rpc.invoke.consumer.test.HelloService;
import com.example.rpc.invoke.consumer.test.User;

/**
 * @author xianpeng.xia
 * on 2022/5/22 22:22
 */
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello!" + name;
    }

    @Override
    public String hello(User user) {
        return "hello!" + user.getName();
    }
}
