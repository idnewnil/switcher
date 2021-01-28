package org.switcher;

/**
 * 禁忌表，记录（代理，服务器）建立连接的失败情况
 * 可以根据禁忌表判断之后的uri是否使用某个代理
 */
public class Tabu {
    private final Switcher switcher;

    public Tabu(Switcher switcher) {
        this.switcher = switcher;
    }
}
