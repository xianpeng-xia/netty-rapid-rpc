package com.example.rpc.config.provider;

import com.example.rpc.config.RpcConfigAbstract;

/**
 * @author xianpeng.xia
 * on 2022/5/22 18:18
 */
public class ProviderConfig extends RpcConfigAbstract {

    protected Object ref;

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
