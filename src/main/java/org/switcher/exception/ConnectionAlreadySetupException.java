package org.switcher.exception;

import org.switcher.ConnectionDetail;

public class ConnectionAlreadySetupException extends ConnectionException {
    public final ConnectionDetail connectionDetail;

    public ConnectionAlreadySetupException(ConnectionDetail connectionDetail) {
        this.connectionDetail = connectionDetail;
    }
}
