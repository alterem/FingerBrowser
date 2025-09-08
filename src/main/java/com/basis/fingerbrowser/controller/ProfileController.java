package com.basis.fingerbrowser.controller;

import com.basis.fingerbrowser.model.BrowserProfile;
import com.basis.fingerbrowser.model.ProxySettings;
import com.basis.fingerbrowser.service.ProfileManagerService;
import com.basis.fingerbrowser.service.ThemeService;
import com.basis.fingerbrowser.util.FingerprintGenerator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ProfileController {
    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> userAgentCombo;
    @FXML
    private ComboBox<String> platformCombo;
    @FXML
    private ComboBox<String> languageCombo;
    @FXML
    private ComboBox<String> timezoneCombo;
    @FXML
    private ComboBox<String> resolutionCombo;
    @FXML
    private TextField browserPathField;

    @FXML
    private CheckBox webrtcEnabledCheckbox;
    @FXML
    private ComboBox<String> webrtcPolicyCombo;
    @FXML
    private CheckBox canvasSpoofCheckbox;
    @FXML
    private Slider canvasNoiseSlider;
    @FXML
    private CheckBox fontSpoofCheckbox;
    @FXML
    private CheckBox webglCheckbox;
    @FXML
    private CheckBox audioCheckbox;

    @FXML
    private CheckBox proxyEnabledCheckbox;
    @FXML
    private ComboBox<String> proxyTypeCombo;
    @FXML
    private TextField proxyHostField;
    @FXML
    private TextField proxyPortField;
    @FXML
    private CheckBox proxyAuthCheckbox;
    @FXML
    private TextField proxyUsernameField;
    @FXML
    private PasswordField proxyPasswordField;

    @FXML
    private TextArea notesArea;

    private ProfileManagerService profileManager;
    private BrowserProfile profile;

    public void setProfileManager(ProfileManagerService profileManager) {
        this.profileManager = profileManager;
    }

    public void setProfile(BrowserProfile profile) {
        this.profile = profile;
        initializeFields();
    }

    @FXML
    public void initialize() {
        // 设置主题支持
        setupTheme();
        
        // 初始化下拉菜单选项

        // User-Agent 选项
        userAgentCombo.setItems(FXCollections.observableArrayList(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.59"
        ));

        // 平台选项
        platformCombo.setItems(FXCollections.observableArrayList(
                "Windows NT 10.0; Win64; x64",
                "Macintosh; Intel Mac OS X 10_15_7",
                "X11; Linux x86_64",
                "Windows NT 10.0; WOW64",
                "iPhone; CPU iPhone OS 14_6 like Mac OS X"
        ));

        // 语言选项
        languageCombo.setItems(FXCollections.observableArrayList(
                "en-US,en;q=0.9",
                "en-GB,en;q=0.9",
                "zh-CN,zh;q=0.9,en;q=0.8",
                "es-ES,es;q=0.9,en;q=0.8",
                "fr-FR,fr;q=0.9,en;q=0.8"
        ));

        // 时区选项
        timezoneCombo.setItems(FXCollections.observableArrayList(
                "America/New_York",
                "Europe/London",
                "Asia/Shanghai",
                "Europe/Berlin",
                "Asia/Tokyo",
                "Australia/Sydney"
        ));

        // 分辨率选项
        resolutionCombo.setItems(FXCollections.observableArrayList(
                "1920x1080",
                "1366x768",
                "2560x1440",
                "1440x900",
                "1280x800",
                "3840x2160"
        ));

        // WebRTC 策略选项
        webrtcPolicyCombo.setItems(FXCollections.observableArrayList(
                "default",
                "default_public_and_private_interfaces",
                "default_public_interface_only",
                "disable_non_proxied_udp"
        ));
        webrtcPolicyCombo.setValue("default_public_interface_only");

        // 代理类型选项
        proxyTypeCombo.setItems(FXCollections.observableArrayList(
                "HTTP",
                "SOCKS5"
        ));
        proxyTypeCombo.setValue("HTTP");
    }

    private void initializeFields() {
        if (profile == null) {
            return;
        }

        // 基本信息
        nameField.setText(profile.getName());
        userAgentCombo.setValue(profile.getUserAgent());
        platformCombo.setValue(profile.getPlatform());
        languageCombo.setValue(profile.getLanguage());
        timezoneCombo.setValue(profile.getTimezone());
        resolutionCombo.setValue(profile.getResolution());
        browserPathField.setText(profile.getBrowserExecutablePath());

        // WebRTC 设置
        if (profile.getWebRTCSettings() != null) {
            Map<String, Object> webRTCSettings = profile.getWebRTCSettings();
            webrtcEnabledCheckbox.setSelected(!webRTCSettings.containsKey("enabled") || (boolean) webRTCSettings.get("enabled"));

            if (webRTCSettings.containsKey("ipHandlingPolicy")) {
                webrtcPolicyCombo.setValue((String) webRTCSettings.get("ipHandlingPolicy"));
            }
        }

        // Canvas 设置
        if (profile.getCanvasFingerprint() != null) {
            Map<String, Object> canvasSettings = profile.getCanvasFingerprint();
            canvasSpoofCheckbox.setSelected(!canvasSettings.containsKey("spoof") || (boolean) canvasSettings.get("spoof"));

            if (canvasSettings.containsKey("noise")) {
                canvasNoiseSlider.setValue((double) canvasSettings.get("noise"));
            }
        }

        // 字体设置
        if (profile.getFontFingerprint() != null) {
            Map<String, Object> fontSettings = profile.getFontFingerprint();
            fontSpoofCheckbox.setSelected(!fontSettings.containsKey("spoof") || (boolean) fontSettings.get("spoof"));
        }

        // 代理设置
        if (profile.getProxySettings() != null) {
            ProxySettings proxySettings = profile.getProxySettings();
            proxyEnabledCheckbox.setSelected(proxySettings.isEnabled());

            if (proxySettings.isEnabled()) {
                proxyTypeCombo.setValue(proxySettings.getType());
                proxyHostField.setText(proxySettings.getHost());
                proxyPortField.setText(String.valueOf(proxySettings.getPort()));

                boolean hasAuth = proxySettings.getUsername() != null &&
                        !proxySettings.getUsername().isEmpty();
                proxyAuthCheckbox.setSelected(hasAuth);

                if (hasAuth) {
                    proxyUsernameField.setText(proxySettings.getUsername());
                    proxyPasswordField.setText(proxySettings.getPassword());
                }
            }
        }

        // 注释
        if (profile.getNotes() != null) {
            notesArea.setText(profile.getNotes());
        }
    }

    @FXML
    private void handleProxyEnabledChange() {
        boolean enabled = proxyEnabledCheckbox.isSelected();
        proxyTypeCombo.setDisable(!enabled);
        proxyHostField.setDisable(!enabled);
        proxyPortField.setDisable(!enabled);
        proxyAuthCheckbox.setDisable(!enabled);
    }

    @FXML
    private void handleProxyAuthChange() {
        /*boolean authEnabled = proxyAuthCheckbox.isSelected();
        proxyUsernameField.setDisable(!authEnabled);
        proxyPasswordField.setDisable(!authEnabled);*/
    }

    @FXML
    private void handleTestProxy() {
        if (!proxyEnabledCheckbox.isSelected() ||
                proxyHostField.getText().isEmpty() ||
                proxyPortField.getText().isEmpty()) {
            showAlert("错误", "请先输入有效的代理设置");
            return;
        }

        String host = proxyHostField.getText();
        int port;

        try {
            port = Integer.parseInt(proxyPortField.getText());
        } catch (NumberFormatException e) {
            showAlert("错误", "代理端口必须是有效的数字");
            return;
        }

        String type = proxyTypeCombo.getValue();
        if (type == null) {
            type = "HTTP";
        }

        // 创建代理
        Proxy.Type proxyType = type.equalsIgnoreCase("SOCKS5") ?
                Proxy.Type.SOCKS : Proxy.Type.HTTP;

        Proxy proxy = new Proxy(proxyType, new InetSocketAddress(host, port));

        // 使用新线程测试代理
        new Thread(() -> {
            try {
                // 显示进度对话框
                showTestingProxyDialog();

                // 尝试连接一个网站
                URI uri = new URI("https://www.google.com");
                URL url = uri.toURL();

                HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.connect();

                int responseCode = connection.getResponseCode();

                if (responseCode >= 200 && responseCode < 400) {
                    showAlert("成功", "代理连接测试成功！");
                } else {
                    showAlert("警告", "代理连接返回了非成功状态码: " + responseCode);
                }

            } catch (Exception e) {
                showAlert("错误", "代理连接测试失败: " + e.getMessage());
            }
        }).start();
    }

    private void showTestingProxyDialog() {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("测试中");
            alert.setHeaderText(null);
            alert.setContentText("正在测试代理连接，请稍候...");

            // 显示对话框，但不阻塞调用线程
            alert.show();

            // 3秒后自动关闭对话框
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(alert::close);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    @FXML
    private void handleBrowseBrowserPath() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择浏览器可执行文件");

        // 设置过滤器
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("可执行文件", "*.exe")
            );
        }

        File file = fileChooser.showOpenDialog(nameField.getScene().getWindow());
        if (file != null) {
            browserPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleRandomizeSettings() {
        // 生成随机配置
        BrowserProfile randomProfile = FingerprintGenerator.generateRandomProfile(nameField.getText());

        // 更新界面
        userAgentCombo.setValue(randomProfile.getUserAgent());
        platformCombo.setValue(randomProfile.getPlatform());
        languageCombo.setValue(randomProfile.getLanguage());
        timezoneCombo.setValue(randomProfile.getTimezone());
        resolutionCombo.setValue(randomProfile.getResolution());

        // 更新WebRTC设置
        if (randomProfile.getWebRTCSettings() != null) {
            Map<String, Object> webRTCSettings = randomProfile.getWebRTCSettings();
            webrtcEnabledCheckbox.setSelected(!webRTCSettings.containsKey("enabled") || (boolean) webRTCSettings.get("enabled"));

            if (webRTCSettings.containsKey("ipHandlingPolicy")) {
                webrtcPolicyCombo.setValue((String) webRTCSettings.get("ipHandlingPolicy"));
            }
        }

        // 更新Canvas设置
        if (randomProfile.getCanvasFingerprint() != null) {
            Map<String, Object> canvasSettings = randomProfile.getCanvasFingerprint();
            canvasSpoofCheckbox.setSelected(!canvasSettings.containsKey("spoof") || (boolean) canvasSettings.get("spoof"));

            if (canvasSettings.containsKey("noise")) {
                canvasNoiseSlider.setValue((double) canvasSettings.get("noise"));
            }
        }

        // 更新代理设置
        if (randomProfile.getProxySettings() != null) {
            ProxySettings proxySettings = randomProfile.getProxySettings();
            proxyEnabledCheckbox.setSelected(proxySettings.isEnabled());

            if (proxySettings.isEnabled()) {
                proxyTypeCombo.setValue(proxySettings.getType());
                proxyHostField.setText(proxySettings.getHost());
                proxyPortField.setText(String.valueOf(proxySettings.getPort()));

                boolean hasAuth = proxySettings.getUsername() != null &&
                        !proxySettings.getUsername().isEmpty();
                proxyAuthCheckbox.setSelected(hasAuth);

                if (hasAuth) {
                    proxyUsernameField.setText(proxySettings.getUsername());
                    proxyPasswordField.setText(proxySettings.getPassword());
                }
            }

            // 更新UI状态
            handleProxyEnabledChange();
        }
    }

    @FXML
    private void handleSave() {
        if (nameField.getText().isEmpty()) {
            showAlert("错误", "请输入配置名称");
            return;
        }

        // 更新基本信息
        profile.setName(nameField.getText());
        profile.setUserAgent(userAgentCombo.getValue());
        profile.setPlatform(platformCombo.getValue());
        profile.setLanguage(languageCombo.getValue());
        profile.setTimezone(timezoneCombo.getValue());
        profile.setResolution(resolutionCombo.getValue());
        profile.setBrowserExecutablePath(browserPathField.getText());

        // 更新WebRTC设置
        Map<String, Object> webRTCSettings = new HashMap<>();
        webRTCSettings.put("enabled", webrtcEnabledCheckbox.isSelected());
        webRTCSettings.put("ipHandlingPolicy", webrtcPolicyCombo.getValue());
        profile.setWebRTCSettings(webRTCSettings);

        // 更新Canvas设置
        Map<String, Object> canvasSettings = new HashMap<>();
        canvasSettings.put("spoof", canvasSpoofCheckbox.isSelected());
        canvasSettings.put("noise", canvasNoiseSlider.getValue());
        profile.setCanvasFingerprint(canvasSettings);

        // 更新字体设置
        Map<String, Object> fontSettings = new HashMap<>();
        fontSettings.put("spoof", fontSpoofCheckbox.isSelected());
        profile.setFontFingerprint(fontSettings);

        // 更新代理设置
        if (proxyEnabledCheckbox.isSelected()) {
            ProxySettings proxySettings = new ProxySettings();
            proxySettings.setEnabled(true);
            proxySettings.setType(proxyTypeCombo.getValue());
            proxySettings.setHost(proxyHostField.getText());

            try {
                proxySettings.setPort(Integer.parseInt(proxyPortField.getText()));
            } catch (NumberFormatException e) {
                showAlert("错误", "代理端口必须是有效的数字");
                return;
            }

            if (proxyAuthCheckbox.isSelected()) {
                proxySettings.setUsername(proxyUsernameField.getText());
                proxySettings.setPassword(proxyPasswordField.getText());
            }

            profile.setProxySettings(proxySettings);
        } else {
            ProxySettings proxySettings = new ProxySettings();
            proxySettings.setEnabled(false);
            profile.setProxySettings(proxySettings);
        }

        // 更新备注
        profile.setNotes(notesArea.getText());

        // 保存配置
        if (profileManager.getProfile(profile.getId()) == null) {
            profileManager.addProfile(profile);
        } else {
            profileManager.updateProfile(profile);
        }

        // 关闭窗口
        closeWindow();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void setupTheme() {
        // 注册当前窗口的场景以支持主题切换
        Platform.runLater(() -> {
            Scene scene = nameField.getScene();
            if (scene != null) {
                ThemeService themeService = ThemeService.getInstance();
                themeService.registerScene(scene);
            }
        });
    }
}
