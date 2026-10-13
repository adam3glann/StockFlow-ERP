package com.adam.warehouse.enums;

import com.adam.warehouse.util.LanguageManager;

public enum PaymentMethod {
    CASH, CARD, BANK_TRANSFER, OTHER, LEGACY;

    @Override
    public String toString() {
        return LanguageManager.get("payment.method." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
