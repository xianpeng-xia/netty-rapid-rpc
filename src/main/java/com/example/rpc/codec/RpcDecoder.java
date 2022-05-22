package com.example.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

/**
 * @author xianpeng.xia
 * on 2022/5/22 16:06
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 如果数据包不足4个字节直接返回
        if (in.readableBytes() < 4) {
            return;
        }
        // 记录当前位置
        in.markReaderIndex();
        // 当前数据包的大小
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        // 解码，返回指定的对象
        Object obj = Serialization.deserialize(data, genericClass);
        // 填充到buffer中，传播给下游handler处理
        out.add(obj);
    }
}
