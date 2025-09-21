package com.basis.fingerbrowser.util;

import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public final class ToastUtil {
    private ToastUtil() {}

    public static void showToast(Scene scene, String message) {
        if (scene == null) return;
        Platform.runLater(() -> {
            Window window = scene.getWindow();
            if (window == null) return;

            HBox toast = buildToast(message);
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);
            popup.getContent().add(toast);

            // Show first to get correct width/height
            popup.show(window);

            double margin = 20.0;
            double x = window.getX() + window.getWidth() - toast.getWidth() - margin;
            double y = window.getY() + margin;
            popup.setX(x);
            popup.setY(y);

            // Reposition on window moves/resizes during lifetime
            final var xProp = window.xProperty();
            final var yProp = window.yProperty();
            final var wProp = window.widthProperty();
            final var listeners = new javafx.beans.InvalidationListener() {
                @Override public void invalidated(javafx.beans.Observable observable) {
                    double nx = window.getX() + window.getWidth() - toast.getWidth() - margin;
                    double ny = window.getY() + margin;
                    popup.setX(nx);
                    popup.setY(ny);
                }
            };
            xProp.addListener(listeners);
            yProp.addListener(listeners);
            wProp.addListener(listeners);

            // Animations on content node (popup itself can't animate)
            toast.setOpacity(0);
            toast.setTranslateY(-10);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(180), toast);
            slideIn.setFromY(-10);
            slideIn.setToY(0);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(240), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(2.2));

            SequentialTransition seq = new SequentialTransition();
            seq.getChildren().addAll(fadeIn, slideIn, fadeOut);
            seq.setOnFinished(e -> {
                xProp.removeListener(listeners);
                yProp.removeListener(listeners);
                wProp.removeListener(listeners);
                popup.hide();
            });
            seq.play();
        });
    }

    public static void showToast(Node anyNode, String message) {
        if (anyNode != null && anyNode.getScene() != null) {
            showToast(anyNode.getScene(), message);
        }
    }

    private static HBox buildToast(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("toast-text");
        HBox box = new HBox(label);
        box.getStyleClass().add("toast-pane");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setSpacing(8);
        return box;
    }
}
