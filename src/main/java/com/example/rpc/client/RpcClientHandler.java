package com.example.rpc.client;

import com.example.rpc.codec.RpcRequest;
import com.example.rpc.codec.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xianpeng.xia
 * on 2022/5/22 10:24
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private Channel channel;
    private SocketAddress remotePeer;
    private Map<String, RpcFuture> pendingRpcTable = new ConcurrentHashMap<>();

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
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
        String requestId = rpcResponse.getRequestId();
        RpcFuture rpcFuture = pendingRpcTable.get(requestId);

        if (rpcFuture != null) {
            pendingRpcTable.remove(requestId);
            rpcFuture.done(rpcResponse);
        }
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

    /**
     * 异步发送请求方法
     */
    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture rpcFuture = new RpcFuture(request);
        pendingRpcTable.put(request.getRequestId(), rpcFuture);
        channel.writeAndFlush(request);
        return rpcFuture;
    }
}
