package com.adam.warehouse.enums;

import com.adam.warehouse.util.LanguageManager;
import java.util.Locale;

public enum UserRole {

    ADMIN,

    MANAGER,

    CASHIER,

    STOREKEEPER;

    @Override
    public String toString() {
        return LanguageManager.get("role." + name().toLowerCase(Locale.ROOT));
    }

}
