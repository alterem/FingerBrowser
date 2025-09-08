package com.basis.fingerbrowser;

import com.basis.fingerbrowser.service.ThemeService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class FingerprintBrowserApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // macOS 特定设置
        setupMacOSMenu();
        
        // 加载主界面
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        // 设置标题和图标
        primaryStage.setTitle("FingerprintBrowser");
        try {
            var iconStream = getClass().getResourceAsStream("/images/app_icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            // 如果加载图标失败，继续运行
            System.out.println("Warning: Could not load application icon");
        }

        // 设置场景
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/application.css")).toExternalForm());
        primaryStage.setScene(scene);

        // 注册主题服务
        ThemeService themeService = ThemeService.getInstance();
        themeService.registerScene(scene);

        // 设置窗口属性
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // 显示主窗口
        primaryStage.show();

        // 设置关闭处理
        primaryStage.setOnCloseRequest(e -> {
            // 注销场景
            themeService.unregisterScene(scene);
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * 设置 macOS 菜单栏
     */
    private void setupMacOSMenu() {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                // 使用 Desktop API 设置关于菜单
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    
                    // 设置关于对话框处理器
                    if (desktop.isSupported(java.awt.Desktop.Action.APP_ABOUT)) {
                        desktop.setAboutHandler(e -> Platform.runLater(() -> {
                            // 在 JavaFX 线程中打开关于对话框
                            // 这里可以调用我们的自定义关于对话框
                            System.out.println("About menu clicked");
                        }));
                    }
                    
                    // 设置偏好设置处理器
                    if (desktop.isSupported(java.awt.Desktop.Action.APP_PREFERENCES)) {
                        desktop.setPreferencesHandler(e -> Platform.runLater(() -> {
                            // 在 JavaFX 线程中打开设置对话框
                            // 这里可以调用我们的设置页面
                            System.out.println("Preferences menu clicked");
                        }));
                    }
                    
                    // 设置退出处理器
                    if (desktop.isSupported(java.awt.Desktop.Action.APP_QUIT_HANDLER)) {
                        desktop.setQuitHandler((e, response) -> {
                            Platform.exit();
                            response.performQuit();
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("Warning: Could not setup macOS menu integration: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
