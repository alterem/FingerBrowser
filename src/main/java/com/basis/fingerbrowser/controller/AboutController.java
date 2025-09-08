package com.basis.fingerbrowser.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 关于对话框控制器
 */
public class AboutController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AboutController.class);

    @FXML private Hyperlink licenseLink;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing AboutController");
        
        // 设置超链接点击事件
        if (licenseLink != null) {
            licenseLink.setOnAction(e -> handleLicenseClick());
        }
        
        logger.info("AboutController initialized successfully");
    }

    /**
     * 设置当前窗口引用
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 处理确定按钮点击
     */
    @FXML
    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 处理许可链接点击
     */
    private void handleLicenseClick() {
        try {
            // 可以打开到具体的许可页面或 GitHub 仓库
            String url = "https://github.com/fingerprintbrowser/fingerprintbrowser";
            
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    logger.info("Opened license URL: {}", url);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to open license URL", e);
            // 如果无法打开浏览器，可以显示一个消息框告知用户手动访问
        }
    }
}