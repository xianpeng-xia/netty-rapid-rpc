package com.example.rpc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author xianpeng.xia
 * on 2022/5/22 03:53
 *
 * 连接管理器
 */
@Slf4j
public class RpcConnectManager {

    private static volatile RpcConnectManager RPC_CONNECT_MANAGER = new RpcConnectManager();

    private RpcConnectManager() {
    }

    public static RpcConnectManager getInstance() {
        return RPC_CONNECT_MANAGER;
    }

    /**
     * 一个连接对应一个实际的业务处理器
     */
    private Map<InetSocketAddress, RpcClientHandler> connectedHandlerMap = new ConcurrentHashMap<>();

    /**
     * 所有连接成功的地址所对应的任务执行器列表
     */
    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlerList = new CopyOnWriteArrayList<>();

    /**
     * 用于异步提交连接请求的线程池
     */
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65535));

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private ReentrantLock connectedLock = new ReentrantLock();
    private Condition connectedLockCondition = connectedLock.newCondition();

    private Long connectTimeoutMills = 6000L;

    private volatile boolean isRunning = true;
    private AtomicInteger handlerIdx = new AtomicInteger(0);

    /**
     * // 1、异步连接，线程池发起连接，连接失败监听，连接成功监听
     * // 2、对于连接进来的资源做一个缓存，updateConnectedServer
     */
    public void connect(final String serverAddressString) {
        List<String> serverAddressList = Arrays.asList(serverAddressString.split(","));
        updateConnectedServer(serverAddressList);
    }

    /**
     * 选择一个handler
     */
    public RpcClientHandler chooseHandler() {
        CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlerList.clone();
        int size = handlers.size();
        while (isRunning && handlers.size() <= 0) {
            try {
                boolean available = waitingForAvailableHandler();
                if (available) {
                    handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlerList.clone();
                    size = handlers.size();
                }
            } catch (InterruptedException e) {
                log.error("waiting for available node is interrupted");
                throw new RuntimeException("no connect any servers!", e);
            }
        }
        if (!isRunning) {
            return null;
        }
        int index = (handlerIdx.getAndAdd(1) + size) % size;
        return handlers.get(index);
    }

    /**
     * 关闭的方法
     */
    public void stop() {

        isRunning = false;
        for (RpcClientHandler rpcClientHandler : connectedHandlerList) {
            rpcClientHandler.close();
            InetSocketAddress remotePeer = (InetSocketAddress) rpcClientHandler.getRemotePeer();
            connectedHandlerMap.remove(remotePeer);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    /**
     * 发起重连的方法，需要把对应的资源释放
     */
    public void reconnect(final RpcClientHandler handler, final SocketAddress remotePeer) {
        if (handler != null) {
            handler.close();
            connectedHandlerList.remove(handler);
            connectedHandlerMap.remove(remotePeer);
        }
        connectAsync((InetSocketAddress) remotePeer);
    }

    /**
     * 更新缓存信息，并异步发起连接
     */
    public void updateConnectedServer(List<String> serverAddressList) {
        if (CollectionUtils.isEmpty(serverAddressList)) {
            log.error("no available server address");
            clearConnected();
            return;
        }
        // 1、解析serverAddress地址，并临时存到newServerNodeAddressSet
        Set<InetSocketAddress> newServerNodeAddressSet = new HashSet<>();
        // eg:192.168.1.1:8765
        for (String serverAddress : serverAddressList) {
            String[] array = serverAddress.split(":");
            if (array.length != 2) {
                log.error("no available server address:{}", serverAddress);
                continue;
            }
            String host = array[0];
            Integer port = Integer.valueOf(array[1]);
            final InetSocketAddress remotePeer = new InetSocketAddress(host, port);
            newServerNodeAddressSet.add(remotePeer);
        }
        // 2、调用建立连接方法，发起远程连接操作
        for (InetSocketAddress inetSocketAddress : newServerNodeAddressSet) {
            if (connectedHandlerMap.containsKey(inetSocketAddress)) {
                continue;
            }
            connectAsync(inetSocketAddress);
        }
        // 3、如果serverNodeAddressSet不存在的连接，需要从connectedHandlerMap中移除
        for (int i = 0; i < connectedHandlerList.size(); i++) {
            RpcClientHandler rpcClientHandler = connectedHandlerList.get(i);
            InetSocketAddress remotePeer = (InetSocketAddress) rpcClientHandler.getRemotePeer();
            if (!newServerNodeAddressSet.contains(remotePeer)) {
                log.info("remove invalid server node :{}", remotePeer);
                RpcClientHandler handler = connectedHandlerMap.get(remotePeer);
                if (handler != null) {
                    handler.close();
                    connectedHandlerMap.remove(remotePeer);
                }
                connectedHandlerList.remove(rpcClientHandler);
            }
        }
    }

    /**
     * 异步发起连接的方法
     */
    private void connectAsync(InetSocketAddress remotePeer) {
        threadPoolExecutor.submit(() -> {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new RpcClientInitializer());

            connect(bootstrap, remotePeer);
        });
    }

    private void connect(final Bootstrap bootstrap, InetSocketAddress remotePeer) {
        // 1、真正的建立连接
        final ChannelFuture channelFuture = bootstrap.connect(remotePeer);
        // 2、连接失败的时候添加监听，清除资源后重连
        channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("channelFuture.channel close operationComplete,remote peer = {}", remotePeer);
                future.channel().eventLoop().schedule(() -> {
                    log.warn("connect fail,to reconnect!");
                    clearConnected();
                    connect(bootstrap, remotePeer);
                }, 3, TimeUnit.SECONDS);
            }


        });
        // 3、连接成功的时候添加监听，把新连接放入缓存
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("successfully connect to remote server,remote peer = {}", remotePeer);
                    RpcClientHandler handler = future.channel().pipeline().get(RpcClientHandler.class);
                    addHandler(handler);
                }
            }


        });
    }

    /**
     * 加到缓存中
     * connectedHandlerList & connectedHandlerMap
     */
    private void addHandler(RpcClientHandler handler) {
        connectedHandlerList.add(handler);
        InetSocketAddress remotePeer = (InetSocketAddress) handler.getChannel().remoteAddress();
        connectedHandlerMap.put(remotePeer, handler);
        // 唤醒可用的业务执行器 signalAvailableHandler
        signalAvailableHandler();

    }

    /**
     * 唤醒另一端的线程(阻塞的状态) 告知有新连接接入
     */
    private void signalAvailableHandler() {
        connectedLock.lock();
        try {
            connectedLockCondition.signalAll();
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 等待新连接计接入通知方法
     */
    private boolean waitingForAvailableHandler() throws InterruptedException {
        connectedLock.lock();
        try {
            return connectedLockCondition.await(this.connectTimeoutMills, TimeUnit.MILLISECONDS);
        } finally {
            connectedLock.unlock();
        }
    }

    /**
     * 连接失败时，及时释放资源，清空缓存
     */
    private void clearConnected() {
        for (final RpcClientHandler rpcClientHandler : connectedHandlerList) {
            // 通过具体的RpcConnectHandler找到具体的remotePeer
            SocketAddress remotePeer = rpcClientHandler.getRemotePeer();
            RpcClientHandler handler = this.connectedHandlerMap.get(remotePeer);
            if (handler != null) {
                handler.close();
                connectedHandlerMap.remove(remotePeer);
            }
        }
        connectedHandlerList.clear();
    }
}
