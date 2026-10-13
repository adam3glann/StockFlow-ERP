package com.adam.warehouse.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class LanguageManager {

    private static Locale locale = Locale.ENGLISH;

    private static ResourceBundle bundle =
            ResourceBundle.getBundle(
                    "languages.messages",
                    locale
            );
    private static final Pattern NON_KEY_CHARACTER = Pattern.compile("[^a-z0-9]+");

    private LanguageManager() {

    }

    public static void setLanguage(Locale newLocale) {

        locale = newLocale != null && "ar".equals(newLocale.getLanguage())
                ? Locale.forLanguageTag("ar") : Locale.ENGLISH;
        Locale.setDefault(locale);

        bundle = ResourceBundle.getBundle(
                "languages.messages",
                    locale
        );

    }

    public static Locale getLocale() {

        return locale;

    }

    public static String get(String key) {

        if (bundle.containsKey(key)) {

            return bundle.getString(key);

        }

        return "!" + key + "!";

    }

    /** Localizes a known enum or database code, retaining unknown codes for forward compatibility. */
    public static String code(String prefix, String value) {
        if (value == null || value.isBlank()) return "";
        String key = prefix + "." + NON_KEY_CHARACTER.matcher(value.trim().toLowerCase(Locale.ROOT))
                .replaceAll(".").replaceAll("^\\.|\\.$", "");
        return bundle.containsKey(key) ? bundle.getString(key) : value;
    }

    /** Converts a known English validation/DAO message to the active language. */
    public static String message(String value) {
        if (value == null || value.isBlank()) return get("database.error.generic");
        if (value.matches("(?is).*(jdbc:|sqlstate|org\\.postgresql|postgresql exception|connection refused|no suitable driver|fatal:).*")) {
            return get("database.error.generic");
        }
        String slug = NON_KEY_CHARACTER.matcher(value.trim().toLowerCase(Locale.ROOT))
                .replaceAll(".").replaceAll("^\\.|\\.$", "");
        String key = "error.message." + slug;
        if (bundle.containsKey(key)) return bundle.getString(key);
        if (value.startsWith("Cannot return ")) return get("error.message.return.quantity.stock");
        if (value.startsWith("Not enough stock for ")) return get("error.message.stock.insufficient");
        if (value.startsWith("Insufficient stock or unavailable product")) return get("error.message.stock.insufficient");
        if (value.startsWith("Unable to restock ")) return get("error.message.stock.restock");
        if (value.startsWith("Return quantity must be greater than zero for ")) return get("error.message.return.quantity.positive");
        if (value.startsWith("Unable to ") || value.startsWith("Failed to ")) {
            return get("database.error.generic");
        }
        return value;
    }

    /** Renders system-generated activity records in the selected language, including older records. */
    public static String activityDescription(String value) {
        if (value == null) return "";
        Matcher sale = Pattern.compile("Recorded sale with (\\d+) item\\(s\\), total ([0-9.]+)\\.").matcher(value);
        if (sale.matches()) return String.format(get("activity.description.sale"), sale.group(1), sale.group(2));
        Matcher purchase = Pattern.compile("Recorded purchase with (\\d+) item\\(s\\), total ([0-9.]+)\\.").matcher(value);
        if (purchase.matches()) return String.format(get("activity.description.purchase"), purchase.group(1), purchase.group(2));
        Matcher returned = Pattern.compile("Processed return for Sale #(\\d+) with (\\d+) item\\(s\\), refund ([0-9.]+)\\.").matcher(value);
        if (returned.matches()) return String.format(get("activity.description.return"), returned.group(1), returned.group(2), returned.group(3));
        Matcher purchaseReturn = Pattern.compile("Returned (\\d+) item\\(s\\) to supplier for Purchase #(\\d+), credit ([0-9.]+)\\.").matcher(value);
        if (purchaseReturn.matches()) return String.format(get("activity.description.purchase.return"), purchaseReturn.group(1), purchaseReturn.group(2), purchaseReturn.group(3));
        return value;
    }

}
