package org.switcher;

import java.net.InetSocketAddress;

public class ConnectionPair {
    public final InetSocketAddress clientSocket;
    public final ConnectionDetail connectionDetail;

    public ConnectionPair(InetSocketAddress clientSocket, ConnectionDetail connectionDetail) {
        this.clientSocket = clientSocket;
        this.connectionDetail = connectionDetail;
    }
}
