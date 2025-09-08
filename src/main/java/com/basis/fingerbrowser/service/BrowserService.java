package com.basis.fingerbrowser.service;

import com.basis.fingerbrowser.model.BrowserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BrowserService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(BrowserService.class);
    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final int PROCESS_TERMINATION_TIMEOUT_SECONDS = 5;
    private static final int FORCE_TERMINATION_TIMEOUT_SECONDS = 2;
    
    private final Map<String, Process> runningBrowsers = new ConcurrentHashMap<>();
    private final ExecutorService monitoringExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "browser-monitor");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    
    private volatile String baseBrowserPath;
    private final String baseDataDir;

    public BrowserService(String baseBrowserPath, String baseDataDir) {
        this.baseBrowserPath = baseBrowserPath;
        this.baseDataDir = baseDataDir;

        // 确保数据目录存在
        try {
            createDirectoryIfNotExists(baseDataDir);
        } catch (IOException e) {
            log.error("Failed to create base data directory: {}", baseDataDir, e);
            throw new RuntimeException("Cannot initialize BrowserService: " + e.getMessage(), e);
        }
    }
    
    /**
     * 设置浏览器可执行文件路径
     * @param path 路径
     * @throws IllegalArgumentException 如果路径无效
     */
    public synchronized void setBrowserExecutablePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Browser executable path cannot be null or empty");
        }
        
        File browserFile = new File(path);
        if (!browserFile.exists() || !browserFile.isFile() || !browserFile.canExecute()) {
            throw new IllegalArgumentException("Invalid browser executable path: " + path);
        }
        
        this.baseBrowserPath = path;
        log.info("Browser executable path updated to: {}", path);
    }

    /**
     * 获取基础数据目录
     */
    public String getBaseDataDir() {
        return baseDataDir;
    }

    /**
     * 启动浏览器实例
     * @param profile 浏览器配置文件
     * @return 是否启动成功
     * @throws IllegalArgumentException 如果配置文件无效
     * @throws IllegalStateException 如果服务已关闭
     */
    public boolean launchBrowser(BrowserProfile profile) {
        validateProfile(profile);
        
        if (isShutdown.get()) {
            throw new IllegalStateException("Browser service has been shutdown");
        }
        
        synchronized (this) {
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
                builder.redirectErrorStream(true);
                Process process = builder.start();

                // 验证进程启动成功
                try {
                    Thread.sleep(2000); // 增加等待时间到2秒，确保进程稳定启动
                    if (!process.isAlive()) {
                        int exitCode = process.exitValue();
                        log.error("Browser process for profile '{}' exited immediately with code {}. Command was: {}", 
                                profile.getName(), exitCode, String.join(" ", command));
                        
                        // 记录标准输出和错误输出以帮助调试
                        try (var scanner = new java.util.Scanner(process.getInputStream())) {
                            if (scanner.hasNext()) {
                                log.error("Process stdout: {}", scanner.useDelimiter("\\A").next());
                            }
                        }
                        return false;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while validating browser startup");
                }

                // 记录启动的浏览器
                runningBrowsers.put(profile.getId(), process);
                profile.setActive(true);
                profile.updateLastUsed();

                // 启动监控线程以检测浏览器关闭
                monitorBrowserProcess(profile, process);

                log.info("Successfully launched browser for profile '{}'", profile.getName());
                return true;

            } catch (IOException e) {
                log.error("Failed to launch browser for profile '{}'", profile.getName(), e);
                return false;
            }
        }
    }

    /**
     * 关闭浏览器实例
     * @param profile 浏览器配置文件
     * @return 是否关闭成功
     * @throws IllegalArgumentException 如果配置文件为空
     */
    public boolean closeBrowser(BrowserProfile profile) {
        if (profile == null || profile.getId() == null) {
            throw new IllegalArgumentException("Profile and profile ID cannot be null");
        }
        
        return closeBrowserById(profile.getId(), profile.getName());
    }
    
    /**
     * 根据ID关闭浏览器实例
     */
    private boolean closeBrowserById(String profileId, String profileName) {
        Process process = runningBrowsers.get(profileId);
        if (process == null) {
            log.warn("Attempted to close browser for profile '{}', but it was not running.", profileName);
            return true; // 浏览器不在运行中，视为成功关闭
        }
        
        log.info("Closing browser for profile '{}'", profileName);
        
        try {
            // 优雅关闭
            process.destroy();
            
            if (!process.waitFor(PROCESS_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Browser process for profile '{}' did not terminate gracefully. Forcing.", profileName);
                process.destroyForcibly();
                
                if (!process.waitFor(FORCE_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    log.error("Failed to forcibly terminate browser process for profile '{}'", profileName);
                }
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for browser process to close.", e);
            Thread.currentThread().interrupt();
            // 强制终止进程
            process.destroyForcibly();
        } finally {
            // 移除记录
            runningBrowsers.remove(profileId);
        }
        
        boolean closed = !process.isAlive();
        if (closed) {
            log.info("Browser for profile '{}' closed successfully.", profileName);
        } else {
            log.error("Failed to close browser for profile '{}'.", profileName);
        }
        return closed;
    }

    /**
     * 准备用户配置目录
     */
    private String prepareProfileDirectory(BrowserProfile profile) throws IOException {
        String profileDir = getBaseDataDir() + File.separator + sanitizeProfileId(profile.getId());
        createDirectoryIfNotExists(profileDir);
        log.debug("User profile directory for profile '{}' is: {}", profile.getName(), profileDir);
        return profileDir;
    }
    
    /**
     * 清理配置文件ID，移除可能的危险字符
     */
    private String sanitizeProfileId(String profileId) {
        if (profileId == null || profileId.trim().isEmpty()) {
            throw new IllegalArgumentException("Profile ID cannot be null or empty");
        }
        
        // 移除路径分隔符和其他危险字符
        return profileId.replaceAll("[/\\:*?\"<>|]", "_").trim();
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

        // 用户数据目录 - 使用完整路径避免配置冲突
        String profileDataDir = getBaseDataDir() + File.separator + sanitizeProfileId(profile.getId());
        command.add("--user-data-dir=" + profileDataDir);

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
            // 只有在明确禁用时才添加禁用参数
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

        // 基础Chrome参数 - 保持兼容性但不过度限制
        command.add("--no-first-run");
        command.add("--no-default-browser-check");
        command.add("--disable-sync");
        command.add("--disable-default-apps");
        command.add("--disable-extensions");
        command.add("--disable-component-update");
        command.add("--disable-background-networking");
        
        // 安全相关参数
        command.add("--disable-features=VizDisplayCompositor");
        command.add("--disable-ipc-flooding-protection");
        
        // 性能优化参数
        command.add("--max_old_space_size=4096");
        command.add("--disable-renderer-backgrounding");

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
        if (isShutdown.get()) {
            return;
        }
        
        monitoringExecutor.submit(() -> {
            try {
                // 等待进程结束
                int exitCode = process.waitFor();
                log.info("Browser process for profile '{}' exited with code {}.", profile.getName(), exitCode);
            } catch (InterruptedException e) {
                if (!isShutdown.get()) {
                    log.warn("Monitoring thread for profile '{}' was interrupted.", profile.getName());
                }
                Thread.currentThread().interrupt();
            } finally {
                // 进程结束后更新状态
                runningBrowsers.remove(profile.getId());
                profile.setActive(false);
                log.info("Browser '{}' has been marked as closed.", profile.getName());
            }
        });
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
        
        // 创建副本避免并发修改
        Map<String, Process> browsersCopy = new HashMap<>(runningBrowsers);
        
        CompletableFuture<Void>[] closeFutures = browsersCopy.entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(() -> {
                Process process = entry.getValue();
                if (process != null && process.isAlive()) {
                    try {
                        process.destroy();
                        if (!process.waitFor(3, TimeUnit.SECONDS)) {
                            log.warn("Forcibly terminating browser process {}", entry.getKey());
                            process.destroyForcibly();
                            process.waitFor(2, TimeUnit.SECONDS);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        process.destroyForcibly();
                    }
                }
                runningBrowsers.remove(entry.getKey());
            }, monitoringExecutor))
            .toArray(CompletableFuture[]::new);
            
        try {
            CompletableFuture.allOf(closeFutures)
                .get(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Some browsers may not have closed properly", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("All browsers closed.");
    }

    private void createDirectoryIfNotExists(String dirPath) throws IOException {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Directory path cannot be null or empty");
        }
        
        Path path = Paths.get(dirPath).toAbsolutePath().normalize();
        
        // 安全检查：确保路径在基础数据目录内
        Path baseDir = Paths.get(baseDataDir).toAbsolutePath().normalize();
        if (!path.startsWith(baseDir)) {
            throw new SecurityException("Directory path must be within base data directory: " + dirPath);
        }
        
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("Created directory: {}", dirPath);
            } catch (IOException e) {
                log.error("Failed to create directory: {}", dirPath, e);
                throw e;
            }
        }
    }
    
    /**
     * 验证浏览器配置文件
     */
    private void validateProfile(BrowserProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Browser profile cannot be null");
        }
        if (profile.getId() == null || profile.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Profile ID cannot be null or empty");
        }
        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Profile name cannot be null or empty");
        }
    }
    
    /**
     * 获取正在运行的浏览器数量
     */
    public int getRunningBrowserCount() {
        return runningBrowsers.size();
    }
    
    /**
     * 获取所有正在运行的浏览器ID列表
     */
    public Set<String> getRunningBrowserIds() {
        return new HashSet<>(runningBrowsers.keySet());
    }
    
    @Override
    public void close() {
        if (!isShutdown.compareAndSet(false, true)) {
            return; // 已经关闭
        }
        
        log.info("Shutting down BrowserService...");
        
        try {
            // 关闭所有浏览器
            closeAllBrowsers();
            
            // 关闭线程池
            monitoringExecutor.shutdown();
            if (!monitoringExecutor.awaitTermination(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Monitoring executor did not terminate gracefully, forcing shutdown");
                monitoringExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            monitoringExecutor.shutdownNow();
        }
        
        log.info("BrowserService shutdown complete");
    }
}
