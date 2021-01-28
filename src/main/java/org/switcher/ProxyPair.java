package org.switcher;

import java.net.InetSocketAddress;
import java.util.Map;

public class ProxyPair {
    public final InetSocketAddress proxySocket;
    public final UpstreamProxyDetail upstreamProxyDetail;

    ProxyPair(Map.Entry<InetSocketAddress, UpstreamProxyDetail> entry) {
        this.proxySocket = entry.getKey();
        this.upstreamProxyDetail = entry.getValue();
    }
}
