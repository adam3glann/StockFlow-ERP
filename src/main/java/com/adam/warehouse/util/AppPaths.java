package com.adam.warehouse.util;

import java.nio.file.Path;

/** Resolves writable per-user storage for settings and private connection configuration. */
public final class AppPaths {
    private AppPaths() { }

    public static Path userDataDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) return Path.of(appData, "StockFlow");
        return Path.of(System.getProperty("user.home", "."), ".stockflow");
    }
}
