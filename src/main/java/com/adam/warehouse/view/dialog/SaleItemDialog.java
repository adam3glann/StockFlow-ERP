package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.ProductController;
import com.adam.warehouse.model.Product;
import com.adam.warehouse.model.SaleItem;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class SaleItemDialog extends BaseDialog {

    private final ProductController productController;

    private final ComboBox<Product> productComboBox;
    private final TextField barcodeField = new TextField();

    private final TextField quantityField;

    private final TextField sellingPriceField;

    private final Button addButton;

    private final Button cancelButton;

    private SaleItem saleItem;

    private boolean saved;


    public SaleItemDialog() {

        super(
                LanguageManager.get(
                        "sale.item.dialog.add"
                )
        );

        productController = new ProductController();

        productComboBox = new ComboBox<>();

        quantityField = new TextField();

        sellingPriceField = new TextField();

        addButton = new Button(
                LanguageManager.get("button.add")
        );

        cancelButton = new Button(
                LanguageManager.get("cancel")
        );

        createForm();
        setOnShown(event -> barcodeField.requestFocus());

    }


    private void createForm() {

        GridPane grid = new GridPane();

        grid.setPadding(new Insets(20));

        grid.setHgap(15);

        grid.setVgap(15);


        productComboBox.setPrefWidth(250);
        barcodeField.setPromptText(LanguageManager.get("barcode.scan.hint"));
        barcodeField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        barcodeField.setOnAction(event -> selectScannedProduct());

        quantityField.setPrefWidth(250);

        sellingPriceField.setPrefWidth(250);

        sellingPriceField.setEditable(false);


        productComboBox.setItems(
                productController.getProducts()
        );


        grid.add(new Label(LanguageManager.get("barcode.scan.transaction")), 0, 0);
        grid.add(barcodeField, 1, 0);

        grid.add(
                new Label(
                        LanguageManager.get("sale.product")
                ),
                0,
                1
        );

        grid.add(
                productComboBox,
                1,
                1
        );


        grid.add(
                new Label(
                        LanguageManager.get("sale.quantity")
                ),
                0,
                2
        );

        grid.add(
                quantityField,
                1,
                2
        );


        grid.add(
                new Label(
                        LanguageManager.get("sale.selling.price")
                ),
                0,
                3
        );

        grid.add(
                sellingPriceField,
                1,
                3
        );


        addButton.getStyleClass()
                .add("primary-button");


        GridPane buttons = new GridPane();

        buttons.setHgap(10);


        buttons.add(
                addButton,
                0,
                0
        );


        buttons.add(
                cancelButton,
                1,
                0
        );


        grid.add(
                buttons,
                1,
                4
        );


        cancelButton.setOnAction(
                e -> close()
        );


        root.setCenter(grid);


        productComboBox.valueProperty()
                .addListener((observable, oldValue, newValue) -> {

                    if (newValue != null) {

                        sellingPriceField.setText(
                                String.valueOf(
                                        newValue.getSellingPrice()
                                )
                        );

                    }

                });


        addButton.setOnAction(
                e -> saveItem()
        );

    }

    private void selectScannedProduct() {
        try {
            Product product = productController.getProductByBarcode(barcodeField.getText().trim());
            if (product == null) throw new IllegalArgumentException(LanguageManager.get("barcode.not.found"));
            productComboBox.setValue(product);
            if (quantityField.getText().isBlank()) quantityField.setText("1");
            quantityField.requestFocus();
        } catch (RuntimeException exception) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText(com.adam.warehouse.util.LanguageManager.message(exception.getMessage()));
            alert.showAndWait();
            barcodeField.requestFocus();
        }
    }


    public SaleItem getSaleItem() {

        return saleItem;

    }


    public boolean isSaved() {

        return saved;

    }


    private void saveItem() {

        try {


            if (productComboBox.getValue() == null
                    || quantityField.getText().isBlank()) {


                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);


                alert.setHeaderText(null);


                alert.setContentText(
                        LanguageManager.get(
                                "alert.sale.item.required"
                        )
                );


                alert.showAndWait();


                return;

            }


            Product product =
                    productComboBox.getValue();


            int quantity =
                    Integer.parseInt(
                            quantityField.getText()
                    );


            if (quantity <= 0) {

                throw new IllegalArgumentException(
                        LanguageManager.get(
                                "alert.quantity.invalid"
                        )
                );

            }


            saleItem = new SaleItem();


            saleItem.setProductId(
                    product.getProductId()
            );


            saleItem.setProductName(
                    product.getName()
            );


            saleItem.setQuantity(
                    quantity
            );


            saleItem.setSellingPrice(
                    product.getSellingPrice()
            );


            // Capture the cost price now, at the moment of sale — later changes to the
            // product's purchase price must never retroactively change this line's profit.
            saleItem.setCostPrice(
                    product.getPurchasePrice()
            );


            saleItem.setSubtotal(
                    saleItem.getQuantity()
                            *
                            saleItem.getSellingPrice()
            );


            saved = true;


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

}
