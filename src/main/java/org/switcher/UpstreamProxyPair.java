package org.switcher;

import java.net.InetSocketAddress;

public class UpstreamProxyPair {
    public final InetSocketAddress proxySocket;
    public final UpstreamProxyDetail upstreamProxyDetail;

    public UpstreamProxyPair(InetSocketAddress proxySocket, UpstreamProxyDetail upstreamProxyDetail) {
        this.proxySocket = proxySocket;
        this.upstreamProxyDetail = upstreamProxyDetail;
    }
}
