package com.adam.warehouse.util;

/** Shared bounds for values stored in PostgreSQL NUMERIC(14,2) columns. */
public final class MoneyValidation {
    public static final double MAX_AMOUNT = 999_999_999_999.99;

    private MoneyValidation() { }

    public static boolean isValid(double amount) {
        return Double.isFinite(amount) && amount >= 0 && amount <= MAX_AMOUNT;
    }

    public static boolean isValidPositive(double amount) {
        return isValid(amount) && amount > 0;
    }
}
