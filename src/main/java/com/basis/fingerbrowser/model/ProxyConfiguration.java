package com.basis.fingerbrowser.model;

import java.io.Serializable;
/**
 * 代理配置模型类
 */
public class ProxyConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    private ProxyType proxyType = ProxyType.DIRECT;
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean authenticationRequired = false;

    // 无参构造函数
    public ProxyConfiguration() {
    }

    // 带参数的构造函数
    public ProxyConfiguration(ProxyType proxyType, String host, int port) {
        this.proxyType = proxyType;
        this.host = host;
        this.port = port;
    }

    // 带认证信息的构造函数
    public ProxyConfiguration(ProxyType proxyType, String host, int port,
                              String username, String password) {
        this.proxyType = proxyType;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.authenticationRequired = true;
    }
    public ProxyType getProxyType() {
        return proxyType;
    }
    public void setProxyType(ProxyType proxyType) {
        this.proxyType = proxyType;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }
    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    @Override
    public String toString() {
        if (proxyType == ProxyType.DIRECT) {
            return "直接连接 (无代理)";
        }

        String auth = authenticationRequired ? " (需要认证)" : "";
        return proxyType + " " + host + ":" + port + auth;
    }
}
