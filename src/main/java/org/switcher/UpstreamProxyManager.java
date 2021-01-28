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
    public UpstreamProxyDetail add(String host, int port) throws UpStreamProxyAlreadyExistsException {
        return add(new InetSocketAddress(host, port));
    }

    /**
     * 添加上游代理
     *
     * @param proxySocket 上游代理socket
     * @throws UpStreamProxyAlreadyExistsException 如果上游代理已经添加则抛出该异常
     */
    public UpstreamProxyDetail add(InetSocketAddress proxySocket) throws UpStreamProxyAlreadyExistsException {
        AtomicBoolean contains = new AtomicBoolean(true);

        // 需要原子性操作，不能换为containsKey+put
        UpstreamProxyDetail upstreamProxyDetail = proxies.computeIfAbsent(proxySocket, __ -> {
            contains.set(false);
            return new UpstreamProxyDetail();
        });

        if (contains.get()) {
            logger.info("上游代理 {} 已存在", proxySocket);
            throw new UpStreamProxyAlreadyExistsException(upstreamProxyDetail);
        }
        return upstreamProxyDetail;
    }

    /**
     * 获取所有上游代理的socket
     *
     * @return 所有socket的集合
     */
    public Set<InetSocketAddress> getAll() {
        return new HashSet<>(proxies.keySet());
    }

    UpstreamProxyDetail sureGetDetail(InetSocketAddress proxySocket) {
        try {
            return getDetail(proxySocket);
        } catch (UpStreamProxyNotFoundException e) {
            logger.debug(UNEXPECTED_EXCEPTION, e);
            return null;
        }
    }

    /**
     * 获取上游代理详细信息
     *
     * @param proxySocket 上游代理socket
     * @return 对应上游代理的详细信息
     * @throws UpStreamProxyNotFoundException 如果该socket没有被添加，则抛出该异常
     */
    public UpstreamProxyDetail getDetail(InetSocketAddress proxySocket) throws UpStreamProxyNotFoundException {
        UpstreamProxyDetail value = proxies.get(proxySocket);
        if (value == null) {
            logger.info("获取不存在上游代理 {} 的信息", proxySocket);
            throw new UpStreamProxyNotFoundException();
        }
        return value;
    }

    /**
     * 移除上游代理
     *
     * @param proxySocket 上游代理socket
     * @throws UpStreamProxyNotFoundException 如果该socket没有被添加，则抛出该异常
     */
    public UpstreamProxyDetail remove(InetSocketAddress proxySocket) throws UpStreamProxyNotFoundException {
        UpstreamProxyDetail upstreamProxyDetail = proxies.remove(proxySocket);
        if (upstreamProxyDetail == null) {
            logger.info("移除不存在的上游代理 {} ", proxySocket);
            throw new UpStreamProxyNotFoundException();
        }
        // 修改状态，防止产生野连接
        upstreamProxyDetail.stateLock.writeLock().lock();
        upstreamProxyDetail.removed = true;
        upstreamProxyDetail.stateLock.writeLock().unlock();
        // 中止和该代理相关的所有连接
        upstreamProxyDetail.relevantConnections.forEach(switcher.connectionManager::sureAbort);
        return upstreamProxyDetail;
    }
}
