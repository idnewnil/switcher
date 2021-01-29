package org.switcher;

import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.net.InetSocketAddress;

public class SwitcherHttpProxyServer {
    public final Switcher switcher;
    private final HttpProxyServer httpProxyServer;

    /**
     * 有时可能需要设置直连的下载速度
     * littleproxy暂时没有相应的接口，因此只能再新建一个代理
     */
    private final HttpProxyServer directHttpProxyServer;

    public static SwitcherHttpProxyServerBootstrap bootstrap() {
        return new SwitcherHttpProxyServerBootstrap();
    }

    SwitcherHttpProxyServer(Switcher switcher, HttpProxyServer httpProxyServer,
                            long directProxyReadThrottleBytesPerSecond, long directProxyWriteThrottleBytesPerSecond) {
        this.switcher = switcher;
        this.httpProxyServer = httpProxyServer;
        directHttpProxyServer = DefaultHttpProxyServer.bootstrap()
                .withName("switcher-direct")
                .withPort(0)
                .withAllowLocalOnly(true)
                .withThrottling(directProxyReadThrottleBytesPerSecond, directProxyWriteThrottleBytesPerSecond)
                .start();
        this.switcher.upstreamProxyManager.remove(UpstreamProxyManager.DIRECT_CONNECTION);
        this.switcher.upstreamProxyManager.add(directHttpProxyServer.getListenAddress());
        this.switcher.serverSocket = directHttpProxyServer.getListenAddress();
    }

    public int getIdleConnectionTimeout() {
        return httpProxyServer.getIdleConnectionTimeout();
    }

    public void setIdleConnectionTimeout(int idleConnectionTimeout) {
        httpProxyServer.setIdleConnectionTimeout(idleConnectionTimeout);
    }

    public int getConnectTimeout() {
        return httpProxyServer.getConnectTimeout();
    }

    public void setConnectTimeout(int connectTimeoutMs) {
        httpProxyServer.setConnectTimeout(connectTimeoutMs);
    }

    public void stopAllConnections() {
        switcher.connectionManager.connections.keySet().forEach(switcher.connectionManager::sureAbort);
    }

    public void stop() {
        httpProxyServer.stop();
    }

    public void abort() {
        httpProxyServer.abort();
    }

    public InetSocketAddress getListenAddress() {
        return httpProxyServer.getListenAddress();
    }

    public void setThrottle(long readThrottleBytesPerSecond, long writeThrottleBytesPerSecond) {
        httpProxyServer.setThrottle(readThrottleBytesPerSecond, writeThrottleBytesPerSecond);
    }

    public void setDirectHttpProxyServerThrottle(long readThrottleBytesPerSecond, long writeThrottleBytesPerSecond) {
        directHttpProxyServer.setThrottle(readThrottleBytesPerSecond, writeThrottleBytesPerSecond);
    }
}
