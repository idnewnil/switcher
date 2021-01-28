package org.switcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.switcher.exception.UpStreamProxyAlreadyExistsException;
import org.switcher.exception.UpStreamProxyNotFoundException;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.switcher.exception.SwitcherException.UNEXPECTED_EXCEPTION;

public class UpstreamProxyManager {
    private final static Logger logger = LoggerFactory.getLogger(UpstreamProxyManager.class);

    /**
     * 不使用上游代理，直接连接
     */
    public final static InetSocketAddress DIRECT_CONNECTION = InetSocketAddress.createUnresolved("", 0);

    /**
     * 上游代理映射，方便通过 {@link InetSocketAddress} 来查找其详细信息
     */
    final Map<InetSocketAddress, UpstreamProxyDetail> proxies;

    /**
     * 在代理发生变化时，会对连接进行一些处理，因此需要引用connectionManager
     */
    private final Switcher switcher;

    /**
     * @param switcher {@link Switcher}
     */
    UpstreamProxyManager(Switcher switcher) {
        this.switcher = switcher;
        proxies = new ConcurrentHashMap<>();
        proxies.put(DIRECT_CONNECTION, new UpstreamProxyDetail());
    }

    /**
     * 添加上游代理
     *
     * @param host 主机
     * @param port 端口
     * @throws UpStreamProxyAlreadyExistsException 如果上游代理已经添加则抛出该异常
     */
    public void add(String host, int port) throws UpStreamProxyAlreadyExistsException {
        add(new InetSocketAddress(host, port));
    }

    /**
     * 添加上游代理
     *
     * @param proxySocket 上游代理socket
     */
    public UpstreamProxyDetail add(InetSocketAddress proxySocket) {
        AtomicBoolean contains = new AtomicBoolean(true);

        // 需要原子性操作，不能换为containsKey+put
        UpstreamProxyDetail upstreamProxyDetail = proxies.computeIfAbsent(proxySocket, __ -> {
            contains.set(false);
            return new UpstreamProxyDetail();
        });

        if (contains.get()) {
            logger.warn("上游代理 {} 已存在", proxySocket);
        }
        return upstreamProxyDetail;
    }

    void sureAdd(InetSocketAddress proxySocket) {
        UpstreamProxyDetail upstreamProxyDetail = add(proxySocket);
        if (upstreamProxyDetail == null) {
            logger.debug(UNEXPECTED_EXCEPTION, new UpStreamProxyAlreadyExistsException());
        }
    }

    /**
     * 获取所有上游代理的socket
     *
     * @return 所有socket的集合
     */
    public Set<InetSocketAddress> getAll() {
        return new HashSet<>(proxies.keySet());
    }

    /**
     * 获取上游代理详细信息
     *
     * @param proxySocket 上游代理socket
     * @return 对应上游代理的详细信息
     */
    public UpstreamProxyDetail getDetail(InetSocketAddress proxySocket) {
        return proxies.get(proxySocket);
    }

    UpstreamProxyDetail sureGetDetail(InetSocketAddress proxySocket) {
        UpstreamProxyDetail upstreamProxyDetail = getDetail(proxySocket);
        if (upstreamProxyDetail == null) {
            logger.debug(UNEXPECTED_EXCEPTION, new UpStreamProxyNotFoundException());
        }
        return upstreamProxyDetail;
    }

    /**
     * 移除上游代理
     *
     * @param proxySocket 上游代理socket
     */
    public UpstreamProxyDetail remove(InetSocketAddress proxySocket) {
        UpstreamProxyDetail upstreamProxyDetail = proxies.remove(proxySocket);
        if (upstreamProxyDetail != null) {
            // 修改状态，防止产生野连接
            upstreamProxyDetail.stateLock.writeLock().lock();
            upstreamProxyDetail.removed = true;
            upstreamProxyDetail.stateLock.writeLock().unlock();
            // 中止和该代理相关的所有连接
            upstreamProxyDetail.relevantConnections.forEach(switcher.connectionManager::sureAbort);
        }
        if (proxySocket == DIRECT_CONNECTION) {
            sureAdd(DIRECT_CONNECTION);
        }
        return upstreamProxyDetail;
    }
}
