package com.basis.fingerbrowser;

public class Launcher {
    public static void main(String[] args) {
        // macOS 特定设置
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            // 设置应用程序名称在菜单栏中显示
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "FingerprintBrowser");
            System.setProperty("apple.awt.application.name", "FingerprintBrowser");
            
            // 设置 Dock 图标名称
            System.setProperty("apple.awt.application.appearance", "system");
            
            // 启用原生 macOS 菜单栏
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "指纹浏览器");
        }
        
        FingerprintBrowserApp.main(args);
    }
}
