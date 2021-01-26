package org.switcher;

import org.switcher.exception.ConnectionAlreadySetupException;
import org.switcher.exception.ConnectionNotFoundException;
import org.switcher.exception.ProxyNotFoundException;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理连接，根据给定的socket，能返回其详细信息
 */
public class ConnectionManager {
    /**
     * 连接映射，方便通过 {@link InetSocketAddress} 来查找其详细信息
     */
    private final Map<InetSocketAddress, ConnectionDetail> connections;

    ConnectionManager() {
        connections = new ConcurrentHashMap<>();
    }

    /**
     * 增加新连接
     *
     * @param localSocket 来自本机或局域网的连接的socket
     * @param proxySocket 上游代理的socket
     * @throws ConnectionAlreadySetupException 如果连接已存在，则会抛出该错误
     */
    public void add(InetSocketAddress localSocket, InetSocketAddress proxySocket) throws ConnectionAlreadySetupException {
        AtomicBoolean contains = new AtomicBoolean(true);

        // 需要原子性操作，不能换为containsKey+put
        connections.computeIfAbsent(localSocket, __ -> {
            contains.set(false);
            return new ConnectionDetail(proxySocket);
        });

        if (contains.get()) {
            throw new ConnectionAlreadySetupException();
        }
    }

    /**
     * 获取所有连接的localSocket
     *
     * @return 所有socket的集合
     */
    public Set<InetSocketAddress> getAllSockets() {
        return connections.keySet();
    }

    /**
     * 获取连接详细信息
     *
     * @param localSocket 连接的socket
     * @return 连接的详细信息
     * @throws ConnectionNotFoundException 如果连接不存在，则抛出该异常
     */
    public ConnectionDetail getDetail(InetSocketAddress localSocket) throws ConnectionNotFoundException {
        ConnectionDetail value = connections.get(localSocket);
        if (value == null) {
            throw new ConnectionNotFoundException();
        }
        return value;
    }

    /**
     * 移除连接（成功状态）
     *
     * @param localSocket 连接的socket
     * @return 连接的详细信息
     * @throws ConnectionNotFoundException 如果连接不存在，则抛出该异常
     */
    public ConnectionDetail remove(InetSocketAddress localSocket) throws ConnectionNotFoundException {
        ConnectionDetail connectionDetail = connections.remove(localSocket);
        if (connectionDetail == null) {
            throw new ConnectionNotFoundException();
        }
        return connectionDetail;
    }

    /**
     * 中止连接
     *
     * @param localSocket 连接的socket
     * @throws ConnectionNotFoundException 如果连接不存在，抛出该异常
     */
    public void abort(InetSocketAddress localSocket) throws ConnectionNotFoundException {
        ConnectionDetail connectionDetail = remove(localSocket);
        // TODO 关闭连接
    }
}
