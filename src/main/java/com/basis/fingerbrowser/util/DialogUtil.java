package com.basis.fingerbrowser.util;

import com.basis.fingerbrowser.service.ThemeService;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

public class DialogUtil {

    public static Alert createInformationAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        applyTheme(alert);
        return alert;
    }

    public static Alert createErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        applyTheme(alert);
        return alert;
    }

    public static Alert createConfirmationAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyTheme(alert);
        return alert;
    }

    private static void applyTheme(Alert alert) {
        // Remove default window decorations for a modern, borderless look
        alert.initStyle(StageStyle.TRANSPARENT);

        DialogPane dialogPane = alert.getDialogPane();
        ThemeService themeService = ThemeService.getInstance();

        // Defer setting the scene's background until it's available
        alert.setOnShowing(event -> {
            dialogPane.getScene().setFill(Color.TRANSPARENT);
        });

        // Remove existing theme classes to prevent conflicts
        dialogPane.getStyleClass().removeAll("dark-theme", "light-theme");

        // Add the main application stylesheet
        String cssPath = DialogUtil.class.getResource("/styles/application.css").toExternalForm();
        if (!dialogPane.getStylesheets().contains(cssPath)) {
            dialogPane.getStylesheets().add(cssPath);
        }

        // Apply the current theme class
        dialogPane.getStyleClass().add(themeService.getCurrentTheme() + "-theme");
    }
}
