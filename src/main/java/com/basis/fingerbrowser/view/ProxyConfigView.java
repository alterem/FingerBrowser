package com.basis.fingerbrowser.view;

import com.basis.fingerbrowser.model.ProxySettings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * 代理配置视图组件
 */
public class ProxyConfigView extends VBox {

    private ComboBox<String> proxyTypeComboBox;
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private CheckBox authCheckBox;
    private Button testButton;
    private Label statusLabel;

    private ProxySettings currentProxy;
    private Consumer<ProxySettings> onProxyTestRequest;

    /**
     * 构造函数
     */
    public ProxyConfigView() {
        setSpacing(10.0);
        setPadding(new Insets(10.0));
        initializeComponents();
    }

    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 标题标签
        Label titleLabel = new Label("代理设置");
        titleLabel.getStyleClass().add("section-header");

        // 代理类型选择
        Label typeLabel = new Label("代理类型:");
        proxyTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(
                "直接连接 (无代理)", "HTTP", "SOCKS4", "SOCKS5"));
        proxyTypeComboBox.getSelectionModel().selectFirst();
        proxyTypeComboBox.setMaxWidth(Double.MAX_VALUE);

        // 代理服务器设置
        GridPane serverGrid = new GridPane();
        serverGrid.setHgap(10.0);
        serverGrid.setVgap(5.0);
        serverGrid.setPadding(new Insets(5.0));

        Label hostLabel = new Label("服务器:");
        hostField = new TextField();
        hostField.setPromptText("例如: proxy.example.com");

        Label portLabel = new Label("端口:");
        portField = new TextField();
        portField.setPromptText("例如: 8080");

        serverGrid.add(hostLabel, 0, 0);
        serverGrid.add(hostField, 1, 0);
        serverGrid.add(portLabel, 0, 1);
        serverGrid.add(portField, 1, 1);

        // 认证设置
        authCheckBox = new CheckBox("需要认证");

        GridPane authGrid = new GridPane();
        authGrid.setHgap(10.0);
        authGrid.setVgap(5.0);
        authGrid.setPadding(new Insets(5.0));

        Label usernameLabel = new Label("用户名:");
        usernameField = new TextField();
        usernameField.setPromptText("用户名");
        usernameField.setDisable(true);

        Label passwordLabel = new Label("密码:");
        passwordField = new PasswordField();
        passwordField.setPromptText("密码");
        passwordField.setDisable(true);

        authGrid.add(usernameLabel, 0, 0);
        authGrid.add(usernameField, 1, 0);
        authGrid.add(passwordLabel, 0, 1);
        authGrid.add(passwordField, 1, 1);

        // 测试按钮和状态标签
        HBox actionBar = new HBox(10.0);
        testButton = new Button("测试连接");
        statusLabel = new Label("未测试");

        actionBar.getChildren().addAll(testButton, statusLabel);

        // 组装视图
        getChildren().addAll(titleLabel, typeLabel, proxyTypeComboBox, serverGrid,
                authCheckBox, authGrid, actionBar);

        // 设置监听器
        setupListeners();
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 代理类型改变时启用/禁用相关字段
        proxyTypeComboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDirectConnection = newVal.intValue() == 0;
            hostField.setDisable(isDirectConnection);
            portField.setDisable(isDirectConnection);
            authCheckBox.setDisable(isDirectConnection);

            if (isDirectConnection) {
                authCheckBox.setSelected(false);
            }

            // 更新认证字段状态
            boolean needAuth = authCheckBox.isSelected() && !isDirectConnection;
            usernameField.setDisable(!needAuth);
            passwordField.setDisable(!needAuth);
        });

        // 认证复选框改变时启用/禁用认证字段
        authCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDirectConnection = proxyTypeComboBox.getSelectionModel().getSelectedIndex() == 0;
            boolean needAuth = newVal && !isDirectConnection;
            usernameField.setDisable(!needAuth);
            passwordField.setDisable(!needAuth);
        });

        // 测试按钮点击事件
        testButton.setOnAction(e -> {
            try {
                ProxySettings proxySettings = getProxySettings();
                if (onProxyTestRequest != null) {
                    statusLabel.setText("正在测试...");
                    onProxyTestRequest.accept(proxySettings);
                }
            } catch (IllegalArgumentException ex) {
                statusLabel.setText("错误: " + ex.getMessage());
            }
        });

        // 端口输入验证 - 只允许数字输入
        portField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                portField.setText(oldVal);
            }
        });
    }

    /**
     * 从UI获取代理设置
     *
     * @return 代理设置对象
     * @throws IllegalArgumentException 如果输入数据无效
     */
    public ProxySettings getProxySettings() throws IllegalArgumentException {
        int selectedType = proxyTypeComboBox.getSelectionModel().getSelectedIndex();

        // 直接连接
        if (selectedType == 0) {
            return null; // 无代理
        }

        String host = hostField.getText();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("代理服务器地址不能为空");
        }

        String portText = portField.getText();
        if (portText == null || portText.trim().isEmpty()) {
            throw new IllegalArgumentException("代理端口不能为空");
        }

        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("端口必须在1-65535范围内");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("端口必须是有效的数字");
        }

        // 构建代理设置对象 (这里根据您的ProxySettings类做了适配)
        ProxySettings proxySettings = new ProxySettings();
        proxySettings.setType(getProxyTypeFromIndex(selectedType));
        proxySettings.setHost(host);
        proxySettings.setPort(port);

        // 设置认证信息
        if (authCheckBox.isSelected()) {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("认证用户名不能为空");
            }

            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("认证密码不能为空");
            }

            proxySettings.setUsername(username);
            proxySettings.setPassword(password);
            proxySettings.setRequiresAuthentication(true);
        } else {
            proxySettings.setRequiresAuthentication(false);
        }

        return proxySettings;
    }

    /**
     * 根据下拉索引获取代理类型字符串
     *
     * @param index 下拉索引
     * @return 代理类型字符串
     */
    private String getProxyTypeFromIndex(int index) {
        switch (index) {
            case 1:
                return "HTTP";
            case 2:
                return "SOCKS4";
            case 3:
                return "SOCKS5";
            default:
                return "DIRECT";
        }
    }

    /**
     * 根据代理类型字符串获取下拉索引
     *
     * @param type 代理类型字符串
     * @return 下拉索引
     */
    private int getIndexFromProxyType(String type) {
        if (type == null) return 0;

        switch (type) {
            case "HTTP":
                return 1;
            case "SOCKS4":
                return 2;
            case "SOCKS5":
                return 3;
            default:
                return 0;
        }
    }

    /**
     * 设置代理测试结果
     *
     * @param success 测试是否成功
     * @param message 测试消息
     */
    public void setTestResult(boolean success, String message) {
        if (success) {
            statusLabel.setText("测试成功");
            statusLabel.getStyleClass().removeAll("error-text");
            statusLabel.getStyleClass().add("success-text");
        } else {
            statusLabel.setText("测试失败: " + message);
            statusLabel.getStyleClass().removeAll("success-text");
            statusLabel.getStyleClass().add("error-text");
        }
    }

    /**
     * 设置当前代理配置
     *
     * @param proxySettings 代理设置对象
     */
    public void setProxySettings(ProxySettings proxySettings) {
        this.currentProxy = proxySettings;

        if (proxySettings == null) {
            // 无代理设置，选择"直接连接"
            proxyTypeComboBox.getSelectionModel().select(0);
            hostField.clear();
            portField.clear();
            authCheckBox.setSelected(false);
            usernameField.clear();
            passwordField.clear();

            hostField.setDisable(true);
            portField.setDisable(true);
            authCheckBox.setDisable(true);
            usernameField.setDisable(true);
            passwordField.setDisable(true);

            return;
        }

        // 设置代理类型
        int typeIndex = getIndexFromProxyType(proxySettings.getType());
        proxyTypeComboBox.getSelectionModel().select(typeIndex);

        // 设置服务器和端口
        hostField.setText(proxySettings.getHost());
        portField.setText(String.valueOf(proxySettings.getPort()));

        // 设置认证信息
        boolean requiresAuth = proxySettings.isRequiresAuthentication();
        authCheckBox.setSelected(requiresAuth);

        if (requiresAuth) {
            usernameField.setText(proxySettings.getUsername());
            passwordField.setText(proxySettings.getPassword());
        } else {
            usernameField.clear();
            passwordField.clear();
        }

        // 更新UI状态
        boolean isDirectConnection = typeIndex == 0;
        hostField.setDisable(isDirectConnection);
        portField.setDisable(isDirectConnection);
        authCheckBox.setDisable(isDirectConnection);
        usernameField.setDisable(!requiresAuth || isDirectConnection);
        passwordField.setDisable(!requiresAuth || isDirectConnection);
    }

    /**
     * 清除代理设置
     */
    public void clearProxySettings() {
        setProxySettings(null);
    }

    /**
     * 设置测试请求回调
     *
     * @param callback 回调函数
     */
    public void setOnProxyTestRequest(Consumer<ProxySettings> callback) {
        this.onProxyTestRequest = callback;
    }

    /**
     * 启用或禁用控件
     *
     * @param enabled 是否启用
     */
    public void setControlsEnabled(boolean enabled) {
        proxyTypeComboBox.setDisable(!enabled);

        boolean isDirectConnection = proxyTypeComboBox.getSelectionModel().getSelectedIndex() == 0;
        hostField.setDisable(isDirectConnection || !enabled);
        portField.setDisable(isDirectConnection || !enabled);
        authCheckBox.setDisable(isDirectConnection || !enabled);

        boolean requiresAuth = authCheckBox.isSelected();
        usernameField.setDisable(!requiresAuth || isDirectConnection || !enabled);
        passwordField.setDisable(!requiresAuth || isDirectConnection || !enabled);

        testButton.setDisable(!enabled);
    }

    /**
     * 验证代理配置是否有效
     *
     * @return 如果配置有效则返回true
     */
    public boolean validateProxyConfig() {
        try {
            getProxySettings();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取当前的代理配置
     *
     * @return 代理配置
     */
    public ProxySettings getCurrentProxy() {
        return currentProxy;
    }

    /**
     * 创建一个简单的代理配置视图对话框
     *
     * @param initialSettings 初始代理设置
     * @return 对话框实例
     */
    public static Dialog<ProxySettings> createProxyConfigDialog(ProxySettings initialSettings) {
        Dialog<ProxySettings> dialog = new Dialog<>();
        dialog.setTitle("代理设置");
        dialog.setHeaderText("配置浏览器代理设置");

        // 设置按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 创建代理配置视图
        ProxyConfigView proxyConfigView = new ProxyConfigView();
        if (initialSettings != null) {
            proxyConfigView.setProxySettings(initialSettings);
        }

        // 绑定对话框结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    return proxyConfigView.getProxySettings();
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            return null;
        });

        // 设置对话框内容
        dialog.getDialogPane().setContent(proxyConfigView);

        // 验证输入
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!proxyConfigView.validateProxyConfig()) {
                event.consume(); // 阻止对话框关闭
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("验证错误");
                alert.setHeaderText(null);
                alert.setContentText("请检查代理设置是否有效");
                alert.showAndWait();
            }
        });

        return dialog;
    }

    /**
     * 创建代理测试对话框
     *
     * @param testCallback 测试回调函数
     * @return 对话框实例
     */
    public static Dialog<Void> createProxyTestDialog(Consumer<ProxySettings> testCallback) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("测试代理连接");
        dialog.setHeaderText("配置并测试代理连接");

        // 设置按钮
        ButtonType testButtonType = new ButtonType("测试", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(testButtonType, closeButtonType);

        // 创建代理配置视图
        ProxyConfigView proxyConfigView = new ProxyConfigView();
        proxyConfigView.setOnProxyTestRequest(proxy -> {
            if (testCallback != null) {
                testCallback.accept(proxy);
            }
        });

        // 设置对话框内容
        dialog.getDialogPane().setContent(proxyConfigView);

        // 处理测试按钮点击
        Button testButton = (Button) dialog.getDialogPane().lookupButton(testButtonType);
        testButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume(); // 阻止对话框关闭
            if (proxyConfigView.validateProxyConfig()) {
                ProxySettings proxy = proxyConfigView.getProxySettings();
                if (testCallback != null) {
                    proxyConfigView.setTestResult(false, "测试中...");
                    testCallback.accept(proxy);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("验证错误");
                alert.setHeaderText(null);
                alert.setContentText("请检查代理设置是否有效");
                alert.showAndWait();
            }
        });

        return dialog;
    }
}
