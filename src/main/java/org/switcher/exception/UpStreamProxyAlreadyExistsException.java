package org.switcher.exception;

import org.switcher.UpstreamProxyDetail;

public class UpStreamProxyAlreadyExistsException extends UpStreamProxyException {
    public final UpstreamProxyDetail upstreamProxyDetail;

    public UpStreamProxyAlreadyExistsException(UpstreamProxyDetail upstreamProxyDetail) {
        this.upstreamProxyDetail = upstreamProxyDetail;
    }
}
