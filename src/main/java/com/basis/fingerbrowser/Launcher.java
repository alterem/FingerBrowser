package com.basis.fingerbrowser;

public class Launcher {
    public static void main(String[] args) {
        // macOS specific settings
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            // Set the application name in the menu bar. This is the standard property.
            System.setProperty("apple.awt.application.name", "FingerprintBrowser");
            
            // Use the native macOS menu bar.
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        
        FingerprintBrowserApp.main(args);
    }
}
