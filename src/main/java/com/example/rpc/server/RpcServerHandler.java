package com.example.rpc.server;

import com.example.rpc.codec.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author xianpeng.xia
 * on 2022/5/22 17:10
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {

    }
}
