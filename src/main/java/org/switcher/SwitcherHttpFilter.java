package org.switcher;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

class SwitcherHttpFilter extends HttpFiltersAdapter {
    private final static Logger logger = LoggerFactory.getLogger(SwitcherHttpFilter.class);

    private final Switcher switcher;

    private static InetSocketAddress toSocket(String socketString) {
        String[] result = socketString.split(":");
        if (result.length == 1) {
            return new InetSocketAddress(result[0], 80);
        } else if (result.length == 2) {
            return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
        } else {
            logger.debug("非法的socket字符串 {}", socketString);
            return null;
        }
    }

    SwitcherHttpFilter(Switcher switcher, HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
        this.switcher = switcher;
    }

    @Override
    public InetSocketAddress proxyToServerResolutionStarted(String resolvingServerHostAndPort) {
        return toSocket(resolvingServerHostAndPort);
    }

    public SwitcherHttpFilter(Switcher switcher, HttpRequest originalRequest) {
        super(originalRequest);
        this.switcher = switcher;
    }

    private ConnectionDetail getConnectionDetail() {
        InetSocketAddress clientSocket = (InetSocketAddress) ctx.channel().remoteAddress();
        return switcher.connectionManager.sureGetDetail(clientSocket);
    }

    private boolean isAbort() {
        ConnectionDetail connectionDetail = getConnectionDetail();
        return connectionDetail == null || connectionDetail.abort;
    }

    @Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        return isAbort() ? null : httpObject;
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        return isAbort() ? null : httpObject;
    }

    @Override
    public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
        InetSocketAddress clientSocket = (InetSocketAddress) ctx.channel().remoteAddress();
        InetSocketAddress proxySocket = (InetSocketAddress) serverCtx.channel().remoteAddress();
        String uri = originalRequest.uri();
        if (proxySocket.equals(toSocket(uri))) {
            // 如果上游代理的socket和目标服务器的socket一致，说明是直接连接
            proxySocket = UpstreamProxyManager.DIRECT_CONNECTION;
        }
        switcher.connectionManager.add(clientSocket, proxySocket, uri);
    }

    @Override
    public void proxyToServerConnectionFailed() {
        logger.info("尝试建立到 {} 的连接失败", originalRequest.uri());
    }
}
