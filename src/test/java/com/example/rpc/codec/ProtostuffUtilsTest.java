package com.example.rpc.codec;

import com.example.rpc.codec.model.Group;
import com.example.rpc.codec.model.User;
import java.util.Arrays;

/**
 * @author xianpeng.xia
 * on 2022/5/22 15:57
 */
public class ProtostuffUtilsTest {

    public static void main(String[] args) {
        User user = User.builder().id(1).name("u1").desc("programmer").age(29).build();
        Group group = Group.builder().id(1).name("g1").user(user).build();

        byte[] data = Serialization.serialize(group);
        System.out.println("serialize: " + Arrays.toString(data));

        Group result = Serialization.deserialize(data, Group.class);
        System.out.println("deserialize: " + result.toString());
    }
}
