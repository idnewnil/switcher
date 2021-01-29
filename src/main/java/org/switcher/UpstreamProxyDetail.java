package org.switcher;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 上游代理的详细信息
 */
public class UpstreamProxyDetail {
    /**
     * 通过此上游代理访问服务端的所有连接的集合
     */
    final Set<InetSocketAddress> relevantConnections;

    public final SpeedRecorder speedRecorder;

    /**
     * 执行和状态相关的操作时需要该锁
     */
    final ReadWriteLock stateLock;

    /**
     * 是否已经被移除
     */
    boolean removed;

    UpstreamProxyDetail(SpeedRecorder parent) {
        relevantConnections = new HashSet<>();
        speedRecorder = new SpeedRecorder(parent);
        stateLock = new ReentrantReadWriteLock(true);
        removed = false;
    }

    public int getRelevantConnectionSize() {
        return relevantConnections.size();
    }
}
