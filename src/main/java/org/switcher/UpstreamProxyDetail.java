package org.switcher;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * 上游代理的详细信息
 */
public class UpstreamProxyDetail {
    /**
     * 通过此上游代理访问服务端的所有连接的集合
     */
    Set<InetSocketAddress> relevantConnections;
}
