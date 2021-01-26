package org.switcher;

import org.switcher.exception.ConnectionNotFoundException;
import org.switcher.exception.ProxyAlreadyExistsException;
import org.switcher.exception.ProxyNotFoundException;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpstreamProxyManager {
    /**
     * 不使用上游代理，直接连接
     */
    public static final InetSocketAddress DIRECT_CONNECTION = InetSocketAddress.createUnresolved("", 0);

    /**
     * 上游代理映射，方便通过 {@link InetSocketAddress} 来查找其详细信息
     */
    final Map<InetSocketAddress, UpstreamProxyDetail> proxies;

    /**
     * 在代理发生变化时，会对连接进行一些处理，因此需要引用connectionManager
     */
    private final ConnectionManager connectionManager;

    /**
     * @param connectionManager {@link UpstreamProxyManager}
     */
    public UpstreamProxyManager(ConnectionManager connectionManager) {
        proxies = new ConcurrentHashMap<>();
        this.connectionManager = connectionManager;
    }

    /**
     * 添加上游代理
     *
     * @param proxySocket 上游代理socket
     * @throws ProxyAlreadyExistsException 如果上游代理已经添加则抛出该异常
     */
    public void add(InetSocketAddress proxySocket) throws ProxyAlreadyExistsException {
        AtomicBoolean contains = new AtomicBoolean(true);

        // 需要原子性操作，不能换为containsKey+put
        proxies.computeIfAbsent(proxySocket, __ -> {
            contains.set(false);
            return new UpstreamProxyDetail();
        });

        if (contains.get()) {
            throw new ProxyAlreadyExistsException();
        }
    }

    /**
     * 获取所有上游代理的socket
     *
     * @return 所有socket的集合
     */
    public Set<InetSocketAddress> getAllSockets() {
        return proxies.keySet();
    }

    /**
     * 获取上游代理详细信息
     *
     * @param proxySocket 上游代理socket
     * @return 对应上游代理的详细信息
     * @throws ProxyNotFoundException 如果该socket没有被添加，则抛出该异常
     */
    public UpstreamProxyDetail getDetail(InetSocketAddress proxySocket) throws ProxyNotFoundException {
        UpstreamProxyDetail value = proxies.get(proxySocket);
        if (value == null) {
            throw new ProxyNotFoundException();
        }
        return value;
    }

    /**
     * 移除上游代理
     *
     * @param proxySocket 上游代理socket
     * @throws ProxyNotFoundException 如果该socket没有被添加，则抛出该异常
     */
    public UpstreamProxyDetail remove(InetSocketAddress proxySocket) throws ProxyNotFoundException {
        UpstreamProxyDetail upstreamProxyDetail = proxies.remove(proxySocket);
        if (upstreamProxyDetail == null) {
            throw new ProxyNotFoundException();
        }
        // 中止和该代理相关的所有连接
        upstreamProxyDetail.relevantConnections.forEach(inetSocketAddress -> {
            try {
                connectionManager.abort(inetSocketAddress);
            } catch (ConnectionNotFoundException ignored) {
            }
        });
        return upstreamProxyDetail;
    }
}
