package com.example.rpc.codec;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;

/**
 * @author xianpeng.xia
 * on 2022/5/22 16:13
 * Serialization Utils
 */
public class Serialization {

    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();
    private static Objenesis objenesis = new ObjenesisStd();

    public Serialization() {
    }

    /**
     * 序列话
     */
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(clazz);
            return ProtobufIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列话
     */
    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            Schema<T> schema = getSchema(clazz);
            T obj = schema.newMessage();
            ProtobufIOUtil.mergeFrom(data, obj, schema);
            return obj;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static <T> Schema<T> getSchema(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        Schema<T> schema = (Schema<T>) cachedSchema.get(clazz);
        if (Objects.isNull(schema)) {
            schema = RuntimeSchema.createFrom(clazz);
            if (Objects.nonNull(schema)) {
                cachedSchema.put(clazz, schema);
            }
        }
        return schema;
    }
}
