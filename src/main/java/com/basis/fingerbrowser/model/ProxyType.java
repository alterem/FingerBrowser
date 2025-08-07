package com.basis.fingerbrowser.model;

/**
 * 代理类型枚举
 */
public enum ProxyType {
    DIRECT("直接连接"),
    HTTP("HTTP"),
    SOCKS4("SOCKS4"),
    SOCKS5("SOCKS5");

    private final String displayName;

    ProxyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
