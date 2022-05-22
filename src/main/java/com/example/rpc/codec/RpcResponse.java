package com.example.rpc.codec;

import java.io.Serializable;
import lombok.Data;

/**
 * @author xianpeng.xia
 * on 2022/5/22 15:29
 *
 * RpcResponse
 */
@Data
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = -382155373066434339L;
    private String requestId;
    private Object result;
    private Throwable throwable;
}
