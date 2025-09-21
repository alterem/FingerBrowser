package com.basis.fingerbrowser.util;

import com.basis.fingerbrowser.model.BrowserProfile;
import com.basis.fingerbrowser.model.ProxySettings;

import java.util.*;
public class FingerprintGenerator {
    private static final String[] USER_AGENTS = {
            // Chrome 120+ (最新版本)
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            // Firefox 121+ (最新版本)
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:121.0) Gecko/20100101 Firefox/121.0",
            // Safari (最新版本)
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            // Edge (最新版本)
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
    };
    private static final String[] PLATFORMS = {
            "Windows NT 10.0; Win64; x64",
            "Macintosh; Intel Mac OS X 10_15_7",
            "X11; Linux x86_64",
            "Windows NT 10.0; WOW64",
            "iPhone; CPU iPhone OS 14_6 like Mac OS X"
    };
    private static final String[] LANGUAGES = {
            "en-US,en;q=0.9", "en-GB,en;q=0.9", "zh-CN,zh;q=0.9,en;q=0.8",
            "es-ES,es;q=0.9,en;q=0.8", "fr-FR,fr;q=0.9,en;q=0.8"
    };
    private static final String[] TIMEZONES = {
            "America/New_York", "Europe/London", "Asia/Shanghai",
            "Europe/Berlin", "Asia/Tokyo", "Australia/Sydney"
    };
    private static final String[] RESOLUTIONS = {
            "1920x1080", "1366x768", "2560x1440",
            "1440x900", "1280x800", "3840x2160",
            "2880x1800", "1680x1050", "1536x864", "1600x900"
    };
    private static final Random random = new Random();
    /**
     * 生成随机浏览器配置文件
     */
    public static BrowserProfile generateRandomProfile(String name) {
        BrowserProfile profile = new BrowserProfile();
        profile.setName(name);

        // 基础设置
        profile.setUserAgent(USER_AGENTS[random.nextInt(USER_AGENTS.length)]);
        profile.setPlatform(PLATFORMS[random.nextInt(PLATFORMS.length)]);
        profile.setLanguage(LANGUAGES[random.nextInt(LANGUAGES.length)]);
        profile.setTimezone(TIMEZONES[random.nextInt(TIMEZONES.length)]);
        profile.setResolution(RESOLUTIONS[random.nextInt(RESOLUTIONS.length)]);

        // WebRTC设置
        var webRTCSettings = new com.basis.fingerbrowser.model.WebRTCSettings();
        webRTCSettings.setEnabled(random.nextBoolean());
        webRTCSettings.setIpHandlingPolicy("default_public_interface_only");
        profile.setWebRTCSettings(webRTCSettings);

        // Canvas指纹设置
        var canvasSettings = new com.basis.fingerbrowser.model.CanvasSettings();
        canvasSettings.setNoise(random.nextDouble() * 10);
        canvasSettings.setSpoof(random.nextBoolean());
        profile.setCanvasFingerprint(canvasSettings);

        // 字体指纹
        var fontSettings = new com.basis.fingerbrowser.model.FontSettings();
        fontSettings.setSpoof(random.nextBoolean());
        profile.setFontFingerprint(fontSettings);

        // 配置随机代理
        if(random.nextBoolean()) {
            profile.setProxySettings(generateRandomProxy());
        }

        return profile;
    }

    /**
     * 生成随机代理设置
     */
    public static ProxySettings generateRandomProxy() {
        String[] types = {"HTTP", "SOCKS5"};
        String type = types[random.nextInt(types.length)];

        // 这里只是示例IP，实际应用需要有效的代理服务器
        String host = "proxy" + (random.nextInt(10) + 1) + ".example.com";
        int port = 8000 + random.nextInt(2000);

        ProxySettings proxy = new ProxySettings(type, host, port);

        // 随机添加认证
        if(random.nextBoolean()) {
            proxy.setUsername("user" + random.nextInt(100));
            proxy.setPassword("pass" + random.nextInt(100));
        }

        return proxy;
    }

    /**
     * 基于已有配置文件生成修改版本
     */
    public static BrowserProfile deriveProfile(BrowserProfile base, String newName) {
        BrowserProfile derived = new BrowserProfile();

        derived.setName(newName);
        derived.setUserAgent(base.getUserAgent());
        derived.setPlatform(base.getPlatform());
        derived.setLanguage(base.getLanguage());
        derived.setTimezone(base.getTimezone());
        derived.setResolution(base.getResolution());

        // 稍微修改一下Canvas指纹
        var canvasSettings2 = new com.basis.fingerbrowser.model.CanvasSettings();
        if (base.getCanvasFingerprint() != null) {
            canvasSettings2.setNoise(base.getCanvasFingerprint().getNoise() + (random.nextDouble() - 0.5));
            canvasSettings2.setSpoof(base.getCanvasFingerprint().isSpoof());
        }
        derived.setCanvasFingerprint(canvasSettings2);

        // 可以选择使用相同的代理或更换
        if (base.getProxySettings() != null && random.nextBoolean()) {
            derived.setProxySettings(base.getProxySettings());
        } else {
            derived.setProxySettings(generateRandomProxy());
        }

        return derived;
    }
}
