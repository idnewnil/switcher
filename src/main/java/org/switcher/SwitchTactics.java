package org.switcher;

import java.util.List;

public interface SwitchTactics {
    /**
     * 给定目标uri和可以使用的代理，返回一个排序后的列表
     *
     * @param uri        目标uri
     * @param proxyPairs 可使用的代理
     * @return 排序后的代理
     */
    List<UpstreamProxyPair> getRank(String uri, List<UpstreamProxyPair> proxyPairs);
}
