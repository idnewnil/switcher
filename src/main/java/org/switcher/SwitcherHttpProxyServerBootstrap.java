package org.switcher;

import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.ServerGroup;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;

import java.net.InetSocketAddress;

public class SwitcherHttpProxyServerBootstrap {
    private final Switcher switcher;
    private final HttpProxyServerBootstrap httpProxyServerBootstrap;
    private long directProxyReadThrottleBytesPerSecond;
    private long directProxyWriteThrottleBytesPerSecond;

    SwitcherHttpProxyServerBootstrap() {
        this.switcher = new Switcher();
        this.httpProxyServerBootstrap = switcher.bootstrap();
        this.directProxyReadThrottleBytesPerSecond = 0;
        this.directProxyWriteThrottleBytesPerSecond = 0;
    }

    public SwitcherHttpProxyServerBootstrap withName(String name) {
        httpProxyServerBootstrap.withName(name);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withTransportProtocol(TransportProtocol transportProtocol) {
        httpProxyServerBootstrap.withTransportProtocol(transportProtocol);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withAddress(InetSocketAddress address) {
        httpProxyServerBootstrap.withAddress(address);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withPort(int port) {
        httpProxyServerBootstrap.withPort(port);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withAllowLocalOnly(boolean allowLocalOnly) {
        httpProxyServerBootstrap.withAllowLocalOnly(allowLocalOnly);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withSslEngineSource(SslEngineSource sslEngineSource) {
        httpProxyServerBootstrap.withSslEngineSource(sslEngineSource);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withAuthenticateSslClients(boolean authenticateSslClients) {
        httpProxyServerBootstrap.withAuthenticateSslClients(authenticateSslClients);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withProxyAuthenticator(ProxyAuthenticator proxyAuthenticator) {
        httpProxyServerBootstrap.withProxyAuthenticator(proxyAuthenticator);
        return this;
    }


    public SwitcherHttpProxyServerBootstrap withUseDnsSec(boolean useDnsSec) {
        httpProxyServerBootstrap.withUseDnsSec(useDnsSec);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withTransparent(boolean transparent) {
        httpProxyServerBootstrap.withTransparent(transparent);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withIdleConnectionTimeout(int idleConnectionTimeout) {
        httpProxyServerBootstrap.withIdleConnectionTimeout(idleConnectionTimeout);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withConnectTimeout(int connectTimeout) {
        httpProxyServerBootstrap.withConnectTimeout(connectTimeout);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withServerResolver(HostResolver serverResolver) {
        httpProxyServerBootstrap.withServerResolver(serverResolver);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withServerGroup(ServerGroup group) {
        httpProxyServerBootstrap.withServerGroup(group);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap plusActivityTracker(ActivityTracker activityTracker) {
        httpProxyServerBootstrap.plusActivityTracker(activityTracker);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withThrottling(long readThrottleBytesPerSecond, long writeThrottleBytesPerSecond) {
        httpProxyServerBootstrap.withThrottling(readThrottleBytesPerSecond, writeThrottleBytesPerSecond);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withDirectProxyThrottling(
            long readThrottleBytesPerSecond, long writeThrottleBytesPerSecond) {
        directProxyReadThrottleBytesPerSecond = readThrottleBytesPerSecond;
        directProxyWriteThrottleBytesPerSecond = writeThrottleBytesPerSecond;
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withNetworkInterface(InetSocketAddress inetSocketAddress) {
        httpProxyServerBootstrap.withNetworkInterface(inetSocketAddress);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withMaxInitialLineLength(int maxInitialLineLength) {
        httpProxyServerBootstrap.withMaxInitialLineLength(maxInitialLineLength);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withMaxHeaderSize(int maxHeaderSize) {
        httpProxyServerBootstrap.withMaxHeaderSize(maxHeaderSize);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withMaxChunkSize(int maxChunkSize) {
        httpProxyServerBootstrap.withMaxChunkSize(maxChunkSize);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withAllowRequestToOriginServer(boolean allowRequestToOriginServer) {
        httpProxyServerBootstrap.withAllowRequestToOriginServer(allowRequestToOriginServer);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withProxyAlias(String alias) {
        httpProxyServerBootstrap.withProxyAlias(alias);
        return this;
    }

    public SwitcherHttpProxyServer start() {
        return new SwitcherHttpProxyServer(switcher, httpProxyServerBootstrap.start(),
                directProxyReadThrottleBytesPerSecond, directProxyWriteThrottleBytesPerSecond);
    }

    public SwitcherHttpProxyServerBootstrap withThreadPoolConfiguration(ThreadPoolConfiguration configuration) {
        httpProxyServerBootstrap.withThreadPoolConfiguration(configuration);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withAcceptProxyProtocol(boolean allowProxyProtocol) {
        httpProxyServerBootstrap.withAcceptProxyProtocol(allowProxyProtocol);
        return this;
    }

    public SwitcherHttpProxyServerBootstrap withSendProxyProtocol(boolean sendProxyProtocol) {
        httpProxyServerBootstrap.withSendProxyProtocol(sendProxyProtocol);
        return this;
    }
}
