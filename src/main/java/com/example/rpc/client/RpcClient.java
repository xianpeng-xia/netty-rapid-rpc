package com.example.rpc.client;


/**
 * @author xianpeng.xia
 * on 2022/5/22 14:44
 */
public class RpcClient {

    private String serverAddress;
    private long timeout;

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
}
