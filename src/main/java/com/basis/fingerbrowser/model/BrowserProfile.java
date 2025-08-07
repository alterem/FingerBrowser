package com.basis.fingerbrowser.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BrowserProfile {
    private String id;
    private String name;
    private String userAgent;
    private String platform;
    private Map<String, Object> webRTCSettings;
    private Map<String, Object> canvasFingerprint;
    private Map<String, Object> fontFingerprint;
    private ProxySettings proxySettings;
    private Map<String, String> cookies;
    private Map<String, String> localStorage;
    private Map<String, String> customHeaders;
    private String language;
    private String timezone;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsed;
    private String notes;
    private String browserExecutablePath;
    private String userDataDir;
    private boolean active;
    private ProxyConfiguration proxyConfiguration;

    public BrowserProfile() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.lastUsed = LocalDateTime.now();
        this.webRTCSettings = new HashMap<>();
        this.canvasFingerprint = new HashMap<>();
        this.fontFingerprint = new HashMap<>();
        this.cookies = new HashMap<>();
        this.localStorage = new HashMap<>();
        this.customHeaders = new HashMap<>();
        this.active = false;
    }

    public BrowserProfile(String id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.lastUsed = LocalDateTime.now();
        this.webRTCSettings = new HashMap<>();
        this.canvasFingerprint = new HashMap<>();
        this.fontFingerprint = new HashMap<>();
        this.cookies = new HashMap<>();
        this.localStorage = new HashMap<>();
        this.customHeaders = new HashMap<>();
        this.active = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Map<String, Object> getWebRTCSettings() {
        return webRTCSettings;
    }

    public void setWebRTCSettings(Map<String, Object> webRTCSettings) {
        this.webRTCSettings = webRTCSettings;
    }

    public Map<String, Object> getCanvasFingerprint() {
        return canvasFingerprint;
    }

    public void setCanvasFingerprint(Map<String, Object> canvasFingerprint) {
        this.canvasFingerprint = canvasFingerprint;
    }

    public Map<String, Object> getFontFingerprint() {
        return fontFingerprint;
    }

    public void setFontFingerprint(Map<String, Object> fontFingerprint) {
        this.fontFingerprint = fontFingerprint;
    }

    public ProxySettings getProxySettings() {
        return proxySettings;
    }

    public void setProxySettings(ProxySettings proxySettings) {
        this.proxySettings = proxySettings;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public Map<String, String> getLocalStorage() {
        return localStorage;
    }

    public void setLocalStorage(Map<String, String> localStorage) {
        this.localStorage = localStorage;
    }

    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    public void updateLastUsed() {
        this.lastUsed = LocalDateTime.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getBrowserExecutablePath() {
        return browserExecutablePath;
    }

    public void setBrowserExecutablePath(String browserExecutablePath) {
        this.browserExecutablePath = browserExecutablePath;
    }

    public String getUserDataDir() {
        return userDataDir;
    }

    public void setUserDataDir(String userDataDir) {
        this.userDataDir = userDataDir;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    @Override
    public String toString() {
        return name;
    }
}
