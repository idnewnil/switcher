package org.switcher;

import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.ClientDetails;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.switcher.exception.ConnectionAlreadySetupException;

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
        connectionManager = new ConnectionManager();
        upstreamProxyManager = new UpstreamProxyManager(connectionManager);
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
            proxyList.forEach(entry ->
                    chainedProxies.add(new SwitcherChainedProxy(clientDetails.getClientAddress(),
                            entry.getKey(), httpRequest.uri())));
        } else {
            // 来自局域网其它主机的连接，只能使用直连
            chainedProxies.add(new SwitcherChainedProxy(clientDetails.getClientAddress(),
                    UpstreamProxyManager.DIRECT_CONNECTION, httpRequest.uri()));
        }
    }

    /**
     * 在原本的 {@link ChainedProxyAdapter} 基础上增加了记录连接信息的功能
     */
    private class SwitcherChainedProxy extends ChainedProxyAdapter {
        private final InetSocketAddress localSocket;
        private final InetSocketAddress proxySocket;
        private final String uri;

        private SwitcherChainedProxy(InetSocketAddress localSocket, InetSocketAddress proxySocket, String uri) {
            this.localSocket = localSocket;
            this.proxySocket = proxySocket;
            this.uri = uri;
        }

        /**
         * @return 代理的地址
         */
        @Override
        public InetSocketAddress getChainedProxyAddress() {
            return localSocket;
        }

        /**
         * 连接通过该代理连接成功后，记录一些必要的信息
         */
        @Override
        public void connectionSucceeded() {
            try {
                connectionManager.add(localSocket, proxySocket);
            } catch (ConnectionAlreadySetupException e) {
                logger.error("{}-{}->{}连接成功，但和已存在连接冲突！", localSocket, proxySocket, uri);
            }
            upstreamProxyManager.proxies.get(proxySocket).relevantConnections.add(localSocket);
        }
    }
}
