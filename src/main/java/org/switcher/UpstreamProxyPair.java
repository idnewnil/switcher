package org.switcher;

import java.net.InetSocketAddress;
import java.util.Map;

public class UpstreamProxyPair {
    public final InetSocketAddress proxySocket;
    public final UpstreamProxyDetail upstreamProxyDetail;

    UpstreamProxyPair(Map.Entry<InetSocketAddress, UpstreamProxyDetail> entry) {
        this.proxySocket = entry.getKey();
        this.upstreamProxyDetail = entry.getValue();
    }
}
