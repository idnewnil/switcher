package org.switcher;

import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.ClientDetails;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.switcher.exception.ConnectionAlreadySetupException;
import org.switcher.exception.ConnectionNotFoundException;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * 负载均衡器，能根据上游代理的拥挤程度自动的分配代理给连接
 */
public class Switcher extends ActivityTrackerAdapter implements ChainedProxyManager {
    private final static Logger logger = LoggerFactory.getLogger(Switcher.class);

    /**
     * 通过连接数来比较两个代理的拥挤程度
     */
    public final static Comparator<UpstreamProxyDetail> CONNECTION_COUNT =
            Comparator.comparingInt(o -> o.relevantConnections.size());

    /**
     * 上游代理
     */
    public final UpstreamProxyManager upstreamProxyManager;

    /**
     * 连接
     */
    public final ConnectionManager connectionManager;

    /**
     * 选择策略
     */
    private Comparator<UpstreamProxyDetail> switchTactics;

    public Switcher() {
        this(CONNECTION_COUNT);
    }

    public Switcher(Comparator<UpstreamProxyDetail> switchTactics) {
        upstreamProxyManager = new UpstreamProxyManager(this);
        connectionManager = new ConnectionManager(this);
        setSwitchTactics(switchTactics);
    }

    public void setSwitchTactics(Comparator<UpstreamProxyDetail> switchTactics) {
        this.switchTactics = switchTactics;
    }

    /**
     * @return 部分配置后的 {@link DefaultHttpProxyServer#bootstrap()}
     */
    public HttpProxyServerBootstrap boostrap() {
        return DefaultHttpProxyServer.bootstrap()
                .plusActivityTracker(this)
                .withChainProxyManager(this);
    }

    /**
     * 在接收到数据后计算下载速度
     *
     * @param flowContext   {@link FullFlowContext}
     * @param numberOfBytes {@link Integer}
     */
    @Override
    public void bytesReceivedFromServer(FullFlowContext flowContext, int numberOfBytes) {
        // TODO 记录下载速度
    }

    @Override
    public void clientDisconnected(InetSocketAddress clientAddress, SSLSession sslSession) {
        try {
            logger.info("客户端 {} 断开连接", clientAddress);
            connectionManager.remove(clientAddress);
        } catch (ConnectionNotFoundException e) {
            logger.debug("", e);
        }
    }

    /**
     * 创建 {@link ChainedProxy}
     *
     * @param clientSocket 客户端的socket
     * @param proxySocket  上游代理的socket
     * @param uri          目标uri
     * @return {@link ChainedProxy}
     */
    private ChainedProxy makeChainedProxy(InetSocketAddress clientSocket, InetSocketAddress proxySocket, String uri) {
        SwitcherChainedProxy switcherChainedProxy = new SwitcherChainedProxy(clientSocket, proxySocket, uri);
        if (proxySocket == UpstreamProxyManager.DIRECT_CONNECTION) {
            // 如果是直连，那么手动调用switcherChainedProxy的connectionSucceeded函数，然后返回FALLBACK_TO_DIRECT_CONNECTION
            switcherChainedProxy.connectionSucceeded();
            return ChainedProxyAdapter.FALLBACK_TO_DIRECT_CONNECTION;
        } else {
            return switcherChainedProxy;
        }
    }

    /**
     * 根据不同的地址，允许使用不同的上游代理
     * 环回地址是本机连接，能够使用所有的上游代理
     * 来自局域网其它主机的连接，则只能使用直连
     *
     * @param httpRequest    {@link HttpRequest}
     * @param chainedProxies {@link ChainedProxy}
     * @param clientDetails  {@link ClientDetails}
     */
    @Override
    public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies,
                                     ClientDetails clientDetails) {
        if (clientDetails.getClientAddress().getAddress().isLoopbackAddress()) {
            // 为了兼容Android，不能使用流的方式
            List<Map.Entry<InetSocketAddress, UpstreamProxyDetail>> proxyList =
                    new ArrayList<>(upstreamProxyManager.proxies.entrySet());
            proxyList.sort((o1, o2) -> switchTactics.compare(o1.getValue(), o2.getValue()));
            // 排序后，将代理地址按顺序加入代理链
            proxyList.forEach(entry -> chainedProxies.add(makeChainedProxy(clientDetails.getClientAddress(),
                    entry.getKey(), httpRequest.uri())));
        } else {
            // 来自局域网其它主机的连接，只能使用直连
            chainedProxies.add(makeChainedProxy(clientDetails.getClientAddress(),
                    UpstreamProxyManager.DIRECT_CONNECTION, httpRequest.uri()));
        }
    }

    /**
     * 在原本的 {@link ChainedProxyAdapter} 基础上增加了记录连接信息的功能
     */
    private class SwitcherChainedProxy extends ChainedProxyAdapter {
        private final InetSocketAddress clientSocket;
        private final InetSocketAddress proxySocket;
        private final String uri;

        private SwitcherChainedProxy(InetSocketAddress clientSocket, InetSocketAddress proxySocket, String uri) {
            this.clientSocket = clientSocket;
            this.proxySocket = proxySocket;
            this.uri = uri;
        }

        /**
         * @return 上游代理的socket
         */
        @Override
        public InetSocketAddress getChainedProxyAddress() {
            return proxySocket;
        }

        /**
         * 连接通过上游代理连接成功后，记录一些必要的信息
         */
        @Override
        public void connectionSucceeded() {
            try {
                connectionManager.add(clientSocket, proxySocket, uri);
                upstreamProxyManager.proxies.get(proxySocket).relevantConnections.add(clientSocket);
            } catch (ConnectionAlreadySetupException e) {
                logger.debug("", e);
            }
        }

        /**
         * 连接出错时的处理
         *
         * @param cause 出错原因
         */
        @Override
        public void connectionFailed(Throwable cause) {
            logger.debug("建立{}失败", ConnectionManager.connectionChain(clientSocket, proxySocket, uri), cause);
        }
    }
}
