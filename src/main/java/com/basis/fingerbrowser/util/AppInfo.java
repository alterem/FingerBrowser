package com.basis.fingerbrowser.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppInfo {
    private static final String PROPERTIES_PATH = "/app.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppInfo.class.getResourceAsStream(PROPERTIES_PATH)) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException ignored) { }
    }

    private AppInfo() {}

    public static String getVersion() {
        String fromProps = PROPS.getProperty("version");
        if (fromProps != null && !fromProps.isBlank()) return fromProps.trim();
        String pkgVersion = AppInfo.class.getPackage().getImplementationVersion();
        if (pkgVersion != null && !pkgVersion.isBlank()) return pkgVersion.trim();
        return "1.0.0"; // fallback
    }
}

