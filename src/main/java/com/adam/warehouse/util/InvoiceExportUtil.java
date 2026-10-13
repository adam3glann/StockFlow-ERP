package com.adam.warehouse.util;

import com.adam.warehouse.model.Purchase;
import com.adam.warehouse.model.PurchaseItem;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Creates shareable CSV invoices for completed sales and purchases. */
public final class InvoiceExportUtil {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private InvoiceExportUtil() { }
    public static void sale(Path path, Sale sale) throws IOException {
        List<List<String>> rows = header(String.format(LanguageManager.get("invoice.sale.title"), sale.getSaleId()), sale.getSaleDate(),
                LanguageManager.get("invoice.customer"), blankAsWalkIn(sale.getCustomerName()));
        rows.add(List.of(LanguageManager.get("invoice.product"), LanguageManager.get("invoice.quantity"),
                LanguageManager.get("invoice.unit.price"), LanguageManager.get("invoice.subtotal")));
        for (SaleItem item : sale.getItems()) rows.add(List.of(item.getProductName(), String.valueOf(item.getQuantity()), money(item.getSellingPrice()), money(item.getSubtotal())));
        rows.add(List.of("", "", LanguageManager.get("invoice.total"), money(sale.getTotalAmount())));
        if (sale.getNotes() != null && !sale.getNotes().isBlank()) rows.add(List.of(LanguageManager.get("invoice.notes"), sale.getNotes()));
        CsvExportUtil.write(path, rows);
    }
    public static void purchase(Path path, Purchase purchase) throws IOException {
        List<List<String>> rows = header(String.format(LanguageManager.get("invoice.purchase.title"), purchase.getPurchaseId()), purchase.getPurchaseDate(),
                LanguageManager.get("invoice.supplier"), safe(purchase.getSupplierName()));
        rows.add(List.of(LanguageManager.get("invoice.product"), LanguageManager.get("invoice.quantity"),
                LanguageManager.get("invoice.unit.price"), LanguageManager.get("invoice.subtotal")));
        for (PurchaseItem item : purchase.getItems()) rows.add(List.of(item.getProductName(), String.valueOf(item.getQuantity()), money(item.getPurchasePrice()), money(item.getSubtotal())));
        rows.add(List.of("", "", LanguageManager.get("invoice.total"), money(purchase.getTotalAmount())));
        if (purchase.getNotes() != null && !purchase.getNotes().isBlank()) rows.add(List.of(LanguageManager.get("invoice.notes"), purchase.getNotes()));
        CsvExportUtil.write(path, rows);
    }
    private static List<List<String>> header(String title, java.time.LocalDateTime time, String party, String partyName) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(LanguageManager.get("app.title"), title));
        rows.add(List.of(LanguageManager.get("invoice.date"), time == null ? "" : TIME.format(time)));
        rows.add(List.of(party, partyName));
        return rows;
    }
    private static String blankAsWalkIn(String value) { return value == null || value.isBlank() ? LanguageManager.get("invoice.walk.in") : value; }
    private static String safe(String value) { return value == null ? "" : value; }
    private static String money(double value) { return String.format(java.util.Locale.US, "%.2f %s", value, LanguageManager.get("currency.egp")); }
}
