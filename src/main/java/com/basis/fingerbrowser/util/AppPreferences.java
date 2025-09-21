package com.basis.fingerbrowser.util;

import java.util.prefs.Preferences;

public final class AppPreferences {
    private AppPreferences() {}

    // Shared preferences node for app-wide settings
    public static Preferences getNode() {
        // Use a stable node anchored to the main package
        return Preferences.userRoot().node("/com/basis/fingerbrowser");
    }

    // Keys
    public static final String BROWSER_PATH_KEY = "browser_path";
    public static final String AUTO_SAVE_KEY = "auto_save";
    public static final String CHECK_UPDATES_KEY = "check_updates";
    public static final String LANGUAGE_KEY = "language";

    // Launch flags (configurable)
    public static final String DISABLE_EXTENSIONS_KEY = "disable_extensions"; // default: false
    public static final String DISABLE_BACKGROUND_NETWORKING_KEY = "disable_background_networking"; // default: true
    public static final String DISABLE_COMPONENT_UPDATE_KEY = "disable_component_update"; // default: true
    public static final String V8_MEMORY_TWEAK_KEY = "v8_memory_tweak"; // default: true
}

