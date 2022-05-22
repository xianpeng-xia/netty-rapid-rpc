package com.example.rpc.invoke.provider.test;

import com.example.rpc.config.provider.ProviderConfig;
import com.example.rpc.config.provider.RpcServerConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xianpeng.xia
 * on 2022/5/22 22:24
 */
public class ProviderStarter {

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                ProviderConfig providerConfig = new ProviderConfig();
                providerConfig.setInterface("com.example.rpc.invoke.consumer.test.HelloService");
                HelloServiceImpl helloServiceImpl = HelloServiceImpl.class.newInstance();
                providerConfig.setRef(helloServiceImpl);

                List<ProviderConfig> providerConfigs = new ArrayList<>();
                providerConfigs.add(providerConfig);

                RpcServerConfig rpcServerConfig = new RpcServerConfig(providerConfigs);
                rpcServerConfig.setPort(8765);
                rpcServerConfig.exporter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
