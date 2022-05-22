package com.example.rpc.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * @author xianpeng.xia
 * on 2022/5/22 10:37
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // 编解码的handler
        // 实际处理业务的RpcConnectHandler
    }
}
