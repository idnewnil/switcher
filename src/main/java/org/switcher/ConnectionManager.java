package org.switcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.switcher.exception.ConnectionAlreadySetupException;
import org.switcher.exception.ConnectionNotFoundException;

import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理连接，根据给定的socket，能返回其详细信息
 */
public class ConnectionManager {
    private final static Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    /**
     * 连接映射，方便通过 {@link InetSocketAddress} 来查找其详细信息
     */
    private final Map<InetSocketAddress, ConnectionDetail> connections;

    private final Switcher switcher;

    ConnectionManager(Switcher switcher) {
        this.switcher = switcher;
        connections = new ConcurrentHashMap<>();
    }

    /**
     * 代理链
     *
     * @param clientSocket 客户端的socket
     * @param proxySocket  上游代理的socket
     * @param uri          目标uri
     * @return 格式化后的代理链
     */
    static String connectionChain(InetSocketAddress clientSocket, InetSocketAddress proxySocket, String uri) {
        String proxyType = proxySocket == UpstreamProxyManager.DIRECT_CONNECTION ? "直接连接" : "代理连接";
        return MessageFormat.format("{0} {1} == {2} => {3} ", proxyType, clientSocket, proxySocket, uri);
    }

    /**
     * 增加新连接
     *
     * @param clientSocket 客户端的socket
     * @param proxySocket  上游代理的socket
     * @param uri          目标uri
     * @throws ConnectionAlreadySetupException 如果连接已存在，则会抛出该错误
     */
    void add(InetSocketAddress clientSocket, InetSocketAddress proxySocket, String uri) throws ConnectionAlreadySetupException {
        // 断言存在该代理
        UpstreamProxyDetail upstreamProxyDetail = switcher.upstreamProxyManager.proxies.get(proxySocket);
        assert upstreamProxyDetail != null;

        // 由于upstreamProxy可能在被获取后又恰好被释放，因此需要获取状态锁
        upstreamProxyDetail.stateLock.readLock().lock();
        if (upstreamProxyDetail.removed) {
            // 如果upstreamProxy恰好被移除了，那么需要中止这一个连接
            // TODO 中止连接
        } else {
            // 用AtomicBoolean并不是为了原子性，可以用new boolean[]{true}来代替
            AtomicBoolean contains = new AtomicBoolean(true);

            // 需要原子性操作，不能换为containsKey+put
            ConnectionDetail connectionDetail = connections.computeIfAbsent(clientSocket, __ -> {
                // 此处如果修改boolean则会报错
                contains.set(false);
                return new ConnectionDetail(proxySocket, uri);
            });

            // 判断是否已经存在该键，如果存在则输出日志并抛出错误，否则打印一条info
            if (contains.get()) {
                String newConnectionChain = connectionChain(clientSocket, proxySocket, uri);
                String existingConnectionChain = connectionChain(clientSocket,
                        connectionDetail.proxySocket, connectionDetail.uri);
                logger.error("尝试增加{}到 {}，但在 {} 中，已存在{}",
                        newConnectionChain, this, this, existingConnectionChain);
                throw new ConnectionAlreadySetupException(connectionDetail);
            } else {
                logger.info("新增{}", connectionChain(clientSocket, proxySocket, uri));
            }
        }
        // 释放锁
        upstreamProxyDetail.stateLock.readLock().unlock();
    }

    /**
     * 获取所有连接的clientSocket
     *
     * @return 所有socket的集合
     */
    public Set<InetSocketAddress> getAll() {
        return new HashSet<>(connections.keySet());
    }

    /**
     * 获取连接详细信息
     *
     * @param clientSocket 客户端的socket
     * @return 连接的详细信息
     * @throws ConnectionNotFoundException 如果连接不存在，则抛出该异常
     */
    public ConnectionDetail getDetail(InetSocketAddress clientSocket) throws ConnectionNotFoundException {
        ConnectionDetail value = connections.get(clientSocket);
        if (value == null) {
            logger.debug("获取不存在连接 {} 的信息", clientSocket);
            throw new ConnectionNotFoundException();
        }
        return value;
    }

    /**
     * 移除连接（成功状态）
     *
     * @param clientSocket 客户端的socket
     * @return 连接的详细信息
     * @throws ConnectionNotFoundException 如果连接不存在，则抛出该异常
     */
    ConnectionDetail remove(InetSocketAddress clientSocket) throws ConnectionNotFoundException {
        ConnectionDetail connectionDetail = connections.remove(clientSocket);
        if (connectionDetail == null) {
            logger.debug("移除不存在的连接 {}", clientSocket);
            throw new ConnectionNotFoundException();
        }
        logger.info("移除连接 {}", clientSocket);
        return connectionDetail;
    }

    /**
     * 中止连接
     *
     * @param clientSocket 客户端的socket
     * @throws ConnectionNotFoundException 如果连接不存在，抛出该异常
     */
    public void abort(InetSocketAddress clientSocket) throws ConnectionNotFoundException {
        logger.info("中止连接 {}", clientSocket);
        ConnectionDetail connectionDetail = remove(clientSocket);
        // TODO 从switcher.upstreamProxyManager.getDetail(connectionDetail.proxySocket).relevantConnections
        //  移除clientSocket
        // TODO 中止连接
    }
}
