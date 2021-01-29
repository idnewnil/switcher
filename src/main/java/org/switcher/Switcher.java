package org.switcher;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.*;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.littleshoot.proxy.impl.ClientDetails;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public final static SwitchTactics CONNECTION_COUNT = (uri, proxyPairs) -> {
        proxyPairs.sort(Comparator.comparingInt(upstreamProxyPair ->
                upstreamProxyPair.upstreamProxyDetail.relevantConnections.size()));
        return proxyPairs;
    };

    /**
     * 上游代理
     */
    public final UpstreamProxyManager upstreamProxyManager;

    /**
     * 连接
     */
    public final ConnectionManager connectionManager;

    public final Tabu tabu;

    public final SpeedRecorder speedRecorder;

    /**
     * 选择策略
     */
    private SwitchTactics switchTactics;

    public Switcher() {
        this(CONNECTION_COUNT);
    }

    public Switcher(SwitchTactics switchTactics) {
        upstreamProxyManager = new UpstreamProxyManager(this);
        connectionManager = new ConnectionManager(this);
        tabu = new Tabu(this);
        speedRecorder = new SpeedRecorder();
        setSwitchTactics(switchTactics);
    }

    public void setSwitchTactics(SwitchTactics switchTactics) {
        this.switchTactics = switchTactics;
    }

    /**
     * @return 部分配置后的 {@link DefaultHttpProxyServer#bootstrap()}
     */
    public HttpProxyServerBootstrap boostrap() {
        return DefaultHttpProxyServer.bootstrap()
                .withPort(12234)
                .plusActivityTracker(this)
                .withChainProxyManager(this)
                .withManInTheMiddle(new SelfSignedMitmManager())
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest) {
                        return new SwitcherHttpFilter(Switcher.this, originalRequest);
                    }

                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new SwitcherHttpFilter(Switcher.this, originalRequest, ctx);
                    }
                });
    }

    /**
     * 在接收到数据后计算下载速度
     *
     * @param flowContext   {@link FullFlowContext}
     * @param numberOfBytes {@link Integer}
     */
    @Override
    public void bytesReceivedFromServer(FullFlowContext flowContext, int numberOfBytes) {
        ConnectionDetail connectionDetail = connectionManager.sureGetDetail(flowContext.getClientAddress());
        if (connectionDetail != null) {
            connectionDetail.speedRecorder.record(numberOfBytes);
        }
    }

    @Override
    public void clientDisconnected(InetSocketAddress clientAddress, SSLSession sslSession) {
        logger.info("客户端 {} 断开连接", clientAddress);
        connectionManager.remove(clientAddress);
    }

    /**
     * 根据proxySocket创建 {@link ChainedProxy}，如果proxySocket是 {@link UpstreamProxyManager#DIRECT_CONNECTION}
     * 那么会返回 {@link ChainedProxyAdapter#FALLBACK_TO_DIRECT_CONNECTION}
     *
     * @param proxySocket 上游代理的socket
     * @return {@link ChainedProxy}
     */
    private ChainedProxy makeChainedProxy(InetSocketAddress proxySocket) {
        if (proxySocket == UpstreamProxyManager.DIRECT_CONNECTION) {
            return ChainedProxyAdapter.FALLBACK_TO_DIRECT_CONNECTION;
        } else {
            return new ChainedProxyAdapter() {
                @Override
                public InetSocketAddress getChainedProxyAddress() {
                    return proxySocket;
                }
            };
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
            Set<Map.Entry<InetSocketAddress, UpstreamProxyDetail>> proxySet = upstreamProxyManager.proxies.entrySet();
            List<UpstreamProxyPair> proxyList = new ArrayList<>(proxySet.size());
            for (Map.Entry<InetSocketAddress, UpstreamProxyDetail> entry : proxySet) {
                proxyList.add(new UpstreamProxyPair(entry));
            }
            proxyList = switchTactics.getRank(httpRequest.uri(), proxyList);
            // 排序后，将代理地址按顺序加入代理链
            proxyList.forEach(pair -> chainedProxies.add(makeChainedProxy(pair.proxySocket)));
        } else {
            // 来自局域网其它主机的连接，只能使用直连
            chainedProxies.add(makeChainedProxy(UpstreamProxyManager.DIRECT_CONNECTION));
        }
    }
}
