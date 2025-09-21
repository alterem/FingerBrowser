package com.basis.fingerbrowser.controller;

import com.basis.fingerbrowser.model.BrowserProfile;
import com.basis.fingerbrowser.model.ProfileViewModel;
import com.basis.fingerbrowser.service.BrowserService;
import com.basis.fingerbrowser.service.ProfileManagerService;
import com.basis.fingerbrowser.service.ThemeService;
import com.basis.fingerbrowser.util.FingerprintGenerator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.prefs.Preferences;

public class MainController {
    // Logger
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // Constants
    private static final String APP_DATA_DIR_NAME = ".fingerbrowser";
    private static final String PROFILES_DIR_NAME = "profiles";
    private static final String BROWSER_DATA_DIR_NAME = "browser_data";
    private static final String BROWSER_PATH_KEY = "browser_path";

    // 移除 browserPathField，现在在设置页面中管理
    // @FXML
    // private TextField browserPathField;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<BrowserProfile> profileList;
    @FXML
    private Label profileCountLabel;
    @FXML
    private Label runningCountLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button topLaunchButton;
    @FXML
    private Button topStopButton;
    @FXML
    private Button leftLaunchButton;

    // --- Profile Details Fields ---
    @FXML
    private TextField nameField;
    @FXML
    private TextField createdAtField;
    @FXML
    private TextField userAgentField;
    @FXML
    private TextField platformField;
    @FXML
    private TextField languageField;
    @FXML
    private TextField timezoneField;
    @FXML
    private TextField resolutionField;
    @FXML
    private TextField webrtcField;
    @FXML
    private TextField canvasField;
    @FXML
    private TextField fontField;
    @FXML
    private TextField proxyTypeField;
    @FXML
    private TextField proxyHostField;
    @FXML
    private TextField proxyPortField;
    @FXML
    private TextField proxyAuthField;
    @FXML
    private TextArea notesArea;

    private ProfileManagerService profileManager;
    private BrowserService browserService;
    private ThemeService themeService;
    private FilteredList<BrowserProfile> filteredProfiles;
    private final ProfileViewModel profileViewModel = new ProfileViewModel();
    private Preferences preferences;
    private final javafx.beans.property.BooleanProperty browserPathValid = new javafx.beans.property.SimpleBooleanProperty(false);
    private java.util.concurrent.ExecutorService executor;

    private final StringProperty status = new SimpleStringProperty("就绪");

    @FXML
    public void initialize() {
        log.info("Initializing MainController...");
        setupServices();
        setupBindings();
        setupEventListeners();
        setupTheme();
        setupKeyboardShortcuts();
        // 是否在启动时检查更新
        boolean shouldCheckUpdates = java.util.prefs.Preferences.userRoot()
                .node("/com/basis/fingerbrowser")
                .getBoolean(com.basis.fingerbrowser.util.AppPreferences.CHECK_UPDATES_KEY, false);
        if (shouldCheckUpdates) {
            checkForUpdatesSilently();
        }
        log.info("MainController initialization complete.");
    }

    private void setupServices() {
        String userHome = System.getProperty("user.home");
        String appDataDir = userHome + File.separator + APP_DATA_DIR_NAME;

        String profilesDir = appDataDir + File.separator + PROFILES_DIR_NAME;
        profileManager = new ProfileManagerService(profilesDir);
        log.info("Profile manager initialized. Loading profiles from: {}", profilesDir);

        // 初始化偏好设置
        preferences = Preferences.userNodeForPackage(MainController.class);

        // 从设置中加载浏览器路径，如果没有则使用默认路径
        String browserPath = preferences.get(BROWSER_PATH_KEY, "");
        if (browserPath.isEmpty() || !new File(browserPath).exists()) {
            browserPath = findDefaultBrowserPath();
            if (!browserPath.isEmpty()) {
                preferences.put(BROWSER_PATH_KEY, browserPath);
                try {
                    preferences.flush();
                } catch (Exception e) {
                    log.warn("Failed to save default browser path to preferences", e);
                }
            }
        }

        try {
            browserService = new BrowserService(browserPath, appDataDir + File.separator + BROWSER_DATA_DIR_NAME);
            log.info("Browser service initialized. Browser path: {}", browserPath);
            boolean valid = browserPath != null && !browserPath.isBlank() && new File(browserPath).exists();
            browserPathValid.set(valid);
            if (!valid) {
                Platform.runLater(() -> {
                    setStatus("未检测到浏览器路径，请在设置中配置");
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "未检测到有效的浏览器路径，请前往设置进行配置。", ButtonType.OK, new ButtonType("打开设置", ButtonBar.ButtonData.YES));
                    alert.setTitle("需要配置浏览器路径");
                    alert.setHeaderText(null);
                    alert.showAndWait().ifPresent(btn -> {
                        if (btn.getButtonData() == ButtonBar.ButtonData.YES) {
                            handleOpenSettings();
                        }
                    });
                });
            }
        } catch (RuntimeException e) {
            log.error("Failed to initialize browser service", e);
            showAlert("初始化错误", "无法初始化浏览器服务: " + e.getMessage());
        }

        // 初始化后台执行器
        executor = java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ui-tasks");
            t.setDaemon(true);
            return t;
        });
    }

    private void setupBindings() {
        // Bind ViewModel properties to UI fields
        nameField.textProperty().bind(profileViewModel.nameProperty());
        createdAtField.textProperty().bind(profileViewModel.createdAtProperty());
        userAgentField.textProperty().bind(profileViewModel.userAgentProperty());
        platformField.textProperty().bind(profileViewModel.platformProperty());
        languageField.textProperty().bind(profileViewModel.languageProperty());
        timezoneField.textProperty().bind(profileViewModel.timezoneProperty());
        resolutionField.textProperty().bind(profileViewModel.resolutionProperty());
        webrtcField.textProperty().bind(profileViewModel.webrtcStatusProperty());
        canvasField.textProperty().bind(profileViewModel.canvasStatusProperty());
        fontField.textProperty().bind(profileViewModel.fontStatusProperty());
        proxyTypeField.textProperty().bind(profileViewModel.proxyTypeProperty());
        proxyHostField.textProperty().bind(profileViewModel.proxyHostProperty());
        proxyPortField.textProperty().bind(profileViewModel.proxyPortProperty());
        proxyAuthField.textProperty().bind(profileViewModel.proxyAuthProperty());
        notesArea.textProperty().bind(profileViewModel.notesProperty());

        // Bind status label
        statusLabel.textProperty().bind(status);

        // 根据浏览器路径有效性禁用启动按钮
        if (topLaunchButton != null) topLaunchButton.disableProperty().bind(browserPathValid.not());
        if (leftLaunchButton != null) leftLaunchButton.disableProperty().bind(browserPathValid.not());
    }

    private void setupEventListeners() {
        // Setup profile list
        filteredProfiles = new FilteredList<>(profileManager.getProfiles(), p -> true);
        profileList.setItems(filteredProfiles);
        updateProfileCount();

        // Listener for list selection changes
        profileList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> profileViewModel.setProfile(newSelection));

        // Listener for search field
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            filteredProfiles.setPredicate(profile -> {
                if (newText == null || newText.isEmpty()) {
                    return true;
                }
                String searchText = newText.toLowerCase();
                return profile.getName().toLowerCase().contains(searchText) ||
                        (profile.getNotes() != null && profile.getNotes().toLowerCase().contains(searchText));
            });
            updateProfileCount();
        });

        // Cell factory for list view
        profileList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(BrowserProfile profile, boolean empty) {
                super.updateItem(profile, empty);
                if (empty || profile == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(profile.getName());
                    setStyle(profile.isActive() ? "-fx-font-weight: bold; -fx-text-fill: green;" : "");
                }
            }
        });
    }

    private void setupTheme() {
        // 初始化主题服务
        themeService = ThemeService.getInstance();

        // 注册当前场景以支持主题切换
        Platform.runLater(() -> {
            Scene scene = profileList.getScene();
            if (scene != null) {
                themeService.registerScene(scene);
                log.info("Scene registered for theme management");
            }
        });

        // 添加主题变更监听器
        themeService.addThemeChangeListener(newTheme -> {
            Platform.runLater(() -> {
                String message = "已切换到" + (themeService.isDarkTheme() ? "深色" : "浅色") + "主题";
                setStatus(message);
                log.info("Theme changed to: {}", newTheme);
            });
        });
    }

    /**
     * 设置键盘快捷键
     */
    private void setupKeyboardShortcuts() {
        Platform.runLater(() -> {
            Scene scene = profileList.getScene();
            if (scene != null) {
                // macOS: Command + ,
                // Windows/Linux: Ctrl + ,
                KeyCombination settingsShortcut = System.getProperty("os.name").toLowerCase().contains("mac")
                        ? new KeyCodeCombination(KeyCode.COMMA, KeyCombination.META_DOWN)
                        : new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN);

                scene.getAccelerators().put(settingsShortcut, this::handleOpenSettings);
                log.info("Keyboard shortcut registered: {} for opening settings", settingsShortcut);
            }
        });
    }

    /**
     * 从设置中加载浏览器路径 (已废弃 - 集成到setupServices中)
     */
    // 移除此方法，功能已集成到setupServices中

    /**
     * 更新浏览器路径（从设置页面调用）
     */
    public void updateBrowserPath(String browserPath) {
        if (browserService == null) return;
        boolean valid = browserPath != null && !browserPath.isBlank() && new File(browserPath).exists();
        try {
            if (valid) {
                browserService.setBrowserExecutablePath(browserPath);
                // 保存到偏好设置
                preferences.put(BROWSER_PATH_KEY, browserPath);
                try {
                    preferences.flush();
                } catch (Exception e) {
                    log.warn("Failed to save browser path to preferences", e);
                }
                browserPathValid.set(true);
                log.info("Browser path updated: {}", browserPath);
            } else {
                browserPathValid.set(false);
                showAlert("无效路径", "请选择有效的浏览器可执行文件路径。");
            }
        } catch (IllegalArgumentException ex) {
            browserPathValid.set(false);
            showAlert("无效路径", ex.getMessage());
        }
    }


    private String findDefaultBrowserPath() {
        log.debug("Attempting to find default browser path...");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String[] paths = {
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe"
            };
            for (String path : paths) {
                if (new File(path).exists()) {
                    log.info("Found browser at: {}", path);
                    return path;
                }
            }
        } else if (os.contains("mac")) {
            String[] paths = {
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
            };
            for (String path : paths) {
                if (new File(path).exists()) {
                    log.info("Found browser at: {}", path);
                    return path;
                }
            }
        } else {
            String[] commands = {"google-chrome", "chromium-browser", "chromium", "microsoft-edge"};
            for (String cmd : commands) {
                try {
                    ProcessBuilder builder = new ProcessBuilder("which", cmd);
                    Process process = builder.start();
                    if (process.waitFor() == 0) {
                        try (var scanner = new java.util.Scanner(process.getInputStream())) {
                            if (scanner.hasNextLine()) {
                                String path = scanner.nextLine();
                                log.info("Found browser at: {}", path);
                                return path;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error while searching for command '{}': {}", cmd, e.getMessage());
                }
            }
        }

        log.warn("Could not find a default browser path.");
        return "";
    }

    private void updateProfileCount() {
        profileCountLabel.setText(String.valueOf(filteredProfiles.size()));
        long runningCount = profileManager.getProfiles().stream().filter(BrowserProfile::isActive).count();
        runningCountLabel.setText(String.valueOf(runningCount));
    }

    @FXML
    private void handleNewProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile_editor.fxml"));
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.setProfileManager(profileManager);

            BrowserProfile profile = FingerprintGenerator.generateRandomProfile("新配置 " + (profileManager.getProfiles().size() + 1));
            controller.setProfile(profile);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("新建浏览器配置");
            Scene scene = new Scene(root);
            stage.setScene(scene);

            // 注册主题服务并确保样式表加载
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            themeService.registerScene(scene);

            stage.showAndWait();

            // 注销场景
            themeService.unregisterScene(scene);

            // After closing the editor, refresh the list and select the new profile
            if (profileManager.getProfiles().contains(profile)) {
                profileList.getSelectionModel().select(profile);
            }
            updateProfileCount();

        } catch (IOException e) {
            log.error("Failed to open profile editor.", e);
            showAlert("错误", "无法打开配置编辑器: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditProfile() {
        BrowserProfile selectedProfile = profileList.getSelectionModel().getSelectedItem();
        if (selectedProfile == null) {
            showAlert("提示", "请先选择一个配置");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile_editor.fxml"));
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.setProfileManager(profileManager);
            controller.setProfile(selectedProfile);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("编辑浏览器配置");
            Scene scene = new Scene(root);
            stage.setScene(scene);

            // 注册主题服务并确保样式表加载
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            themeService.registerScene(scene);

            stage.showAndWait();

            // 注销场景
            themeService.unregisterScene(scene);

            // Refresh the view model to reflect changes
            profileViewModel.setProfile(selectedProfile);
            profileList.refresh();

        } catch (IOException e) {
            log.error("Failed to open profile editor for profile '{}'", selectedProfile.getName(), e);
            showAlert("错误", "无法打开配置编辑器: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteProfile() {
        BrowserProfile selectedProfile = profileList.getSelectionModel().getSelectedItem();
        if (selectedProfile == null) {
            showAlert("提示", "请先选择一个配置");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除浏览器配置");
        alert.setContentText("确定要删除 " + selectedProfile.getName() + " 配置吗？此操作不可恢复。");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            log.info("User confirmed deletion of profile '{}'", selectedProfile.getName());
            if (selectedProfile.isActive()) {
                handleStopSelected();
            }
            profileManager.deleteProfile(selectedProfile.getId());
            updateProfileCount();
        }
    }

    @FXML
    private void handleImportProfile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导入配置文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON配置文件", "*.json"));

        File file = fileChooser.showOpenDialog(profileList.getScene().getWindow());
        if (file != null) {
            log.info("Importing profiles from file: {}", file.getAbsolutePath());
            profileManager.importProfiles(file);
            updateProfileCount();
            setStatus("已导入配置");
        }
    }

    @FXML
    private void handleExportProfile() {
        BrowserProfile selectedProfile = profileList.getSelectionModel().getSelectedItem();
        if (selectedProfile == null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "您要导出所有配置还是选择特定配置？",
                    new ButtonType("导出所有", ButtonBar.ButtonData.YES),
                    new ButtonType("选择配置", ButtonBar.ButtonData.NO),
                    new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE));
            alert.setTitle("导出配置");
            alert.setHeaderText("导出配置选项");

            alert.showAndWait().ifPresent(response -> {
                if (response.getButtonData() == ButtonBar.ButtonData.YES) {
                    exportAllProfiles();
                } else if (response.getButtonData() == ButtonBar.ButtonData.NO) {
                    showProfileSelectionDialog();
                }
            });
        } else {
            exportSingleProfile(selectedProfile);
        }
    }

    private void exportSingleProfile(BrowserProfile profile) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出配置");
        fileChooser.setInitialFileName(profile.getName() + ".json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON配置文件", "*.json"));

        File file = fileChooser.showSaveDialog(profileList.getScene().getWindow());
        if (file != null) {
            log.info("Exporting profile '{}' to file: {}", profile.getName(), file.getAbsolutePath());
            if (profileManager.exportProfile(profile, file)) {
                setStatus("已导出配置到 " + file.getName());
            } else {
                showAlert("错误", "导出配置失败");
            }
        }
    }

    private void exportAllProfiles() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("选择导出目录");
        File dir = dirChooser.showDialog(profileList.getScene().getWindow());
        if (dir != null) {
            log.info("Exporting all profiles to directory: {}", dir.getAbsolutePath());
            if (profileManager.exportAllProfiles(dir)) {
                setStatus("已导出所有配置到 " + dir.getName() + " 目录");
            } else {
                showAlert("错误", "导出配置失败");
            }
        }
    }

    private void showProfileSelectionDialog() {
        Dialog<BrowserProfile> dialog = new Dialog<>();
        dialog.setTitle("选择要导出的配置");
        dialog.setHeaderText("请选择要导出的浏览器配置");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ListView<BrowserProfile> listView = new ListView<>(profileManager.getProfiles());
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BrowserProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getName());
            }
        });
        dialog.getDialogPane().setContent(listView);

        dialog.setResultConverter(dialogButton -> dialogButton == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);
        dialog.showAndWait().ifPresent(this::exportSingleProfile);
    }

    @FXML
    private void handleLaunchSelected() {
        BrowserProfile selectedProfile = profileList.getSelectionModel().getSelectedItem();
        if (selectedProfile == null) {
            showAlert("提示", "请先选择一个配置");
            return;
        }
        if (selectedProfile.isActive()) {
            showAlert("提示", "此配置已在运行中。");
            return;
        }

        setStatus("正在启动浏览器: " + selectedProfile.getName() + "...");
        if (!browserPathValid.get()) {
            showAlert("提示", "浏览器路径未配置或无效，请先前往设置配置。");
            return;
        }
        runTask(new Task<>() {
            @Override
            protected Boolean call() {
                // 浏览器路径现在由BrowserService管理，不需要从UI获取
                return browserService.launchBrowser(selectedProfile);
            }
        }, "启动", selectedProfile.getName());
    }

    @FXML
    private void handleStopSelected() {
        BrowserProfile selectedProfile = profileList.getSelectionModel().getSelectedItem();
        if (selectedProfile == null) {
            showAlert("提示", "请先选择一个配置");
            return;
        }
        if (!selectedProfile.isActive()) {
            showAlert("提示", "此浏览器实例未在运行");
            return;
        }

        setStatus("正在关闭浏览器: " + selectedProfile.getName() + "...");
        runTask(new Task<>() {
            @Override
            protected Boolean call() {
                return browserService.closeBrowser(selectedProfile);
            }
        }, "关闭", selectedProfile.getName());
    }

    private void runTask(Task<Boolean> task, String action, String profileName) {
        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                setStatus("已" + action + "浏览器: " + profileName);
            } else {
                showAlert("错误", action + "浏览器失败");
                setStatus(action + "浏览器失败");
            }
            profileList.refresh();
            updateProfileCount();
        });
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            log.error("Browser {} task failed for profile '{}'", action, profileName, ex);
            showAlert("错误", action + "任务失败: " + ex.getMessage());
            setStatus(action + "浏览器失败");
        });
        if (executor != null) {
            executor.submit(task);
        } else {
            new Thread(task).start();
        }
    }

    @FXML
    private void handleRefreshList() {
        profileList.refresh();
        updateProfileCount();
        setStatus("已刷新列表");
        log.info("Profile list refreshed.");
    }

    @FXML
    private void handleCheckUpdates() {
        setStatus("正在检查更新...");
        if (executor == null) {
            executor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "update-check"); t.setDaemon(true); return t; });
        }
        executor.submit(() -> {
            String current = com.basis.fingerbrowser.util.AppInfo.getVersion();
            // 重新请求 latest.json 原文以便解析 notes
            java.util.Optional<String> latestOpt = com.basis.fingerbrowser.service.UpdateService.fetchLatestVersion();
            Platform.runLater(() -> {
                if (latestOpt.isEmpty()) {
                    setStatus("检查更新失败或无网络");
                    com.basis.fingerbrowser.util.ToastUtil.showToast(getScene(), "无法获取更新信息");
                } else {
                    String latest = latestOpt.get();
                    if (com.basis.fingerbrowser.service.UpdateService.isNewer(latest, current)) {
                        setStatus("有可用更新: " + latest);
                        String notes = null;
                        // 尝试再次读取 notes（可扩展：缓存 latest.json 原文）
                        try {
                            var client = java.net.http.HttpClient.newHttpClient();
                            var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://raw.githubusercontent.com/alterem/fingerbrowser/main/latest.json")).GET().build();
                            var resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                            if (resp.statusCode() == 200) {
                                notes = com.basis.fingerbrowser.service.UpdateService.parseNotesFromJson(resp.body()).orElse(null);
                            }
                        } catch (Exception ignored) {}

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("更新提示");
                        alert.setHeaderText("发现新版本 " + latest + "（当前 " + current + ")");
                        String content = (notes != null && !notes.isBlank()) ? notes : "是否前往下载？";
                        alert.setContentText(content);
                        alert.getButtonTypes().setAll(
                                new ButtonType("前往下载", ButtonBar.ButtonData.YES),
                                new ButtonType("稍后", ButtonBar.ButtonData.CANCEL_CLOSE)
                        );
                        alert.showAndWait().ifPresent(btn -> {
                            if (btn.getButtonData() == ButtonBar.ButtonData.YES) {
                                try {
                                    if (java.awt.Desktop.isDesktopSupported()) {
                                        java.awt.Desktop.getDesktop().browse(new java.net.URI(com.basis.fingerbrowser.service.UpdateService.getReleasesUrl()));
                                    }
                                } catch (Exception ignored) { }
                            }
                        });
                    } else {
                        setStatus("已是最新版本");
                        com.basis.fingerbrowser.util.ToastUtil.showToast(getScene(), "已是最新版本");
                    }
                }
            });
        });
    }

    /**
     * 浏览器路径选择功能已移动到设置页面
     * 此方法已废弃
     */
    // @FXML
    // private void handleBrowseBrowserPath() {
    //     // 功能已移动到设置页面
    // }

    @FXML
    private void handleAbout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/about.fxml"));
            Parent root = loader.load();

            AboutController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("关于 FingerprintBrowser");
            stage.setResizable(false);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // 注册主题服务并确保样式表加载
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            themeService.registerScene(scene);
            controller.setStage(stage);
            
            stage.showAndWait();

            // 注销场景
            themeService.unregisterScene(scene);
            
            log.info("About window closed");

        } catch (IOException e) {
            log.error("Failed to open about window", e);
            // 如果无法打开自定义关于窗口，回退到简单的 Alert
            showAlert("关于", "指纹浏览器 v1.0\n\n一款用于防关联的多开浏览器，每个实例拥有独立的浏览器指纹。\n\n© 2025 FingerprintBrowser");
        }
    }

    @FXML
    private void handleToggleTheme() {
        if (themeService != null) {
            themeService.toggleTheme();
            log.info("User toggled theme to: {}", themeService.getCurrentTheme());
        }
    }

    /**
     * 打开设置页面
     */
    @FXML
    private void handleOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();

            SettingsController controller = loader.getController();
            controller.setMainController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("设置");
            Scene scene = new Scene(root);
            stage.setScene(scene);

            // 注册主题服务并确保样式表加载
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            themeService.registerScene(scene);
            controller.setStage(stage);

            stage.showAndWait();

            // 注销场景
            themeService.unregisterScene(scene);

            log.info("Settings window closed");

        } catch (IOException e) {
            log.error("Failed to open settings window", e);
            showAlert("错误", "无法打开设置页面: " + e.getMessage());
        }
    }

    @FXML
    private void handleSetLightTheme() {
        if (themeService != null) {
            themeService.setLightTheme();
            log.info("User switched to light theme");
        }
    }

    @FXML
    private void handleSetDarkTheme() {
        if (themeService != null) {
            themeService.setDarkTheme();
            log.info("User switched to dark theme");
        }
    }

    private void setStatus(String message) {
        Platform.runLater(() -> status.set(message));
    }

    /**
     * 提供主界面的 Scene 以供外部展示 Toast 等
     */
    public Scene getScene() {
        if (profileList != null) {
            return profileList.getScene();
        }
        return null;
    }

    private void checkForUpdatesSilently() {
        var current = com.basis.fingerbrowser.util.AppInfo.getVersion();
        if (executor == null) {
            executor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "update-check"); t.setDaemon(true); return t; });
        }
        executor.submit(() -> {
            var latestOpt = com.basis.fingerbrowser.service.UpdateService.fetchLatestVersion();
            latestOpt.ifPresent(latest -> {
                if (com.basis.fingerbrowser.service.UpdateService.isNewer(latest, current)) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                "发现新版本 " + latest + "，当前版本 " + current + "。是否前往下载？",
                                new ButtonType("前往下载", ButtonBar.ButtonData.YES),
                                new ButtonType("稍后", ButtonBar.ButtonData.CANCEL_CLOSE));
                        alert.setTitle("更新提示");
                        alert.setHeaderText("有可用更新");
                        alert.showAndWait().ifPresent(btn -> {
                            if (btn.getButtonData() == ButtonBar.ButtonData.YES) {
                                try {
                                    if (java.awt.Desktop.isDesktopSupported()) {
                                        java.awt.Desktop.getDesktop().browse(new java.net.URI(com.basis.fingerbrowser.service.UpdateService.getReleasesUrl()));
                                    }
                                } catch (Exception ignored) { }
                            }
                        });
                    });
                }
            });
        });
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 应用即将关闭时的清理逻辑
     */
    public void onAppClose() {
        try {
            if (browserService != null) {
                browserService.close();
            }
            if (executor != null) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            log.warn("Error while closing BrowserService", e);
        }
    }
}
