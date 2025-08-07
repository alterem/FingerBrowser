package com.basis.fingerbrowser.service;

import com.basis.fingerbrowser.model.BrowserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BrowserService {

    private static final Logger log = LoggerFactory.getLogger(BrowserService.class);
    private final Map<String, Process> runningBrowsers = new HashMap<>();
    private String baseBrowserPath;
    private final String baseDataDir;

    public BrowserService(String baseBrowserPath, String baseDataDir) {
        this.baseBrowserPath = baseBrowserPath;
        this.baseDataDir = baseDataDir;

        // 确保数据目录存在
        createDirectoryIfNotExists(baseDataDir);
    }
    
    /**
     * 设置浏览器可执行文件路径
     * @param path 路径
     */
    public synchronized void setBrowserExecutablePath(String path) {
        this.baseBrowserPath = path;
    }

    /**
     * 获取基础数据目录
     */
    public String getBaseDataDir() {
        return baseDataDir;
    }

    /**
     * 启动浏览器实例
     */
    public boolean launchBrowser(BrowserProfile profile) {
        try {
            // 如果浏览器已经在运行，则返回
            if (runningBrowsers.containsKey(profile.getId())) {
                log.warn("Profile {} is already running.", profile.getName());
                return true;
            }

            // 准备用户数据目录
            String userDataDir = prepareProfileDirectory(profile);
            profile.setUserDataDir(userDataDir);

            // 准备启动命令
            List<String> command = buildBrowserCommand(profile);
            log.info("Launching browser for profile '{}' with command: {}", profile.getName(), String.join(" ", command));

            // 启动进程
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();

            // 记录启动的浏览器
            runningBrowsers.put(profile.getId(), process);
            profile.setActive(true);
            profile.updateLastUsed();

            // 启动监控线程以检测浏览器关闭
            monitorBrowserProcess(profile, process);

            return true;

        } catch (IOException e) {
            log.error("Failed to launch browser for profile '{}'", profile.getName(), e);
            return false;
        }
    }

    /**
     * 关闭浏览器实例
     */
    public boolean closeBrowser(BrowserProfile profile) {
        Process process = runningBrowsers.get(profile.getId());
        if (process != null) {
            log.info("Closing browser for profile '{}'", profile.getName());
            process.destroy();

            // 等待进程终止
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    log.warn("Browser process for profile '{}' did not terminate gracefully. Forcing.", profile.getName());
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for browser process to close.", e);
                Thread.currentThread().interrupt();
            }

            // 移除记录
            runningBrowsers.remove(profile.getId());
            profile.setActive(false);

            boolean closed = !process.isAlive();
            if(closed) {
                log.info("Browser for profile '{}' closed successfully.", profile.getName());
            } else {
                log.error("Failed to close browser for profile '{}'.", profile.getName());
            }
            return closed;
        }
        log.warn("Attempted to close browser for profile '{}', but it was not running.", profile.getName());
        return true; // 浏览器不在运行中，视为成功关闭
    }

    /**
     * 准备用户配置目录
     */
    private String prepareProfileDirectory(BrowserProfile profile) {
        String profileDir = getBaseDataDir() + File.separator + profile.getId();
        createDirectoryIfNotExists(profileDir);
        log.debug("User profile directory for profile '{}' is: {}", profile.getName(), profileDir);
        return profileDir;
    }

    /**
     * 构建浏览器启动命令
     */
    private List<String> buildBrowserCommand(BrowserProfile profile) {
        List<String> command = new ArrayList<>();

        // 设置浏览器路径
        String browserPath = profile.getBrowserExecutablePath();
        if (browserPath == null || browserPath.isEmpty()) {
            browserPath = baseBrowserPath;
        }
        command.add(browserPath);

        // 用户数据目录
        command.add("--user-data-dir=" + getBaseDataDir());
        command.add("--profile-directory=" + profile.getId());

        // User-Agent
        if (profile.getUserAgent() != null && !profile.getUserAgent().isEmpty()) {
            command.add("--user-agent=" + profile.getUserAgent());
        }

        // 语言
        if (profile.getLanguage() != null && !profile.getLanguage().isEmpty()) {
            command.add("--lang=" + profile.getLanguage().split(",")[0]);
        }

        // Platform (User-Agent is the primary way to influence this)
        if (profile.getPlatform() != null && !profile.getPlatform().isEmpty()) {
            log.debug("Platform is set to '{}' in the profile, but this is primarily influenced by the User-Agent.", profile.getPlatform());
        }

        // Timezone
        if (profile.getTimezone() != null && !profile.getTimezone().isEmpty()) {
            command.add("--force-timezone=" + profile.getTimezone());
        }

        // 分辨率
        if (profile.getResolution() != null && !profile.getResolution().isEmpty()) {
            String[] dims = profile.getResolution().split("x");
            if (dims.length == 2) {
                command.add("--window-size=" + dims[0] + "," + dims[1]);
            }
        }

        // Canvas and Font Fingerprinting (requires extension)
        if (profile.getCanvasFingerprint() != null && !profile.getCanvasFingerprint().isEmpty()) {
            log.warn("Canvas fingerprinting settings are present in the profile but cannot be applied via command-line arguments. An extension is required.");
        }
        if (profile.getFontFingerprint() != null && !profile.getFontFingerprint().isEmpty()) {
            log.warn("Font fingerprinting settings are present in the profile but cannot be applied via command-line arguments. An extension is required.");
        }

        // WebRTC设置
        Map<String, Object> webRTCSettings = profile.getWebRTCSettings();
        if (webRTCSettings != null) {
            if (webRTCSettings.containsKey("ipHandlingPolicy")) {
                command.add("--webrtc-ip-handling-policy=" + webRTCSettings.get("ipHandlingPolicy"));
            }
            if (webRTCSettings.containsKey("enabled") && !(boolean) webRTCSettings.get("enabled")) {
                command.add("--disable-webrtc");
            }
        }

        // 代理设置
        if (profile.getProxySettings() != null && profile.getProxySettings().isEnabled()) {
            String proxyString = buildProxyString(profile);
            if (!proxyString.isEmpty()) {
                command.add("--proxy-server=" + proxyString);
            }
        }

        // 其他Chrome/Chromium特定参数
        command.add("--disable-gpu");
        command.add("--disable-software-rasterizer");
        command.add("--disable-dev-shm-usage");
        command.add("--disable-extensions");
        command.add("--disable-default-apps");
        command.add("--disable-background-networking");
        command.add("--disable-background-timer-throttling");
        command.add("--disable-breakpad");
        command.add("--disable-component-update");
        command.add("--disable-domain-reliability");
        command.add("--disable-features=AudioServiceOutOfProcess");
        command.add("--disable-hang-monitor");
        command.add("--disable-popup-blocking");
        command.add("--disable-prompt-on-repost");
        command.add("--disable-renderer-backgrounding");
        command.add("--safebrowsing-disable-auto-update");

        command.add("--no-first-run");
        command.add("--no-default-browser-check");
        command.add("--disable-sync");

        return command;
    }

    /**
     * 构建代理字符串
     */
    private String buildProxyString(BrowserProfile profile) {
        if (profile.getProxySettings() == null || !profile.getProxySettings().isEnabled()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 代理类型
        String type = profile.getProxySettings().getType().toLowerCase();
        sb.append(type).append("://");

        // 用户名和密码
        if (profile.getProxySettings().getUsername() != null && !profile.getProxySettings().getUsername().isEmpty()) {
            sb.append(profile.getProxySettings().getUsername());
            if (profile.getProxySettings().getPassword() != null) {
                sb.append(":").append(profile.getProxySettings().getPassword());
            }
            sb.append("@");
        }

        // 主机和端口
        sb.append(profile.getProxySettings().getHost())
                .append(":")
                .append(profile.getProxySettings().getPort());

        return sb.toString();
    }

    /**
     * 监控浏览器进程
     */
    private void monitorBrowserProcess(BrowserProfile profile, Process process) {
        new Thread(() -> {
            try {
                // 等待进程结束
                int exitCode = process.waitFor();
                log.info("Browser process for profile '{}' exited with code {}.", profile.getName(), exitCode);
            } catch (InterruptedException e) {
                log.warn("Monitoring thread for profile '{}' was interrupted.", profile.getName());
                Thread.currentThread().interrupt();
            } finally {
                // 进程结束后更新状态
                runningBrowsers.remove(profile.getId());
                profile.setActive(false);
                log.info("Browser " + profile.getName() + " has been marked as closed.");
            }
        }).start();
    }

    /**
     * 检查浏览器是否在运行
     */
    public boolean isBrowserRunning(String profileId) {
        Process process = runningBrowsers.get(profileId);
        return process != null && process.isAlive();
    }

    /**
     * 关闭所有浏览器
     */
    public void closeAllBrowsers() {
        log.info("Closing all running browsers.");
        for (Map.Entry<String, Process> entry : new HashMap<>(runningBrowsers).entrySet()) {
            Process process = entry.getValue();
            if (process != null && process.isAlive()) {
                process.destroy();
                try {
                    if (!process.waitFor(3, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            runningBrowsers.remove(entry.getKey());
        }
        log.info("All browsers closed.");
    }

    private void createDirectoryIfNotExists(String dirPath) {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("Created directory: {}", dirPath);
            } catch (IOException e) {
                log.error("Failed to create directory: {}", dirPath, e);
            }
        }
    }
}
