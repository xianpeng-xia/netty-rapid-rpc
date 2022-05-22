package com.example.rpc.client;


import com.example.rpc.client.proxy.RpcProxyImpl;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.cglib.proxy.Proxy;

/**
 * @author xianpeng.xia
 * on 2022/5/22 14:44
 */
public class RpcClient {

    private String serverAddress;
    private long timeout;

    private final Map<Class<?>, Object> syncProxyInterfaceMap = new ConcurrentHashMap<>();

    public void initClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }

    private void connect() {
        RpcConnectManager.getInstance().connect(serverAddress);
    }

    private void stop() {
        RpcConnectManager.getInstance().stop();
    }

    /**
     * 同步调用方法
     */
    public <T> T invokeSync(Class<T> interfaceClass) {
        if (syncProxyInterfaceMap.containsKey(interfaceClass)) {
            return (T) syncProxyInterfaceMap.get(interfaceClass);
        }
        Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            new RpcProxyImpl<>(interfaceClass, timeout));
        syncProxyInterfaceMap.putIfAbsent(interfaceClass, proxy);
        return (T) proxy;
    }

}
