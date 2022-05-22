package com.example.rpc.codec;

import java.io.Serializable;
import lombok.Data;

/**
 * @author xianpeng.xia
 * on 2022/5/22 15:28
 * RpcRequest
 */
@Data
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = 7357415349103066625L;
    private String requestId;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
}
