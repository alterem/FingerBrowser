package com.basis.fingerbrowser.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;
import com.basis.fingerbrowser.util.DialogUtil;

/**
 * 关于对话框控制器
 */
public class AboutController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AboutController.class);
    private static final String LICENSE_URL = "https://github.com/alterem/fingerbrowser";

    @FXML private Hyperlink licenseLink;
    @FXML private Label versionLabel;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing AboutController");
        
        // 设置超链接点击事件
        if (licenseLink != null) {
            licenseLink.setOnAction(e -> handleLicenseClick());
        }
        // 设置版本号
        if (versionLabel != null) {
            versionLabel.setText(com.basis.fingerbrowser.util.AppInfo.getVersion());
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
            String url = LICENSE_URL;
            
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    logger.info("Opened license URL: {}", url);
                    return;
                }
            }
            // Fallback: show message with URL
            DialogUtil.createInformationAlert("开源许可", "请在浏览器访问：\n" + url).showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open license URL", e);
            DialogUtil.createInformationAlert("开源许可", "无法自动打开浏览器，请手动访问：\n" + LICENSE_URL).showAndWait();
        }
    }
}
