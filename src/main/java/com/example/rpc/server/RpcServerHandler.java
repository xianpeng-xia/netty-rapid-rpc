package com.example.rpc.server;

import com.example.rpc.codec.RpcRequest;
import com.example.rpc.codec.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

/**
 * @author xianpeng.xia
 * on 2022/5/22 17:10
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private Map<String, Object> handlerMap;
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
        executor.submit(() -> {
            // 1、解析rpcRequest
            // 2、从handlerMap中找到具体的接口（key）所绑定的实例（bean）
            // 3、通过反射cglib调用 具体方法 传递相关执行参数执行逻辑即可
            // 4、返回响应结果给调用方
            RpcResponse response = new RpcResponse();
            response.setRequestId(rpcRequest.getRequestId());
            try {
                Object result = handle(rpcRequest);
                response.setResult(result);
            } catch (Throwable t) {
                response.setThrowable(t);
                log.error("rpc service handle request Throwable", t);
            }
            ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // afterRpcHook
                    }
                }
            });
        });
    }

    /**
     * 解析request请求并且去通过反射调用具体的本地服务执行具体的方法
     */
    private Object handle(RpcRequest rpcRequest) throws InvocationTargetException {
        String className = rpcRequest.getClassName();
        Object serviceRef = handlerMap.get(className);
        Class<?> serviceRefClass = serviceRef.getClass();
        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] parameters = rpcRequest.getParameters();

        //  Cglib
        FastClass serviceFastClass = FastClass.create(serviceRefClass);
        FastMethod serviceFastClassMethod = serviceFastClass.getMethod(methodName, parameterTypes);

        return serviceFastClassMethod.invoke(serviceRef, parameters);
    }

    /**
     * 异常关闭连接
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server caught Throwable:", cause);
        ctx.close();
    }
}
