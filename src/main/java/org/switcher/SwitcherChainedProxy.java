package org.switcher;

import org.littleshoot.proxy.ChainedProxyAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.switcher.exception.ConnectionAlreadySetupException;

import java.net.InetSocketAddress;

import static org.switcher.exception.SwitcherException.UNEXPECTED_EXCEPTION;

/**
 * 在原本的 {@link ChainedProxyAdapter} 基础上增加了记录连接信息的功能
 */
class SwitcherChainedProxy extends ChainedProxyAdapter {
    private final static Logger logger = LoggerFactory.getLogger(SwitcherChainedProxy.class);

    private final Switcher switcher;
    private final InetSocketAddress clientSocket;
    private final InetSocketAddress proxySocket;
    private final String uri;

    SwitcherChainedProxy(Switcher switcher, InetSocketAddress clientSocket, InetSocketAddress proxySocket, String uri) {
        this.switcher = switcher;
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
            switcher.connectionManager.add(clientSocket, proxySocket, uri);
        } catch (ConnectionAlreadySetupException e) {
            logger.debug(UNEXPECTED_EXCEPTION, e);
        }
    }

    /**
     * 连接出错时的处理
     *
     * @param cause 出错原因
     */
    @Override
    public void connectionFailed(Throwable cause) {
        // TODO 将这个组合加入禁忌表
    }
}
