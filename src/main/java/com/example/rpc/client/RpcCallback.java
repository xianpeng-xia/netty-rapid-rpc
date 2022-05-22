package com.example.rpc.client;

/**
 * @author xianpeng.xia
 * on 2022/5/22 20:57
 */
public interface RpcCallback {

    void success(Object result);

    void failure(Throwable cause);
}
