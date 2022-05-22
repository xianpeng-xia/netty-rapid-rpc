package com.example.rpc.invoke.consumer.test;

import com.example.rpc.client.RpcClient;
import com.example.rpc.client.RpcFuture;
import com.example.rpc.client.proxy.RpcAsyncProxy;
import java.util.concurrent.ExecutionException;

/**
 * @author xianpeng.xia
 * on 2022/5/22 22:37
 */
public class ConsumerStarter {

    public static void sync() {
        RpcClient rpcClient = new RpcClient();
        rpcClient.initClient("127.0.0.1:8765", 3000);
        HelloService helloService = rpcClient.invokeSync(HelloService.class);
        String result = helloService.hello("zhanghua");
        System.out.println(result);
    }

    public static void async() throws ExecutionException, InterruptedException {
        RpcClient rpcClient = new RpcClient();
        rpcClient.initClient("127.0.0.1:8765", 3000);
        RpcAsyncProxy rpcAsyncProxy = rpcClient.invokeAsync(HelloService.class);
        RpcFuture future = rpcAsyncProxy.call("hello", "xiaoming");
        RpcFuture future2 = rpcAsyncProxy.call("hello", new User("1", "xiaohong"));

        Object result = future.get();
        Object result2 = future2.get();
        System.out.println("result = " + result);
        System.out.println("result2 = " + result2);
    }

    public static void main(String[] args) throws Exception {
        sync();
        async();
    }
}
