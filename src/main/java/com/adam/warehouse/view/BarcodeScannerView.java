package com.adam.warehouse.view;

import com.adam.warehouse.controller.ProductController;
import com.adam.warehouse.model.Product;
import com.adam.warehouse.util.BarcodeScanner;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

/** Visible scan station for hardware scanners and camera/photo images. */
public class BarcodeScannerView extends BorderPane {
    private final ProductController products = new ProductController();
    private final TextField barcode = new TextField();
    private final Label result = new Label(LanguageManager.get("scanner.ready"));
    private final GridPane details = new GridPane();

    public BarcodeScannerView() {
        setPadding(new Insets(24));
        createLayout();
    }

    private void createLayout() {
        Label title = new Label(LanguageManager.get("scanner.title"));
        title.getStyleClass().add("page-title");
        Label help = new Label(LanguageManager.get("scanner.help"));
        barcode.setPromptText(LanguageManager.get("barcode.scan.hint"));
        barcode.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        barcode.setOnAction(event -> findProduct());
        HBox.setHgrow(barcode, Priority.ALWAYS);
        Button find = new Button(LanguageManager.get("scanner.find"));
        Button image = new Button(LanguageManager.get("barcode.scan.image"));
        find.getStyleClass().add("primary-button");
        find.setOnAction(event -> findProduct());
        image.setOnAction(event -> scanImage());
        result.getStyleClass().add("section-title");
        details.setHgap(16);
        details.setVgap(12);
        VBox panel = new VBox(16, title, help, new HBox(10, barcode, find, image), result, details);
        panel.getStyleClass().add("dashboard-card");
        panel.setPadding(new Insets(24));
        setCenter(panel);
    }

    private void scanImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LanguageManager.get("barcode.scan.image.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                LanguageManager.get("barcode.scan.image.files"), "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;
        try {
            barcode.setText(BarcodeScanner.decode(file.toPath()));
            findProduct();
        } catch (RuntimeException exception) {
            showError(com.adam.warehouse.util.LanguageManager.message(exception.getMessage()));
        }
    }

    private void findProduct() {
        try {
            String code = barcode.getText() == null ? "" : barcode.getText().trim();
            if (code.isEmpty()) throw new IllegalArgumentException(LanguageManager.get("scanner.required"));
            Product product = products.getProductByBarcode(code);
            if (product == null) throw new IllegalArgumentException(LanguageManager.get("barcode.not.found"));
            showProduct(product);
        } catch (RuntimeException exception) {
            details.getChildren().clear();
            result.setText(LanguageManager.get("scanner.not.found"));
            showError(com.adam.warehouse.util.LanguageManager.message(exception.getMessage()));
            barcode.requestFocus();
        }
    }

    private void showProduct(Product product) {
        result.setText(LanguageManager.get("scanner.found"));
        details.getChildren().clear();
        addDetail(0, LanguageManager.get("product.name"), product.getName());
        addDetail(1, LanguageManager.get("barcode"), product.getBarcode());
        addDetail(2, LanguageManager.get("category"), product.getCategoryName());
        addDetail(3, LanguageManager.get("inventory.on.hand"), String.valueOf(product.getQuantity()));
        addDetail(4, LanguageManager.get("inventory.minimum"), String.valueOf(product.getMinimumStock()));
        addDetail(5, LanguageManager.get("inventory.stock.status"), product.isOutOfStock()
                ? LanguageManager.get("inventory.out.of.stock")
                : product.isLowStock() ? LanguageManager.get("inventory.low.stock") : LanguageManager.get("inventory.in.stock"));
    }

    private void addDetail(int row, String label, String value) {
        details.add(new Label(label + ":"), 0, row);
        details.add(new Label(value == null ? "" : value), 1, row);
    }

    private void showError(String message) {
        Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
