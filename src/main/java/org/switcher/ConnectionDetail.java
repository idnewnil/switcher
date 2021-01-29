package org.switcher;

import java.net.InetSocketAddress;

/**
 * 连接的详细信息
 */
public class ConnectionDetail {
    /**
     * 此连接所对应的上游代理的socket
     */
    public final InetSocketAddress proxySocket;

    /**
     * 服务端uri
     */
    public final String uri;

    public final SpeedRecorder speedRecorder;

    /**
     * 连接是否被中止
     */
    boolean abort;

    /**
     * @param proxySocket 上游代理的地址
     * @param uri         目标uri
     */
    ConnectionDetail(InetSocketAddress proxySocket, String uri, SpeedRecorder parent) {
        this.proxySocket = proxySocket;
        this.uri = uri;
        speedRecorder = new SpeedRecorder(parent);
        abort = false;
    }

    public boolean isAbort() {
        return abort;
    }
}
