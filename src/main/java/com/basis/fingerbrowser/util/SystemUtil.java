package com.basis.fingerbrowser.util;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统工具类
 * 提供系统操作和浏览器进程管理相关的实用方法
 */
public class SystemUtil {

    /**
     * 检测系统类型
     *
     * @return 操作系统名称(" Windows ", " Mac ", " Linux " 等)
     */
    public static String getOSName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "Windows";
        } else if (os.contains("mac")) {
            return "Mac";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "Linux";
        } else {
            return System.getProperty("os.name");
        }
    }

    /**
     * 检测Chrome浏览器可执行文件路径
     *
     * @return Chrome可执行文件的路径，如果没有找到则返回null
     */
    public static String findChromeExecutable() {
        String os = getOSName();

        // 定义可能的Chrome安装路径
        List<String> possiblePaths = new ArrayList<>();

        if ("Windows".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                    System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe"
            ));
        } else if ("Mac".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                    System.getProperty("user.home") + "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
            ));
        } else if ("Linux".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "/usr/bin/google-chrome",
                    "/usr/bin/google-chrome-stable",
                    "/usr/bin/chromium",
                    "/usr/bin/chromium-browser"
            ));
        }

        // 检查这些路径是否存在
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        // 如果上述路径都不存在，尝试通过命令查找Chrome
        try {
            if ("Windows".equals(os)) {
                // 在Windows中通过注册表查找
                String registryOutput = executeCommand("reg", "query",
                        "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe", "/ve");

                if (registryOutput != null && !registryOutput.trim().isEmpty()) {
                    // 从注册表输出中提取路径
                    String[] lines = registryOutput.split("\n");
                    for (String line : lines) {
                        if (line.contains("REG_SZ")) {
                            String path = line.trim().split("REG_SZ")[1].trim();
                            File file = new File(path);
                            if (file.exists() && file.canExecute()) {
                                return file.getAbsolutePath();
                            }
                        }
                    }
                }
            } else if ("Linux".equals(os) || "Mac".equals(os)) {
                // 在Linux或Mac中通过which命令查找
                String chromePathFromWhich = executeCommand("which", "google-chrome");
                if (chromePathFromWhich != null && !chromePathFromWhich.trim().isEmpty()) {
                    File file = new File(chromePathFromWhich.trim());
                    if (file.exists() && file.canExecute()) {
                        return file.getAbsolutePath();
                    }
                }

                // 也尝试查找chromium
                String chromiumPathFromWhich = executeCommand("which", "chromium-browser");
                if (chromiumPathFromWhich != null && !chromiumPathFromWhich.trim().isEmpty()) {
                    File file = new File(chromiumPathFromWhich.trim());
                    if (file.exists() && file.canExecute()) {
                        return file.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("查找Chrome路径时出错: " + e.getMessage());
        }

        return null;
    }

    /**
     * 检测Firefox浏览器可执行文件路径
     *
     * @return Firefox可执行文件的路径，如果没有找到则返回null
     */
    public static String findFirefoxExecutable() {
        String os = getOSName();

        // 定义可能的Firefox安装路径
        List<String> possiblePaths = new ArrayList<>();

        if ("Windows".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "C:\\Program Files\\Mozilla Firefox\\firefox.exe",
                    "C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe"
            ));
        } else if ("Mac".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "/Applications/Firefox.app/Contents/MacOS/firefox",
                    System.getProperty("user.home") + "/Applications/Firefox.app/Contents/MacOS/firefox"
            ));
        } else if ("Linux".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "/usr/bin/firefox",
                    "/usr/local/bin/firefox"
            ));
        }

        // 检查这些路径是否存在
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        // 如果上述路径都不存在，尝试通过命令查找Firefox
        try {
            if ("Windows".equals(os)) {
                // 在Windows中通过注册表查找
                String registryOutput = executeCommand("reg", "query",
                        "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\firefox.exe", "/ve");

                if (registryOutput != null && !registryOutput.trim().isEmpty()) {
                    // 从注册表输出中提取路径
                    String[] lines = registryOutput.split("\n");
                    for (String line : lines) {
                        if (line.contains("REG_SZ")) {
                            String path = line.trim().split("REG_SZ")[1].trim();
                            File file = new File(path);
                            if (file.exists() && file.canExecute()) {
                                return file.getAbsolutePath();
                            }
                        }
                    }
                }
            } else if ("Linux".equals(os) || "Mac".equals(os)) {
                // 在Linux或Mac中通过which命令查找
                String firefoxPath = executeCommand("which", "firefox");
                if (firefoxPath != null && !firefoxPath.trim().isEmpty()) {
                    File file = new File(firefoxPath.trim());
                    if (file.exists() && file.canExecute()) {
                        return file.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("查找Firefox路径时出错: " + e.getMessage());
        }

        return null;
    }

    /**
     * 检测Edge浏览器可执行文件路径
     *
     * @return Edge可执行文件的路径，如果没有找到则返回null
     */
    public static String findEdgeExecutable() {
        String os = getOSName();

        // 定义可能的Edge安装路径
        List<String> possiblePaths = new ArrayList<>();

        if ("Windows".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe"
            ));
        } else if ("Mac".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
                    System.getProperty("user.home") + "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
            ));
        } else if ("Linux".equals(os)) {
            possiblePaths.addAll(Arrays.asList(
                    "/usr/bin/microsoft-edge",
                    "/usr/bin/microsoft-edge-stable"
            ));
        }

        // 检查这些路径是否存在
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        // 如果上述路径都不存在，尝试通过命令查找Edge
        try {
            if ("Windows".equals(os)) {
                // 在Windows中通过注册表查找
                String registryOutput = executeCommand("reg", "query",
                        "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\msedge.exe", "/ve");

                if (registryOutput != null && !registryOutput.trim().isEmpty()) {
                    // 从注册表输出中提取路径
                    String[] lines = registryOutput.split("\n");
                    for (String line : lines) {
                        if (line.contains("REG_SZ")) {
                            String path = line.trim().split("REG_SZ")[1].trim();
                            File file = new File(path);
                            if (file.exists() && file.canExecute()) {
                                return file.getAbsolutePath();
                            }
                        }
                    }
                }
            } else if ("Linux".equals(os) || "Mac".equals(os)) {
                // 在Linux或Mac中通过which命令查找
                String edgePath = executeCommand("which", "microsoft-edge");
                if (edgePath != null && !edgePath.trim().isEmpty()) {
                    File file = new File(edgePath.trim());
                    if (file.exists() && file.canExecute()) {
                        return file.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("查找Edge路径时出错: " + e.getMessage());
        }

        return null;
    }

    /**
     * 创建临时用户数据目录
     *
     * @param profileId 配置文件ID作为目录名
     * @return 创建的用户数据目录路径
     */
    public static String createUserDataDir(String profileId) throws IOException {
        // 在临时目录中创建浏览器用户数据目录
        String tempDir = System.getProperty("java.io.tmpdir");
        String userDataDir = tempDir + File.separator + "finger_browser_" + profileId;

        // 确保目录存在
        File dir = new File(userDataDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("无法创建用户数据目录: " + userDataDir);
            }
        }

        return userDataDir;
    }

    /**
     * 删除用户数据目录
     *
     * @param userDataDir 用户数据目录路径
     * @return 是否成功删除
     */
    public static boolean deleteUserDataDir(String userDataDir) {
        if (userDataDir == null || userDataDir.trim().isEmpty()) {
            return false;
        }

        File dir = new File(userDataDir);
        if (!dir.exists()) {
            return true; // 目录不存在也算成功
        }

        try {
            // 递归删除目录及其内容
            return deleteDirectoryRecursively(dir);
        } catch (IOException e) {
            System.err.println("删除目录时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 递归删除目录及其内容
     *
     * @param directory 要删除的目录
     * @return 是否成功删除
     */
    private static boolean deleteDirectoryRecursively(File directory) throws IOException {
        if (!directory.exists()) {
            return true;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    if (!file.delete()) {
                        System.err.println("无法删除文件: " + file.getAbsolutePath());
                    }
                }
            }
        }

        return directory.delete();
    }

    /**
     * 执行系统命令，并返回输出结果
     *
     * @param command 命令及其参数
     * @return 命令的输出结果
     */
    public static String executeCommand(String... command) {
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // 读取标准输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待进程执行完成
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return null;
            }

            return output.toString();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("执行命令失败: " + String.join(" ", command) + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取进程ID
     *
     * @param process 进程对象
     * @return 进程ID，如果获取失败则返回-1
     */
    public static long getProcessId(Process process) {
        if (process == null) {
            return -1;
        }

        try {
            // Java 9+方法
            return process.pid();
        } catch (UnsupportedOperationException e) {
            // Java 8兼容方法 - 使用反射获取PID
            try {
                // 尝试反射方式获取进程ID
                Class<?> processClass = process.getClass();
                if (processClass.getName().equals("java.lang.ProcessImpl")) {
                    java.lang.reflect.Field pidField = processClass.getDeclaredField("pid");
                    pidField.setAccessible(true);
                    return pidField.getLong(process);
                } else if (processClass.getName().equals("java.lang.Win32Process") ||
                        processClass.getName().equals("java.lang.UNIXProcess")) {
                    java.lang.reflect.Field pidField = processClass.getDeclaredField("pid");
                    pidField.setAccessible(true);
                    return pidField.getInt(process);
                }
            } catch (Exception ex) {
                System.err.println("无法获取进程ID: " + ex.getMessage());
            }
            return -1;
        }
    }

    /**
     * 检查端口是否被占用
     *
     * @param port 端口号
     * @return 如果端口被占用则返回true
     */
    public static boolean isPortInUse(int port) {
        String os = getOSName();
        String command, option;

        if ("Windows".equals(os)) {
            command = "netstat";
            option = "-an";
        } else {
            command = "lsof";
            option = "-i:" + port;
        }

        try {
            String output = executeCommand(command, option);
            return output != null && output.contains(":" + port);
        } catch (Exception e) {
            System.err.println("检查端口时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取可用端口
     *
     * @param startingPort 起始端口号
     * @param range        搜索范围
     * @return 可用的端口号，如果没有找到则返回-1
     */
    public static int findAvailablePort(int startingPort, int range) {
        for (int port = startingPort; port < startingPort + range; port++) {
            if (!isPortInUse(port)) {
                return port;
            }
        }
        return -1;
    }

    /**
     * 终止进程
     *
     * @param pid 进程ID
     * @return 是否成功终止
     */
    public static boolean killProcess(long pid) {
        if (pid <= 0) {
            return false;
        }

        String os = getOSName();
        ProcessBuilder processBuilder;

        try {
            if ("Windows".equals(os)) {
                // Windows 系统使用 taskkill 命令
                processBuilder = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
            } else {
                // Linux/Mac 系统使用 kill 命令
                processBuilder = new ProcessBuilder("kill", "-9", String.valueOf(pid));
            }

            // 启动进程
            Process process = processBuilder.start();

            // 等待进程完成并获取退出码
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.err.println("终止进程失败: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }


    /**
     * 检查进程是否存在
     *
     * @param pid 进程ID
     * @return 如果进程存在则返回true
     */
    public static boolean isProcessRunning(long pid) {
        if (pid <= 0) {
            return false;
        }

        String os = getOSName();

        try {
            if ("Windows".equals(os)) {
                String output = executeCommand("tasklist", "/FI", "PID eq " + pid);
                return output != null && output.contains(String.valueOf(pid));
            } else {
                String output = executeCommand("ps", "-p", String.valueOf(pid));
                return output != null && output.contains(String.valueOf(pid));
            }
        } catch (Exception e) {
            System.err.println("检查进程状态时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取系统可用内存大小（MB）
     *
     * @return 可用内存大小（MB）
     */
    public static long getAvailableMemory() {
        return Runtime.getRuntime().freeMemory() / (1024 * 1024);
    }

    /**
     * 获取系统总内存大小（MB）
     *
     * @return 总内存大小（MB）
     */
    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory() / (1024 * 1024);
    }

    /**
     * 判断系统代理是否已设置
     *
     * @return 如果系统代理已设置则返回true
     */
    public static boolean isSystemProxySet() {
        return System.getProperty("http.proxyHost") != null &&
                !System.getProperty("http.proxyHost").trim().isEmpty();
    }

    /**
     * 获取系统所有字体名称
     *
     * @return 字体名称列表
     */
    public static List<String> getSystemFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = ge.getAllFonts();

        List<String> fontNames = new ArrayList<>();
        for (Font font : fonts) {
            fontNames.add(font.getFontName());
        }

        return fontNames;
    }

    /**
     * 获取系统屏幕分辨率
     *
     * @return 屏幕分辨率字符串，格式为"宽x高"
     */
    public static String getScreenResolution() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return screenSize.width + "x" + screenSize.height;
    }

    /**
     * 获取系统时区ID
     *
     * @return 时区ID
     */
    public static String getSystemTimezone() {
        return java.util.TimeZone.getDefault().getID();
    }

    /**
     * 获取系统语言
     *
     * @return 语言代码
     */
    public static String getSystemLanguage() {
        return java.util.Locale.getDefault().toLanguageTag();
    }

    /**
     * 生成随机的用户代理字符串
     *
     * @return 随机用户代理字符串
     */
    public static String generateRandomUserAgent() {
        // 这里只是示例，实际应该有更复杂的随机UA生成逻辑
        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36"
        };

        int index = (int) (Math.random() * userAgents.length);
        return userAgents[index];
    }
}
