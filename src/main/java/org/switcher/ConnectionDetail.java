package org.switcher;

import java.net.InetSocketAddress;

/**
 * 连接的详细信息
 */
public class ConnectionDetail {
    /**
     * 此连接所对应的上游代理的socket
     */
    final InetSocketAddress proxySocket;

    /**
     * 服务端uri
     */
    final String uri;

    /**
     * @param proxySocket 上游代理的地址
     * @param uri         目标uri
     */
    ConnectionDetail(InetSocketAddress proxySocket, String uri) {
        this.proxySocket = proxySocket;
        this.uri = uri;
    }
}
