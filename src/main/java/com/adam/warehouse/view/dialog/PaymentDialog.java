package com.adam.warehouse.view.dialog;

import com.adam.warehouse.enums.PaymentMethod;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.util.ThemeManager;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.util.StringConverter;

import java.util.Optional;

/** Collects one payment against an outstanding sale balance. */
public final class PaymentDialog {
    public record PaymentDetails(PaymentMethod method, double amount, double tendered, String reference) { }

    private PaymentDialog() { }

    public static Optional<PaymentDetails> show(double balance, javafx.stage.Window owner) {
        Dialog<PaymentDetails> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("sale.payment.dialog.title"));
        dialog.setHeaderText(LanguageManager.get("sale.balance.due") + ": "
                + String.format(LanguageManager.get("sale.amount.format"), balance));
        if (owner != null) dialog.initOwner(owner);
        dialog.getDialogPane().sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) ThemeManager.applyTheme(newScene);
        });

        ComboBox<PaymentMethod> method = new ComboBox<>();
        method.getItems().setAll(PaymentMethod.CASH, PaymentMethod.CARD,
                PaymentMethod.BANK_TRANSFER, PaymentMethod.OTHER);
        method.setValue(PaymentMethod.CASH);
        method.setConverter(new StringConverter<>() {
            @Override public String toString(PaymentMethod value) { return value == null ? "" : value.toString(); }
            @Override public PaymentMethod fromString(String value) { return null; }
        });
        TextField amount = new TextField(String.format(java.util.Locale.ROOT, "%.2f", balance));
        TextField tendered = new TextField(String.format(java.util.Locale.ROOT, "%.2f", balance));
        TextField reference = new TextField();
        reference.setPromptText(LanguageManager.get("sale.payment.reference.prompt"));
        amount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (method.getValue() != PaymentMethod.CASH) tendered.setText(newValue);
        });
        method.setOnAction(event -> {
            if (method.getValue() != PaymentMethod.CASH) tendered.setText(amount.getText());
        });

        GridPane form = new GridPane();
        form.setPadding(new Insets(12));
        form.setHgap(12);
        form.setVgap(12);
        form.add(new Label(LanguageManager.get("sale.payment.method")), 0, 0);
        form.add(method, 1, 0);
        form.add(new Label(LanguageManager.get("sale.payment.amount")), 0, 1);
        form.add(amount, 1, 1);
        form.add(new Label(LanguageManager.get("sale.amount.tendered")), 0, 2);
        form.add(tendered, 1, 2);
        form.add(new Label(LanguageManager.get("sale.payment.reference")), 0, 3);
        form.add(reference, 1, 3);
        dialog.getDialogPane().setContent(form);
        ButtonType save = new ButtonType(LanguageManager.get("save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button != save) return null;
            try {
                return new PaymentDetails(method.getValue(), parse(amount.getText()),
                        parse(tendered.getText()), reference.getText());
            } catch (NumberFormatException exception) {
                return new PaymentDetails(method.getValue(), Double.NaN,
                        Double.NaN, reference.getText());
            }
        });
        return dialog.showAndWait();
    }

    private static double parse(String value) {
        if (value == null || value.isBlank()) throw new NumberFormatException();
        return Double.parseDouble(value.trim().replace(',', '.'));
    }
}
