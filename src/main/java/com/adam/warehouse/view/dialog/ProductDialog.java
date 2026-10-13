package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.CategoryController;
import com.adam.warehouse.controller.ProductController;
import com.adam.warehouse.enums.DialogMode;
import com.adam.warehouse.model.Category;
import com.adam.warehouse.model.Product;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.geometry.NodeOrientation;

import java.util.UUID;

public class ProductDialog extends BaseDialog {

    private final ProductController productController;
    private final CategoryController categoryController;

    private final DialogMode mode;

    private Product product;

    private Runnable onProductSaved;

    private final TextField barcodeField;
    private final TextField nameField;

    private final ComboBox<Category> categoryComboBox;

    private final TextField purchasePriceField;
    private final TextField sellingPriceField;
    private final TextField quantityField;
    private final TextField minimumStockField;

    private final Button saveButton;
    private final Button cancelButton;

    public ProductDialog(DialogMode mode) {

        super(
                mode == DialogMode.ADD
                        ? LanguageManager.get("product.dialog.add")
                        : LanguageManager.get("product.dialog.edit")
        );

        this.mode = mode;

        productController = new ProductController();
        categoryController = new CategoryController();

        barcodeField = new TextField();
        nameField = new TextField();

        categoryComboBox = new ComboBox<>();

        purchasePriceField = new TextField();
        sellingPriceField = new TextField();
        quantityField = new TextField();
        minimumStockField = new TextField();

        saveButton = new Button(

                mode == DialogMode.ADD

                        ? LanguageManager.get("button.add.product")

                        : LanguageManager.get("button.update.product")

        );

        cancelButton = new Button(
                LanguageManager.get("cancel")
        );

        createForm();

    }

    private void createForm() {

        GridPane grid = new GridPane();

        grid.setPadding(new Insets(20));

        grid.setHgap(15);

        grid.setVgap(15);

        barcodeField.setPrefWidth(250);
        barcodeField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        nameField.setPrefWidth(250);
        categoryComboBox.setPrefWidth(250);
        purchasePriceField.setPrefWidth(250);
        sellingPriceField.setPrefWidth(250);
        quantityField.setPrefWidth(250);
        if (mode == DialogMode.EDIT) {
            quantityField.setEditable(false);
            quantityField.setTooltip(new Tooltip(LanguageManager.get("product.quantity.edit.help")));
        }
        minimumStockField.setPrefWidth(250);

        grid.add(

                new Label(
                        LanguageManager.get("product.barcode")
                ),

                0,

                0

        );

        Button generateBarcodeButton = new Button(LanguageManager.get("barcode.generate"));
        generateBarcodeButton.setOnAction(event -> barcodeField.setText(generateBarcodeValue()));
        HBox barcodeInput = new HBox(8, barcodeField, generateBarcodeButton);
        grid.add(barcodeInput, 1, 0);

        grid.add(

                new Label(
                        LanguageManager.get("product.name")
                ),

                0,

                1

        );

        grid.add(nameField, 1, 1);

        grid.add(

                new Label(
                        LanguageManager.get("product.category")
                ),

                0,

                2

        );

        grid.add(categoryComboBox, 1, 2);

        grid.add(

                new Label(
                        LanguageManager.get("product.purchase.price")
                ),

                0,

                3

        );

        grid.add(purchasePriceField, 1, 3);

        grid.add(

                new Label(
                        LanguageManager.get("product.selling.price")
                ),

                0,

                4

        );

        grid.add(sellingPriceField, 1, 4);

        grid.add(

                new Label(
                        LanguageManager.get("product.quantity")
                ),

                0,

                5

        );

        grid.add(quantityField, 1, 5);

        grid.add(

                new Label(
                        LanguageManager.get("product.minimum.stock")
                ),

                0,

                6

        );

        grid.add(minimumStockField, 1, 6);

        categoryComboBox.setItems(
                categoryController.getCategories()
        );

        saveButton.getStyleClass().add("primary-button");

        GridPane buttons = new GridPane();

        buttons.setHgap(10);

        buttons.add(saveButton, 0, 0);

        buttons.add(cancelButton, 1, 0);

        grid.add(buttons, 1, 7);

        saveButton.setOnAction(e -> saveProduct());

        cancelButton.setOnAction(e -> close());

        root.setCenter(grid);

    }

    private String generateBarcodeValue() {
        // Code 128 accepts this compact SKU format; UUID randomness makes collisions negligible.
        return "SF" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase(java.util.Locale.ROOT);
    }

    public void setProduct(Product product) {

        this.product = product;

        if (product == null) {
            return;
        }

        barcodeField.setText(product.getBarcode());

        nameField.setText(product.getName());

        purchasePriceField.setText(
                String.valueOf(product.getPurchasePrice())
        );

        sellingPriceField.setText(
                String.valueOf(product.getSellingPrice())
        );

        quantityField.setText(
                String.valueOf(product.getQuantity())
        );

        minimumStockField.setText(
                String.valueOf(product.getMinimumStock())
        );

        for (Category category : categoryComboBox.getItems()) {

            if (category.getCategoryId() == product.getCategoryId()) {

                categoryComboBox.setValue(category);

                break;

            }

        }

    }
    private void saveProduct() {

        try {

            if (barcodeField.getText().isBlank()
                    || nameField.getText().isBlank()
                    || categoryComboBox.getValue() == null
                    || purchasePriceField.getText().isBlank()
                    || sellingPriceField.getText().isBlank()
                    || quantityField.getText().isBlank()
                    || minimumStockField.getText().isBlank()) {

                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);

                alert.setHeaderText(null);

                alert.setContentText(
                        LanguageManager.get("alert.fill.fields")
                );

                alert.showAndWait();

                return;

            }

            if (mode == DialogMode.ADD) {

                addProduct();

            } else {

                updateProduct();

            }

            if (onProductSaved != null) {

                onProductSaved.run();

            }

            close();

        } catch (Exception ex) {

            Alert alert =
                    com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);

            alert.setHeaderText(
                    LanguageManager.get("error")
            );

            alert.setContentText(
                    com.adam.warehouse.util.LanguageManager.message(ex.getMessage())
            );

            alert.showAndWait();

        }

    }


    private void addProduct() {

        Product product = new Product();

        product.setBarcode(
                barcodeField.getText()
        );

        product.setName(
                nameField.getText()
        );

        product.setCategoryId(
                categoryComboBox
                        .getValue()
                        .getCategoryId()
        );

        product.setPurchasePrice(
                Double.parseDouble(
                        purchasePriceField.getText()
                )
        );

        product.setSellingPrice(
                Double.parseDouble(
                        sellingPriceField.getText()
                )
        );

        product.setQuantity(
                Integer.parseInt(
                        quantityField.getText()
                )
        );

        product.setMinimumStock(
                Integer.parseInt(
                        minimumStockField.getText()
                )
        );

        product.setActive(true);


        productController.addProduct(product);


        Alert alert =
                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);

        alert.setHeaderText(null);

        alert.setContentText(
                LanguageManager.get("product.added")
        );

        alert.showAndWait();

    }


    private void updateProduct() {

        product.setBarcode(
                barcodeField.getText()
        );

        product.setName(
                nameField.getText()
        );

        product.setCategoryId(
                categoryComboBox
                        .getValue()
                        .getCategoryId()
        );

        product.setPurchasePrice(
                Double.parseDouble(
                        purchasePriceField.getText()
                )
        );

        product.setSellingPrice(
                Double.parseDouble(
                        sellingPriceField.getText()
                )
        );

        product.setQuantity(
                Integer.parseInt(
                        quantityField.getText()
                )
        );

        product.setMinimumStock(
                Integer.parseInt(
                        minimumStockField.getText()
                )
        );


        productController.updateProduct(product);


        Alert alert =
                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);

        alert.setHeaderText(null);

        alert.setContentText(
                LanguageManager.get("product.updated")
        );

        alert.showAndWait();

    }


    public void setOnProductSaved(
            Runnable onProductSaved
    ) {

        this.onProductSaved = onProductSaved;

    }

}
