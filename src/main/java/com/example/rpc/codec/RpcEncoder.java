package com.example.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author xianpeng.xia
 * on 2022/5/22 16:06
 * RpcEncoder
 */
public class RpcEncoder extends MessageToByteEncoder<Object> {

    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 1、把对应的java对象进行编码
     * 2、把内容填充到buffer中去
     * 3、写出到Server端口
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (genericClass.isInstance(msg)) {
            byte[] data = Serialization.serialize(msg);
            // 消息分为包头和包体
            out.writeInt(data.length);
            out.writeBytes(data);
        }

    }
}
