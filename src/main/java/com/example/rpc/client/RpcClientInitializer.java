package com.example.rpc.client;

import com.example.rpc.codec.RpcDecoder;
import com.example.rpc.codec.RpcEncoder;
import com.example.rpc.codec.RpcRequest;
import com.example.rpc.codec.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author xianpeng.xia
 * on 2022/5/22 10:37
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline cp = socketChannel.pipeline();
        // 编解码的handler
        cp.addLast(new RpcEncoder(RpcRequest.class));
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcResponse.class));
        // 实际处理业务的RpcConnectHandler
        cp.addLast(new RpcClientHandler());
    }
}
