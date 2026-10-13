package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.ProductController;
import com.adam.warehouse.model.Product;
import com.adam.warehouse.model.PurchaseItem;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class PurchaseItemDialog extends BaseDialog {

    private final ProductController productController;

    private final ComboBox<Product> productComboBox;
    private final TextField barcodeField = new TextField();

    private final TextField quantityField;

    private final TextField purchasePriceField;

    private final Label subtotalLabel;

    private final Button addButton;

    private final Button cancelButton;

    private PurchaseItem purchaseItem;

    private boolean saved;


    public PurchaseItemDialog() {

        super(
                LanguageManager.get(
                        "purchase.item.dialog.add"
                )
        );

        productController = new ProductController();

        productComboBox = new ComboBox<>();

        quantityField = new TextField("1");

        purchasePriceField = new TextField();

        subtotalLabel = new Label("0.00");

        addButton = new Button(
                LanguageManager.get("button.add")
        );

        cancelButton = new Button(
                LanguageManager.get("cancel")
        );

        createForm();

        registerEvents();
        setOnShown(event -> barcodeField.requestFocus());

    }


    private void createForm() {

        GridPane grid = new GridPane();

        grid.setPadding(new Insets(20));

        grid.setHgap(15);

        grid.setVgap(15);


        productComboBox.setPrefWidth(260);
        barcodeField.setPromptText(LanguageManager.get("barcode.scan.hint"));
        barcodeField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        quantityField.setPrefWidth(260);

        purchasePriceField.setPrefWidth(260);


        productComboBox.setItems(
                productController.getProducts()
        );


        grid.add(new Label(LanguageManager.get("barcode.scan.transaction")), 0, 0);
        grid.add(barcodeField, 1, 0);

        grid.add(
                new Label(
                        LanguageManager.get("purchase.product")
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
                        LanguageManager.get("purchase.quantity")
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
                        LanguageManager.get("purchase.price")
                ),
                0,
                3
        );

        grid.add(
                purchasePriceField,
                1,
                3
        );


        grid.add(
                new Label(
                        LanguageManager.get("purchase.subtotal")
                ),
                0,
                4
        );

        grid.add(
                subtotalLabel,
                1,
                4
        );


        addButton.getStyleClass()
                .add("primary-button");


        addButton.setDefaultButton(true);


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
                5
        );


        root.setCenter(grid);

    }


    private void registerEvents() {

        barcodeField.setOnAction(event -> {
            try {
                Product product = productController.getProductByBarcode(barcodeField.getText().trim());
                if (product == null) throw new IllegalArgumentException(LanguageManager.get("barcode.not.found"));
                productComboBox.setValue(product);
                quantityField.requestFocus();
                quantityField.selectAll();
            } catch (RuntimeException exception) {
                Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);
                alert.setHeaderText(null);
                alert.setContentText(com.adam.warehouse.util.LanguageManager.message(exception.getMessage()));
                alert.showAndWait();
                barcodeField.requestFocus();
            }
        });

        productComboBox.setOnAction(e -> {

            Product product =
                    productComboBox.getValue();


            if (product != null) {


                purchasePriceField.setText(

                        String.format(
                                "%.2f",
                                product.getPurchasePrice()
                        )

                );


                calculateSubtotal();

            }

        });


        quantityField.textProperty()
                .addListener(
                        (obs, oldValue, newValue)
                                -> calculateSubtotal()
                );


        purchasePriceField.textProperty()
                .addListener(
                        (obs, oldValue, newValue)
                                -> calculateSubtotal()
                );


        addButton.setOnAction(
                e -> saveItem()
        );


        cancelButton.setOnAction(
                e -> close()
        );

    }


    private void calculateSubtotal() {

        try {

            int quantity =
                    Integer.parseInt(
                            quantityField.getText()
                    );


            double price =
                    Double.parseDouble(
                            purchasePriceField.getText()
                    );


            subtotalLabel.setText(
                    String.format(
                            "%.2f",
                            quantity * price
                    )
            );


        } catch (Exception ex) {

            subtotalLabel.setText("0.00");

        }

    }


    public PurchaseItem getPurchaseItem() {

        return purchaseItem;

    }


    public boolean isSaved() {

        return saved;

    }


    private void saveItem() {

        try {

            Product product =
                    productComboBox.getValue();


            if (product == null) {

                throw new IllegalArgumentException(
                        LanguageManager.get(
                                "alert.product.select"
                        )
                );

            }


            int quantity =
                    Integer.parseInt(
                            quantityField.getText().trim()
                    );


            if (quantity <= 0) {

                throw new IllegalArgumentException(
                        LanguageManager.get(
                                "alert.quantity.invalid"
                        )
                );

            }


            double purchasePrice =
                    Double.parseDouble(
                            purchasePriceField.getText().trim()
                    );


            if (purchasePrice < 0) {

                throw new IllegalArgumentException(
                        LanguageManager.get(
                                "alert.price.negative"
                        )
                );

            }


            purchaseItem = new PurchaseItem();


            purchaseItem.setProductId(
                    product.getProductId()
            );


            purchaseItem.setProductName(
                    product.getName()
            );


            purchaseItem.setQuantity(
                    quantity
            );


            purchaseItem.setPurchasePrice(
                    purchasePrice
            );


            purchaseItem.setSubtotal(
                    quantity * purchasePrice
            );


            saved = true;


            close();


        } catch (NumberFormatException ex) {


            com.adam.warehouse.util.AlertUtil.create(
                    Alert.AlertType.ERROR,
                    LanguageManager.get(
                            "alert.numeric.invalid"
                    )
            ).showAndWait();


        } catch (Exception ex) {


            com.adam.warehouse.util.AlertUtil.create(
                    Alert.AlertType.ERROR,
                    com.adam.warehouse.util.LanguageManager.message(ex.getMessage())
            ).showAndWait();

        }

    }

}
