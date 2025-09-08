package com.basis.fingerbrowser.service;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * 主题管理服务
 * 负责管理应用程序的主题切换功能，支持深色和浅色主题
 */
public class ThemeService {

    private static final Logger logger = LoggerFactory.getLogger(ThemeService.class);
    private static final String THEME_PREFERENCE_KEY = "ui_theme";
    private static final String DARK_THEME = "dark";
    private static final String LIGHT_THEME = "light";

    private static ThemeService instance;
    private final Preferences preferences;
    private final List<Scene> registeredScenes;
    private String currentTheme;
    private final List<ThemeChangeListener> listeners;

    /**
     * 主题变更监听器接口
     */
    public interface ThemeChangeListener {
        void onThemeChanged(String newTheme);
    }

    private ThemeService() {
        this.preferences = Preferences.userNodeForPackage(ThemeService.class);
        this.registeredScenes = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.currentTheme = loadSavedTheme();
        logger.info("ThemeService initialized with theme: {}", currentTheme);
    }

    /**
     * 获取主题服务单例实例
     */
    public static synchronized ThemeService getInstance() {
        if (instance == null) {
            instance = new ThemeService();
        }
        return instance;
    }

    /**
     * 注册场景以支持主题切换
     * @param scene 要注册的场景
     */
    public void registerScene(Scene scene) {
        if (scene != null && !registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
            applyThemeToScene(scene, currentTheme);
            logger.debug("Scene registered for theme management");
        }
    }

    /**
     * 注销场景
     * @param scene 要注销的场景
     */
    public void unregisterScene(Scene scene) {
        registeredScenes.remove(scene);
        logger.debug("Scene unregistered from theme management");
    }

    /**
     * 切换到深色主题
     */
    public void setDarkTheme() {
        setTheme(DARK_THEME);
    }

    /**
     * 切换到浅色主题
     */
    public void setLightTheme() {
        setTheme(LIGHT_THEME);
    }

    /**
     * 切换主题（深色 <-> 浅色）
     */
    public void toggleTheme() {
        String newTheme = DARK_THEME.equals(currentTheme) ? LIGHT_THEME : DARK_THEME;
        setTheme(newTheme);
    }

    /**
     * 设置主题
     * @param theme 主题名称 ("dark" 或 "light")
     */
    public void setTheme(String theme) {
        if (!DARK_THEME.equals(theme) && !LIGHT_THEME.equals(theme)) {
            logger.warn("Invalid theme: {}, using dark theme instead", theme);
            theme = DARK_THEME;
        }

        if (!theme.equals(currentTheme)) {
            String oldTheme = currentTheme;
            final String newTheme = theme;
            currentTheme = theme;

            // 应用主题到所有注册的场景
            registeredScenes.forEach(scene -> applyThemeToScene(scene, newTheme));

            // 保存主题偏好
            saveTheme(theme);

            // 通知监听器
            notifyThemeChanged(theme);

            logger.info("Theme changed from {} to {}", oldTheme, theme);
        }
    }

    /**
     * 获取当前主题
     * @return 当前主题名称
     */
    public String getCurrentTheme() {
        return currentTheme;
    }

    /**
     * 判断当前是否为深色主题
     * @return true 如果是深色主题
     */
    public boolean isDarkTheme() {
        return DARK_THEME.equals(currentTheme);
    }

    /**
     * 判断当前是否为浅色主题
     * @return true 如果是浅色主题
     */
    public boolean isLightTheme() {
        return LIGHT_THEME.equals(currentTheme);
    }

    /**
     * 添加主题变更监听器
     * @param listener 监听器
     */
    public void addThemeChangeListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除主题变更监听器
     * @param listener 监听器
     */
    public void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * 应用主题到指定场景
     */
    public void applyThemeToScene(Scene scene, String theme) {
        try {
            // 移除现有的主题样式类
            scene.getRoot().getStyleClass().removeAll("dark-theme", "light-theme");

            // 添加新的主题样式类
            if (LIGHT_THEME.equals(theme)) {
                scene.getRoot().getStyleClass().add("light-theme");
            } else {
                scene.getRoot().getStyleClass().add("dark-theme");
            }

            logger.debug("Applied {} theme to scene", theme);
        } catch (Exception e) {
            logger.error("Failed to apply theme to scene", e);
        }
    }

    

    /**
     * 从偏好设置加载保存的主题
     */
    private String loadSavedTheme() {
        return preferences.get(THEME_PREFERENCE_KEY, LIGHT_THEME);
    }

    /**
     * 保存主题到偏好设置
     */
    private void saveTheme(String theme) {
        try {
            preferences.put(THEME_PREFERENCE_KEY, theme);
            preferences.flush();
            logger.debug("Theme preference saved: {}", theme);
        } catch (Exception e) {
            logger.error("Failed to save theme preference", e);
        }
    }

    /**
     * 通知所有监听器主题已变更
     */
    private void notifyThemeChanged(String newTheme) {
        listeners.forEach(listener -> {
            try {
                listener.onThemeChanged(newTheme);
            } catch (Exception e) {
                logger.error("Error notifying theme change listener", e);
            }
        });
    }
}
