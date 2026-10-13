package com.adam.warehouse.util;

import com.adam.warehouse.model.Purchase;
import com.adam.warehouse.model.PurchaseItem;
import com.adam.warehouse.model.ReportSummary;
import com.adam.warehouse.model.Sale;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.model.BranchIdentity;
import com.adam.warehouse.model.TopSellingProduct;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Writes compact, single-page PDF exports with Java2D-shaped vector text (including Arabic). */
public final class SimplePdfInvoiceExporter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(new AffineTransform(), true, true);

    private SimplePdfInvoiceExporter() { }

    public static void purchaseInvoice(Path path, Purchase purchase) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(LanguageManager.get("app.title"));
        BranchIdentity branch = BranchIdentityManager.get();
        lines.add(branch.name() + (branch.location() == null || branch.location().isBlank() ? "" : " — " + branch.location()));
        lines.add(LanguageManager.get("branch.code") + ": " + branch.code());
        lines.add(String.format(LanguageManager.get("invoice.purchase.title"), purchase.getPurchaseId()));
        lines.add(LanguageManager.get("invoice.date") + ": " + date(purchase.getPurchaseDate()));
        lines.add(LanguageManager.get("invoice.supplier") + ": " + safe(purchase.getSupplierName()));
        lines.add("");
        lines.add(columns());
        lines.add(separator());
        for (PurchaseItem item : purchase.getItems()) {
            lines.add(String.format(Locale.ROOT, "%-30s %6d %12.2f %12.2f",
                    truncate(item.getProductName(), 30), item.getQuantity(), item.getPurchasePrice(), item.getSubtotal()));
        }
        lines.add(separator());
        lines.add(String.format(LanguageManager.get("invoice.total.format"), purchase.getTotalAmount(), LanguageManager.get("currency.egp")));
        if (purchase.getNotes() != null && !purchase.getNotes().isBlank()) {
            lines.add(LanguageManager.get("invoice.notes") + ": " + purchase.getNotes());
        }
        write(path, lines);
    }

    public static void saleInvoice(Path path, Sale sale) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(LanguageManager.get("app.title"));
        BranchIdentity branch = BranchIdentityManager.get();
        lines.add(branch.name() + (branch.location() == null || branch.location().isBlank() ? "" : " — " + branch.location()));
        lines.add(LanguageManager.get("branch.code") + ": " + branch.code());
        lines.add(String.format(LanguageManager.get("invoice.sale.title"), sale.getSaleId()));
        lines.add(LanguageManager.get("invoice.date") + ": " + date(sale.getSaleDate()));
        lines.add(LanguageManager.get("invoice.customer") + ": " + blankAsWalkIn(sale.getCustomerName()));
        lines.add("");
        lines.add(columns());
        lines.add(separator());
        for (SaleItem item : sale.getItems()) {
            lines.add(String.format(Locale.ROOT, "%-30s %6d %12.2f %12.2f",
                    truncate(item.getProductName(), 30), item.getQuantity(), item.getSellingPrice(), item.getSubtotal()));
        }
        lines.add(separator());
        lines.add(String.format(LanguageManager.get("invoice.total.format"), sale.getTotalAmount(), LanguageManager.get("currency.egp")));
        lines.add(LanguageManager.get("report.metric.discounts") + ": " + money(sale.getDiscountAmount()));
        lines.add(LanguageManager.get("report.metric.tax") + " (" + sale.getTaxRate() + "%): " + money(sale.getTaxAmount()));
        lines.add(LanguageManager.get("report.metric.collected") + ": " + money(sale.getAmountPaid()));
        lines.add(LanguageManager.get("report.metric.balance.due") + ": " + money(sale.getBalanceDue()));
        if (sale.getNotes() != null && !sale.getNotes().isBlank()) {
            lines.add(LanguageManager.get("invoice.notes") + ": " + sale.getNotes());
        }
        write(path, lines);
    }

    public static void reportSummary(Path path, ReportSummary summary, List<TopSellingProduct> topProducts) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(LanguageManager.get("app.title"));
        lines.add(LanguageManager.get("report.pdf.report.title") + ": " + safe(summary.getTitle()));
        lines.add(LanguageManager.get("report.generated.label") + ": " + TIME.format(LocalDateTime.now()));
        lines.add("");
        lines.add(LanguageManager.get("report.column.metric") + " | " + LanguageManager.get("inventory.column.value"));
        lines.add(separator());
        addMetric(lines, "report.metric.sales", summary.getTotalSales());
        addMetric(lines, "report.metric.purchases", summary.getTotalPurchases());
        addMetric(lines, "report.metric.gross.profit", summary.getTotalProfit());
        addMetric(lines, "report.metric.returns", summary.getTotalReturns());
        addMetric(lines, "report.metric.gross.profit.after.refunds", summary.getGrossProfitLessRefunds());
        addMetric(lines, "report.metric.discounts", summary.getTotalDiscounts());
        addMetric(lines, "report.metric.tax", summary.getTotalTax());
        addMetric(lines, "report.metric.collected", summary.getTotalCollected());
        addMetric(lines, "report.metric.balance.due", summary.getOutstandingBalance());
        addMetric(lines, "report.payment.cash", summary.getCashCollected());
        addMetric(lines, "report.payment.card", summary.getCardCollected());
        addMetric(lines, "report.payment.transfer", summary.getTransferCollected());
        addMetric(lines, "report.metric.inventory.value", summary.getInventoryValue());
        lines.add(LanguageManager.get("report.metric.transactions") + ": " + summary.getTotalTransactions());
        lines.add(LanguageManager.get("report.metric.products.sold") + ": " + summary.getTotalProductsSold());
        addMetric(lines, "report.metric.average.sale", summary.getAverageSaleValue());
        lines.add(LanguageManager.get("report.metric.profit.margin") + ": " + String.format(Locale.US, "%.1f%%", summary.getProfitMargin()));
        lines.add(LanguageManager.get("report.metric.low.stock") + ": " + summary.getLowStockProducts());

        if (topProducts != null && !topProducts.isEmpty()) {
            lines.add("");
            lines.add(LanguageManager.get("report.metric.top.products"));
            for (TopSellingProduct product : topProducts) {
                lines.add(String.format(Locale.ROOT, "%-32s %8d %12.2f",
                        truncate(product.name(), 32), product.quantitySold(), product.revenue()));
            }
        }
        write(path, lines);
    }

    private static void addMetric(List<String> lines, String labelKey, double value) {
        lines.add(LanguageManager.get(labelKey) + ": " + money(value));
    }

    private static String columns() {
        return LanguageManager.get("invoice.product") + " | " + LanguageManager.get("invoice.quantity") + " | "
                + LanguageManager.get("invoice.unit.price") + " | " + LanguageManager.get("invoice.subtotal");
    }

    private static String separator() { return "------------------------------------------------------------------------"; }
    private static String date(LocalDateTime value) { return value == null ? "" : TIME.format(value); }
    private static String money(double value) { return String.format(Locale.US, "%.2f %s", value, LanguageManager.get("currency.egp")); }
    private static String blankAsWalkIn(String value) { return value == null || value.isBlank() ? LanguageManager.get("invoice.walk.in") : value; }
    private static String safe(String value) { return value == null ? "" : value; }
    private static String truncate(String value, int length) { String text = safe(value); return text.length() <= length ? text : text.substring(0, length - 3) + "..."; }

    private static void write(Path path, List<String> lines) throws IOException {
        StringBuilder content = new StringBuilder("q\n");
        double baseline = 790;
        for (int index = 0; index < lines.size() && baseline > 36; index++) {
            Font font = new Font(Font.SANS_SERIF, index < 2 ? Font.BOLD : Font.PLAIN, index == 0 ? 17 : index == 1 ? 13 : 10);
            appendOutlinedLine(content, lines.get(index), font, baseline);
            baseline -= index < 2 ? 22 : 15;
        }
        content.append("Q\n");
        byte[] stream = content.toString().getBytes(StandardCharsets.US_ASCII);
        List<byte[]> objects = List.of(
                bytes("<< /Type /Catalog /Pages 2 0 R >>"),
                bytes("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R >>"),
                bytes("<< /Length " + stream.length + " >>\nstream\n" + new String(stream, StandardCharsets.US_ASCII) + "endstream"));
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(bytes("%PDF-1.4\n% StockFlow vector text\n"));
        List<Integer> offsets = new ArrayList<>(); offsets.add(0);
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(pdf.size()); pdf.write(bytes((index + 1) + " 0 obj\n")); pdf.write(objects.get(index)); pdf.write(bytes("\nendobj\n"));
        }
        int xref = pdf.size(); pdf.write(bytes("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (int offset : offsets.subList(1, offsets.size())) pdf.write(bytes(String.format(Locale.ROOT, "%010d 00000 n \n", offset)));
        pdf.write(bytes("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));
        Files.write(path, pdf.toByteArray());
    }

    private static void appendOutlinedLine(StringBuilder output, String value, Font font, double baseline) {
        if (value == null || value.isEmpty()) return;
        TextLayout layout = new TextLayout(value, font, FONT_CONTEXT);
        double x = LanguageManager.getLocale().getLanguage().equals("ar")
                ? Math.max(42, 553 - layout.getAdvance()) : 42;
        AffineTransform transform = new AffineTransform(1, 0, 0, -1, x, baseline);
        PathIterator iterator = layout.getOutline(null).getPathIterator(transform, 0.35);
        double[] coordinates = new double[6];
        while (!iterator.isDone()) {
            int segment = iterator.currentSegment(coordinates);
            if (segment == PathIterator.SEG_MOVETO) output.append(number(coordinates[0])).append(' ').append(number(coordinates[1])).append(" m\n");
            else if (segment == PathIterator.SEG_LINETO) output.append(number(coordinates[0])).append(' ').append(number(coordinates[1])).append(" l\n");
            else if (segment == PathIterator.SEG_CLOSE) output.append("h\n");
            iterator.next();
        }
        output.append("f\n");
    }

    private static String number(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.US_ASCII); }
}
