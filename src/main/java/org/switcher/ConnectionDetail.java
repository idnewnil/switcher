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
     * @param proxySocket 上游代理的地址
     */
    ConnectionDetail(InetSocketAddress proxySocket) {
        this.proxySocket = proxySocket;
    }
}
