package org.switcher;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

class SwitcherHttpFilter extends HttpFiltersAdapter {
    private final static Logger logger = LoggerFactory.getLogger(SwitcherHttpFilter.class);

    private final Switcher switcher;

    public SwitcherHttpFilter(Switcher switcher, HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
        this.switcher = switcher;
    }

    public SwitcherHttpFilter(Switcher switcher, HttpRequest originalRequest) {
        super(originalRequest);
        this.switcher = switcher;
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        return null;
    }

    @Override
    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
        return super.proxyToServerRequest(httpObject);
    }

    @Override
    public void proxyToServerRequestSending() {
        super.proxyToServerRequestSending();
    }

    @Override
    public void proxyToServerRequestSent() {
        super.proxyToServerRequestSent();
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        return super.serverToProxyResponse(httpObject);
    }

    @Override
    public void serverToProxyResponseTimedOut() {
        super.serverToProxyResponseTimedOut();
    }

    @Override
    public void serverToProxyResponseReceiving() {
        super.serverToProxyResponseReceiving();
    }

    @Override
    public void serverToProxyResponseReceived() {
        super.serverToProxyResponseReceived();
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        return super.proxyToClientResponse(httpObject);
    }

    @Override
    public void proxyToServerConnectionQueued() {
        super.proxyToServerConnectionQueued();
    }

    @Override
    public InetSocketAddress proxyToServerResolutionStarted(String resolvingServerHostAndPort) {
        return super.proxyToServerResolutionStarted(resolvingServerHostAndPort);
    }

    @Override
    public void proxyToServerResolutionFailed(String hostAndPort) {
        super.proxyToServerResolutionFailed(hostAndPort);
    }

    @Override
    public void proxyToServerResolutionSucceeded(String serverHostAndPort, InetSocketAddress resolvedRemoteAddress) {
        super.proxyToServerResolutionSucceeded(serverHostAndPort, resolvedRemoteAddress);
    }

    @Override
    public void proxyToServerConnectionStarted() {
        super.proxyToServerConnectionStarted();
    }

    @Override
    public void proxyToServerConnectionSSLHandshakeStarted() {
        super.proxyToServerConnectionSSLHandshakeStarted();
    }

    @Override
    public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
        super.proxyToServerConnectionSucceeded(serverCtx);
    }

    @Override
    public void proxyToServerConnectionFailed() {
        logger.info("建立到服务器 {} 的连接失败", originalRequest.uri());
    }
}
