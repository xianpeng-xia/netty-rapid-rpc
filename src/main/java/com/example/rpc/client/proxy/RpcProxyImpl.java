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
public class RpcProxyImpl<T> implements InvocationHandler, RpcAsyncProxy {

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

    /**
     * 异步的代理接口实现，抛出RpcFuture给业务方做实际的回调等待处理
     */
    @Override
    public RpcFuture call(String funcName, Object... args) {
        // 1、设置请求对象
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(this.clazz.getName());
        request.setMethodName(funcName);
        request.setParameters(args);
        request.setParameterTypes(parameterTypes);
        // 2、选择一个合适的client任务处理器
        RpcClientHandler handler = RpcConnectManager.getInstance().chooseHandler();
        // 3、发送真正的客户端请求，返回结果
        RpcFuture future = handler.sendRequest(request);
        return future;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        if (typeName.equals("java.lang.Integer")) {
            return Integer.TYPE;
        } else if (typeName.equals("java.lang.Long")) {
            return Long.TYPE;
        } else if (typeName.equals("java.lang.Float")) {
            return Float.TYPE;
        } else if (typeName.equals("java.lang.Double")) {
            return Double.TYPE;
        } else if (typeName.equals("java.lang.Character")) {
            return Character.TYPE;
        } else if (typeName.equals("java.lang.Boolean")) {
            return Boolean.TYPE;
        } else if (typeName.equals("java.lang.Short")) {
            return Short.TYPE;
        } else if (typeName.equals("java.lang.Byte")) {
            return Byte.TYPE;
        }
        return classType;
    }
}
