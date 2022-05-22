package com.example.rpc.client;

import com.example.rpc.codec.RpcRequest;
import com.example.rpc.codec.RpcResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianpeng.xia
 * on 2022/5/22 20:46
 */
@Slf4j
public class RpcFuture implements Future<Object> {

    private RpcRequest request;

    private RpcResponse response;

    private long startTime;

    private static final long TIME_THRESHOLD = 5000;

    private List<RpcCallback> pendingCallbacks = new ArrayList<>();

    private Sync sync;

    private ReentrantLock lock = new ReentrantLock();

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    public RpcFuture(RpcRequest request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
        this.sync = new Sync();
    }

    /**
     * 实际的回调过程
     */
    public void done(RpcResponse rpcResponse) {
        this.response = rpcResponse;
        boolean success = sync.release(1);
        if (success) {
            invokeCallbacks();
        }

        // 整体耗时
        long costTime = System.currentTimeMillis() - startTime;
        if (TIME_THRESHOLD < costTime) {
            log.warn("tht rpc response time is too slow,requestId={},costTime={}", request.getRequestId(), costTime);
        }
    }

    /**
     * 依次执行回调函数
     */
    private void invokeCallbacks() {
        lock.lock();
        try {
            for (final RpcCallback callback : pendingCallbacks) {
                runCallback(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    private void runCallback(RpcCallback callback) {
        final RpcResponse response = this.response;
        executor.submit(() -> {
            if (response.getThrowable() == null) {
                callback.success(response.getResult());
            } else {
                callback.failure(response.getThrowable());
            }
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        if (this.response != null) {
            return this.response.getResult();
        } else {
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("timeout exception request: " + request.toString());
        }
    }

    class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = -5302972055158562733L;

        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int acquires) {
            return getState() == done ? true : false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isDone() {
            return getState() == done;
        }
    }

    /**
     * 可以在应用执行的过程中添加回调处理函数
     */
    public RpcFuture addCallback(RpcCallback callback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                this.pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }
}
