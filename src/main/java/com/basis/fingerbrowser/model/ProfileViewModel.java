package com.basis.fingerbrowser.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.format.DateTimeFormatter;

/**
 * ViewModel for the BrowserProfile, providing JavaFX properties for data binding.
 */
public class ProfileViewModel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty createdAt = new SimpleStringProperty();
    private final StringProperty userAgent = new SimpleStringProperty();
    private final StringProperty platform = new SimpleStringProperty();
    private final StringProperty language = new SimpleStringProperty();
    private final StringProperty timezone = new SimpleStringProperty();
    private final StringProperty resolution = new SimpleStringProperty();
    private final StringProperty webrtcStatus = new SimpleStringProperty();
    private final StringProperty canvasStatus = new SimpleStringProperty();
    private final StringProperty fontStatus = new SimpleStringProperty();
    private final StringProperty proxyType = new SimpleStringProperty();
    private final StringProperty proxyHost = new SimpleStringProperty();
    private final StringProperty proxyPort = new SimpleStringProperty();
    private final StringProperty proxyAuth = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();

    private BrowserProfile profile;

    public void setProfile(BrowserProfile profile) {
        this.profile = profile;
        updateProperties();
    }

    private void updateProperties() {
        if (profile == null) {
            clearProperties();
            return;
        }

        name.set(profile.getName());
        createdAt.set(profile.getCreatedAt().format(DATE_FORMATTER));
        userAgent.set(profile.getUserAgent());
        platform.set(profile.getPlatform());
        language.set(profile.getLanguage());
        timezone.set(profile.getTimezone());
        resolution.set(profile.getResolution());
        notes.set(profile.getNotes());

        // WebRTC
        if (profile.getWebRTCSettings() != null) {
            boolean enabled = profile.getWebRTCSettings().containsKey("enabled") && (boolean) profile.getWebRTCSettings().get("enabled");
            webrtcStatus.set(enabled ? "已保护" : "未保护");
        } else {
            webrtcStatus.set("未配置");
        }

        // Canvas
        if (profile.getCanvasFingerprint() != null) {
            boolean spoof = profile.getCanvasFingerprint().containsKey("spoof") && (boolean) profile.getCanvasFingerprint().get("spoof");
            canvasStatus.set(spoof ? "已保护" : "未保护");
        } else {
            canvasStatus.set("未配置");
        }

        // Font
        if (profile.getFontFingerprint() != null) {
            boolean spoof = profile.getFontFingerprint().containsKey("spoof") && (boolean) profile.getFontFingerprint().get("spoof");
            fontStatus.set(spoof ? "已保护" : "未保护");
        } else {
            fontStatus.set("未配置");
        }

        // Proxy
        if (profile.getProxySettings() != null && profile.getProxySettings().isEnabled()) {
            proxyType.set(profile.getProxySettings().getType());
            proxyHost.set(profile.getProxySettings().getHost());
            proxyPort.set(String.valueOf(profile.getProxySettings().getPort()));
            boolean hasAuth = profile.getProxySettings().getUsername() != null && !profile.getProxySettings().getUsername().isEmpty();
            proxyAuth.set(hasAuth ? "是" : "否");
        } else {
            proxyType.set("未使用");
            proxyHost.set("");
            proxyPort.set("");
            proxyAuth.set("");
        }
    }

    private void clearProperties() {
        name.set("");
        createdAt.set("");
        userAgent.set("");
        platform.set("");
        language.set("");
        timezone.set("");
        resolution.set("");
        webrtcStatus.set("");
        canvasStatus.set("");
        fontStatus.set("");
        proxyType.set("");
        proxyHost.set("");
        proxyPort.set("");
        proxyAuth.set("");
        notes.set("");
    }

    // --- Getters for JavaFX properties ---

    public StringProperty nameProperty() { return name; }
    public StringProperty createdAtProperty() { return createdAt; }
    public StringProperty userAgentProperty() { return userAgent; }
    public StringProperty platformProperty() { return platform; }
    public StringProperty languageProperty() { return language; }
    public StringProperty timezoneProperty() { return timezone; }
    public StringProperty resolutionProperty() { return resolution; }
    public StringProperty webrtcStatusProperty() { return webrtcStatus; }
    public StringProperty canvasStatusProperty() { return canvasStatus; }
    public StringProperty fontStatusProperty() { return fontStatus; }
    public StringProperty proxyTypeProperty() { return proxyType; }
    public StringProperty proxyHostProperty() { return proxyHost; }
    public StringProperty proxyPortProperty() { return proxyPort; }
    public StringProperty proxyAuthProperty() { return proxyAuth; }
    public StringProperty notesProperty() { return notes; }
}
