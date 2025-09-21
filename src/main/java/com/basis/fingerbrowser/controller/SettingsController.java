package com.basis.fingerbrowser.controller;

import com.basis.fingerbrowser.service.ThemeService;
import com.basis.fingerbrowser.util.DialogUtil;
import com.basis.fingerbrowser.util.AppPreferences;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static com.basis.fingerbrowser.util.ToastUtil.showToast;

/**
 * 设置页面控制器
 * 管理应用程序的各种设置，包括主题、浏览器路径等
 */
public class SettingsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    // 偏好设置键 - 与MainController保持一致
    private static final String BROWSER_PATH_KEY = AppPreferences.BROWSER_PATH_KEY;
    private static final String AUTO_SAVE_KEY = AppPreferences.AUTO_SAVE_KEY;
    private static final String CHECK_UPDATES_KEY = AppPreferences.CHECK_UPDATES_KEY;
    private static final String LANGUAGE_KEY = AppPreferences.LANGUAGE_KEY;

    // FXML 控件
    @FXML private ComboBox<String> themeComboBox;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private TextField browserPathField;
    @FXML private CheckBox autoSaveCheckBox;
    @FXML private CheckBox checkUpdatesCheckBox;
    // Launch flags
    @FXML private CheckBox disableExtensionsCheckBox;
    @FXML private CheckBox disableBackgroundNetworkingCheckBox;
    @FXML private CheckBox disableComponentUpdateCheckBox;
    @FXML private CheckBox v8MemoryTweakCheckBox;

    // 服务和工具
    private ThemeService themeService;
    private Preferences preferences;
    private Stage stage;
    private MainController mainController;

    // 用于追踪设置是否有变更
    private boolean hasChanges = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing SettingsController");

        // 初始化服务
        themeService = ThemeService.getInstance();
        // 使用与MainController相同的Preferences节点
        preferences = AppPreferences.getNode();

        // 设置控件监听器
        setupControlListeners();

        // 加载当前设置
        loadCurrentSettings();

        logger.info("SettingsController initialized successfully");
    }

    /**
     * 设置父控制器引用
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * 设置当前窗口引用
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 设置控件变更监听器
     */
    private void setupControlListeners() {
        // 主题选择监听器
        themeComboBox.setOnAction(e -> markAsChanged());

        // 浏览器路径监听器
        browserPathField.textProperty().addListener((observable, oldValue, newValue) -> markAsChanged());

        // 其他设置监听器
        autoSaveCheckBox.setOnAction(e -> markAsChanged());
        checkUpdatesCheckBox.setOnAction(e -> markAsChanged());
        languageComboBox.setOnAction(e -> markAsChanged());
        // Launch flags listeners
        disableExtensionsCheckBox.setOnAction(e -> markAsChanged());
        disableBackgroundNetworkingCheckBox.setOnAction(e -> markAsChanged());
        disableComponentUpdateCheckBox.setOnAction(e -> markAsChanged());
        v8MemoryTweakCheckBox.setOnAction(e -> markAsChanged());
    }

    /**
     * 标记设置已变更
     */
    private void markAsChanged() {
        hasChanges = true;
    }

    /**
     * 加载当前设置到界面
     */
    private void loadCurrentSettings() {
        try {
            // 加载主题设置
            String currentTheme = themeService.getCurrentTheme();
            if ("light".equals(currentTheme)) {
                themeComboBox.setValue("浅色主题");
            } else {
                themeComboBox.setValue("深色主题");
            }

            // 加载浏览器路径
            String browserPath = preferences.get(BROWSER_PATH_KEY, "");
            browserPathField.setText(browserPath);

            // 加载其他设置
            autoSaveCheckBox.setSelected(preferences.getBoolean(AUTO_SAVE_KEY, true));
            checkUpdatesCheckBox.setSelected(preferences.getBoolean(CHECK_UPDATES_KEY, false));
            // 加载启动参数设置
            disableExtensionsCheckBox.setSelected(preferences.getBoolean(AppPreferences.DISABLE_EXTENSIONS_KEY, false));
            disableBackgroundNetworkingCheckBox.setSelected(preferences.getBoolean(AppPreferences.DISABLE_BACKGROUND_NETWORKING_KEY, true));
            disableComponentUpdateCheckBox.setSelected(preferences.getBoolean(AppPreferences.DISABLE_COMPONENT_UPDATE_KEY, true));
            v8MemoryTweakCheckBox.setSelected(preferences.getBoolean(AppPreferences.V8_MEMORY_TWEAK_KEY, true));

            // 加载语言设置
            String language = preferences.get(LANGUAGE_KEY, "简体中文");
            languageComboBox.setValue(language);

            // 重置变更标记
            hasChanges = false;

            logger.debug("Current settings loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load current settings", e);
            showErrorAlert("加载设置失败", "无法加载当前设置，可能会显示默认值。");
        }
    }

    /**
     * 处理浏览器路径浏览按钮点击
     */
    @FXML
    private void handleBrowseBrowserPath() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择浏览器可执行文件");

            // 设置文件过滤器
            FileChooser.ExtensionFilter allFiles = new FileChooser.ExtensionFilter("所有文件", "*.*");
            FileChooser.ExtensionFilter appFiles = new FileChooser.ExtensionFilter("应用程序", "*.app", "*.exe");
            fileChooser.getExtensionFilters().addAll(appFiles, allFiles);

            // 设置初始目录
            String currentPath = browserPathField.getText();
            if (!currentPath.isEmpty()) {
                File currentFile = new File(currentPath);
                if (currentFile.exists() && currentFile.getParent() != null) {
                    fileChooser.setInitialDirectory(new File(currentFile.getParent()));
                }
            } else {
                // macOS 默认应用程序目录
                File applicationsDir = new File("/Applications");
                if (applicationsDir.exists()) {
                    fileChooser.setInitialDirectory(applicationsDir);
                }
            }

            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                browserPathField.setText(selectedFile.getAbsolutePath());
                logger.info("Browser path selected: {}", selectedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Failed to browse for browser path", e);
            showErrorAlert("文件选择失败", "无法打开文件选择器，请检查系统权限。");
        }
    }

    /**
     * 处理应用按钮点击
     */
    @FXML
    private void handleApply() {
        try {
            // 保存主题设置
            String selectedTheme = themeComboBox.getValue();
            if ("浅色主题".equals(selectedTheme)) {
                themeService.setLightTheme();
            } else {
                themeService.setDarkTheme();
            }

            // 保存浏览器路径
            String browserPath = browserPathField.getText().trim();
            preferences.put(BROWSER_PATH_KEY, browserPath);

            // 更新主控制器的浏览器路径
            if (mainController != null) {
                mainController.updateBrowserPath(browserPath);
            }

            // 保存其他设置
            preferences.putBoolean(AUTO_SAVE_KEY, autoSaveCheckBox.isSelected());
            preferences.putBoolean(CHECK_UPDATES_KEY, checkUpdatesCheckBox.isSelected());
            preferences.put(LANGUAGE_KEY, languageComboBox.getValue());
            // 保存启动参数设置
            preferences.putBoolean(AppPreferences.DISABLE_EXTENSIONS_KEY, disableExtensionsCheckBox.isSelected());
            preferences.putBoolean(AppPreferences.DISABLE_BACKGROUND_NETWORKING_KEY, disableBackgroundNetworkingCheckBox.isSelected());
            preferences.putBoolean(AppPreferences.DISABLE_COMPONENT_UPDATE_KEY, disableComponentUpdateCheckBox.isSelected());
            preferences.putBoolean(AppPreferences.V8_MEMORY_TWEAK_KEY, v8MemoryTweakCheckBox.isSelected());

            // 刷新偏好设置
            preferences.flush();

            // 重置变更标记
            hasChanges = false;

            // Toast 成功提示（优先主界面场景，确保根节点支持叠加）
            if (mainController != null && mainController.getScene() != null) {
                showToast(mainController.getScene(), "设置已保存");
            } else if (stage != null && stage.getScene() != null) {
                showToast(stage.getScene(), "设置已保存");
            } else {
                showInfoAlert("设置已保存", "您的设置已成功保存并应用。");
            }

            logger.info("Settings applied successfully");

            // 关闭设置窗口
            closeWindow();
        } catch (Exception e) {
            logger.error("Failed to apply settings", e);
            showErrorAlert("保存设置失败", "无法保存设置，请重试。错误信息：" + e.getMessage());
        }
    }

    /**
     * 处理取消按钮点击
     */
    @FXML
    private void handleCancel() {
        if (hasChanges) {
            Alert alert = DialogUtil.createConfirmationAlert("确认取消", "您有未保存的更改", "是否确定要取消并放弃所有更改？");
            ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
            if (result == ButtonType.OK) {
                closeWindow();
            }
        } else {
            closeWindow();
        }
    }

    /**
     * 处理重置为默认按钮点击
     */
    @FXML
    private void handleResetToDefaults() {
        Alert alert = DialogUtil.createConfirmationAlert("重置设置", "重置为默认设置", "这将重置所有设置为默认值。是否继续？");
        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (result == ButtonType.OK) {
            resetToDefaults();
        }
    }

    /**
     * 重置所有设置为默认值
     */
    private void resetToDefaults() {
        try {
            // 重置主题为浅色（新的默认值）
            themeComboBox.setValue("浅色主题");

            // 重置浏览器路径
            browserPathField.setText("");

            // 重置其他设置
            autoSaveCheckBox.setSelected(true);
            checkUpdatesCheckBox.setSelected(false);
            languageComboBox.setValue("简体中文");

            // 标记为已变更
            markAsChanged();

            logger.info("Settings reset to defaults");
        } catch (Exception e) {
            logger.error("Failed to reset settings to defaults", e);
            showErrorAlert("重置失败", "无法重置设置，请重试。");
        }
    }

    /**
     * 关闭设置窗口
     */
    private void closeWindow() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 显示信息提示框
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = DialogUtil.createInformationAlert(title, message);
        alert.showAndWait();
    }

    /**
     * 显示错误提示框
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = DialogUtil.createErrorAlert(title, message);
        alert.showAndWait();
    }

    /**
     * 获取当前浏览器路径设置
     */
    public String getBrowserPath() {
        return preferences.get(BROWSER_PATH_KEY, "");
    }

    /**
     * 检查是否有未保存的更改
     */
    public boolean hasUnsavedChanges() {
        return hasChanges;
    }
}
