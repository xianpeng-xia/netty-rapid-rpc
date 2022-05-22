package com.example.rpc.server;

import com.example.rpc.codec.RpcDecoder;
import com.example.rpc.codec.RpcEncoder;
import com.example.rpc.codec.RpcRequest;
import com.example.rpc.codec.RpcResponse;
import com.example.rpc.config.provider.ProviderConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianpeng.xia
 * on 2022/5/22 17:01
 */
@Slf4j
public class RpcServer {

    private String serverAddress;
    private EventLoopGroup boosGroup = new NioEventLoopGroup();
    private EventLoopGroup workGroup = new NioEventLoopGroup();

    private volatile Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serverAddress) throws InterruptedException {
        this.serverAddress = serverAddress;
        this.start();
    }

    /**
     * start
     */
    private void start() throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boosGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            // tcp =  sync + accept = backlog
            .option(ChannelOption.SO_BACKLOG, 1024)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    ChannelPipeline cp = socketChannel.pipeline();
                    // 编解码的handler
                    cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
                    cp.addLast(new RpcDecoder(RpcRequest.class));
                    cp.addLast(new RpcEncoder(RpcResponse.class));
                    // 实际处理业务的RpcConnectHandler
                    cp.addLast(new RpcServerHandler(handlerMap));
                }
            });

        String[] array = serverAddress.split(":");
        String host = array[0];
        Integer port = Integer.parseInt(array[1]);
        ChannelFuture cf = serverBootstrap.bind(host, port).sync();

        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("server success bing to :{}", serverAddress);
                } else {
                    log.info("server fail bing to :{}", serverAddress);
                    throw new Exception("server start fail,cause:{}", future.cause());
                }
            }
        });

        try {
            cf.await(5000, TimeUnit.MILLISECONDS);
            if (cf.isSuccess()) {
                log.info("start rapid rpc success,bing to :{}", serverAddress);
            }
        } catch (InterruptedException e) {
            log.info("start rapid rpc occur interrupted :{}", serverAddress, e);
        }
    }


    /**
     * 程序注册器
     */
    public void registerProcessor(ProviderConfig providerConfig) {
        // key:providerConfig.interface(userService接口权限命名)
        // value:providerConfig.ref(userService接口下的具体实现类userServiceImpl实例对象 )
        handlerMap.put(providerConfig.getInterface(), providerConfig.getRef());
    }

    /**
     * close
     */
    public void close() {
        boosGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }
}
