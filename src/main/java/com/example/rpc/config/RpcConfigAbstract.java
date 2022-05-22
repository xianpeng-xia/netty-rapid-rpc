package com.example.rpc.config;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;

/**
 * @author xianpeng.xia
 * on 2022/5/22 18:06
 */
public class RpcConfigAbstract {

    private AtomicInteger generator = new AtomicInteger(0);

    protected String id;

    protected String interfaceClass = null;

    public String getId() {
        if (StringUtils.isBlank(id)) {
            id = "rapid-cfg-gen-" + generator.getAndIncrement();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInterface(String interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public String getInterface() {
        return this.interfaceClass;
    }
}
