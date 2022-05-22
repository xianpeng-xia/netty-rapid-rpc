package com.example.rpc.client.proxy;

import com.example.rpc.client.RpcClientHandler;
import com.example.rpc.client.RpcConnectManager;
import com.example.rpc.client.RpcFuture;
import com.example.rpc.codec.RpcRequest;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.sf.cglib.proxy.InvocationHandler;

/**
 * @author xianpeng.xia
 * on 2022/5/22 21:35
 */
public class RpcProxyImpl<T> implements InvocationHandler {

    private Class<T> clazz;
    private long timeout;

    public RpcProxyImpl(Class<T> clazz, long timeout) {
        this.clazz = clazz;
        this.timeout = timeout;
    }

    /**
     * invoke代理接口调用方式
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1、设置请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        // 2、选择一个合适的client任务处理器
        RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
        // 3、发送真正的客户端请求，返回结果
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture.get(timeout, TimeUnit.SECONDS);
    }
}
