package com.basis.fingerbrowser;

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
        // 加载主界面
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        // 设置标题和图标
        primaryStage.setTitle("FingerprintBrowser");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/app_icon.png"))));

        // 设置场景
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/application.css")).toExternalForm());
        primaryStage.setScene(scene);

        // 设置窗口属性
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // 显示主窗口
        primaryStage.show();

        // 设置关闭处理
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
