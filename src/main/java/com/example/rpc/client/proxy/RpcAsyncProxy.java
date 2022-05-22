package com.example.rpc.client.proxy;

import com.example.rpc.client.RpcFuture;

/**
 * @author xianpeng.xia
 * on 2022/5/22 21:57
 * 异步代理接口
 */
public interface RpcAsyncProxy {

    RpcFuture call(String funcName, Object... args);
}
