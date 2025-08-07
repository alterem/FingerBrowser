package com.basis.fingerbrowser.model;

import java.io.Serializable;
/**
 * 代理设置模型类
 * 用于存储和管理浏览器代理配置信息
 */
public class ProxySettings implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type = "DIRECT"; // "DIRECT", "HTTP", "SOCKS4", "SOCKS5"
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean requiresAuthentication = false;
    private boolean enabled = true;

    /**
     * 默认构造函数
     */
    public ProxySettings() {
    }

    /**
     * 带参数的构造函数
     *
     * @param type 代理类型
     * @param host 主机地址
     * @param port 端口
     */
    public ProxySettings(String type, String host, int port) {
        this.type = type;
        this.host = host;
        this.port = port;
    }

    /**
     * 带认证信息的构造函数
     *
     * @param type 代理类型
     * @param host 主机地址
     * @param port 端口
     * @param username 用户名
     * @param password 密码
     */
    public ProxySettings(String type, String host, int port, String username, String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.requiresAuthentication = true;
    }
    /**
     * 获取代理类型
     *
     * @return 代理类型（DIRECT, HTTP, SOCKS4, SOCKS5）
     */
    public String getType() {
        return type;
    }
    /**
     * 设置代理类型
     *
     * @param type 代理类型
     */
    public void setType(String type) {
        this.type = type;
    }
    /**
     * 获取代理服务器主机地址
     *
     * @return 主机地址
     */
    public String getHost() {
        return host;
    }
    /**
     * 设置代理服务器主机地址
     *
     * @param host 主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }
    /**
     * 获取代理服务器端口
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }
    /**
     * 设置代理服务器端口
     *
     * @param port 端口号
     */
    public void setPort(int port) {
        this.port = port;
    }
    /**
     * 获取代理认证用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }
    /**
     * 设置代理认证用户名
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * 获取代理认证密码
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }
    /**
     * 设置代理认证密码
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }
    /**
     * 检查代理是否需要认证
     *
     * @return 如果需要认证则返回true
     */
    public boolean isRequiresAuthentication() {
        return requiresAuthentication;
    }
    /**
     * 设置代理是否需要认证
     *
     * @param requiresAuthentication 是否需要认证
     */
    public void setRequiresAuthentication(boolean requiresAuthentication) {
        this.requiresAuthentication = requiresAuthentication;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 检查该代理设置是否有效
     *
     * @return 如果设置有效则返回true
     */
    public boolean isValid() {
        if (type.equals("DIRECT")) {
            return true;
        }

        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        if (port <= 0 || port > 65535) {
            return false;
        }

        if (requiresAuthentication) {
            return username != null && !username.trim().isEmpty()
                    && password != null && !password.trim().isEmpty();
        }

        return true;
    }

    /**
     * 克隆当前代理设置对象
     *
     * @return 新的代理设置实例
     */
    public ProxySettings clone() {
        ProxySettings clone = new ProxySettings();
        clone.type = this.type;
        clone.host = this.host;
        clone.port = this.port;
        clone.username = this.username;
        clone.password = this.password;
        clone.requiresAuthentication = this.requiresAuthentication;
        return clone;
    }

    /**
     * 判断是否为直接连接（无代理）
     *
     * @return 如果是直接连接则返回true
     */
    public boolean isDirect() {
        return "DIRECT".equals(type);
    }

    /**
     * 获取浏览器启动时的代理参数
     *
     * @return 代理参数字符串
     */
    public String getBrowserProxyArgument() {
        if (isDirect()) {
            return null;
        }

        if ("HTTP".equals(type)) {
            return "--proxy-server=http://" + host + ":" + port;
        } else if ("SOCKS4".equals(type)) {
            return "--proxy-server=socks4://" + host + ":" + port;
        } else if ("SOCKS5".equals(type)) {
            return "--proxy-server=socks5://" + host + ":" + port;
        }

        return null;
    }

    /**
     * 构建Java标准代理对象
     *
     * @return Java代理对象
     */
    public java.net.Proxy toJavaProxy() {
        if (isDirect()) {
            return java.net.Proxy.NO_PROXY;
        }

        java.net.InetSocketAddress address = new java.net.InetSocketAddress(host, port);

        if ("HTTP".equals(type)) {
            return new java.net.Proxy(java.net.Proxy.Type.HTTP, address);
        } else if ("SOCKS4".equals(type) || "SOCKS5".equals(type)) {
            return new java.net.Proxy(java.net.Proxy.Type.SOCKS, address);
        }

        return java.net.Proxy.NO_PROXY;
    }

    @Override
    public String toString() {
        if (isDirect()) {
            return "直接连接 (无代理)";
        }

        String auth = requiresAuthentication ? " (需要认证)" : "";
        return type + " " + host + ":" + port + auth;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ProxySettings other = (ProxySettings) obj;

        if (isDirect() && other.isDirect()) {
            return true;
        }

        return port == other.port &&
                requiresAuthentication == other.requiresAuthentication &&
                type.equals(other.type) &&
                (host == null ? other.host == null : host.equals(other.host)) &&
                (username == null ? other.username == null : username.equals(other.username)) &&
                (password == null ? other.password == null : password.equals(other.password));
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (requiresAuthentication ? 1 : 0);
        return result;
    }
}
