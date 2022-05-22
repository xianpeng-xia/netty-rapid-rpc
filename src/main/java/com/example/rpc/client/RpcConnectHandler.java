package com.example.rpc.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.SocketAddress;

/**
 * @author xianpeng.xia
 * on 2022/5/22 10:24
 */
public class RpcConnectHandler extends SimpleChannelInboundHandler<Object> {

    private Channel channel;
    private SocketAddress remotePeer;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

    }

    public SocketAddress getRemotePeer() {
        return this.remotePeer;
    }

    /**
     * 关闭连接，ChannelFutureListener监听
     */
    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
