package com.example.rpc.invoke.consumer.test;

/**
 * @author xianpeng.xia
 * on 2022/5/22 22:22
 */
public interface HelloService {

    String hello(String name);

    String hello(User user);
}
