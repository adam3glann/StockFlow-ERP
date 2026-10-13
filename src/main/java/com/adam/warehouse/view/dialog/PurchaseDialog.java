package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.PurchaseController;
import com.adam.warehouse.controller.SupplierController;
import com.adam.warehouse.model.Purchase;
import com.adam.warehouse.model.PurchaseItem;
import com.adam.warehouse.model.Supplier;
import com.adam.warehouse.session.SessionManager;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDateTime;

public class PurchaseDialog extends BaseDialog {

    private final PurchaseController purchaseController;

    private final SupplierController supplierController;

    private final ComboBox<Supplier> supplierComboBox;

    private final TableView<PurchaseItem> itemsTable;

    private final TextArea notesArea;

    private final Label totalLabel;

    private final Button addItemButton;

    private final Button removeItemButton;

    private final Button saveButton;

    private final Button cancelButton;


    public PurchaseDialog() {

        super(
                LanguageManager.get(
                        "purchase.dialog.new"
                )
        );

        purchaseController = new PurchaseController();

        supplierController = new SupplierController();

        supplierComboBox = new ComboBox<>();

        itemsTable = new TableView<>();

        notesArea = new TextArea();

        totalLabel = new Label(
                String.format(
                        LanguageManager.get(
                                "purchase.total"
                        ),
                        0.00
                )
        );

        addItemButton = new Button(
                LanguageManager.get(
                        "button.add.item"
                )
        );

        removeItemButton = new Button(
                LanguageManager.get(
                        "button.remove.item"
                )
        );

        saveButton = new Button(
                LanguageManager.get(
                        "button.save.purchase"
                )
        );

        cancelButton = new Button(
                LanguageManager.get(
                        "cancel"
                )
        );


        createTable();

        createForm();

        registerEvents();

    }


    private void createTable() {

        TableColumn<PurchaseItem, String> productColumn =
                new TableColumn<>(
                        LanguageManager.get(
                                "purchase.product"
                        )
                );


        productColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "productName"
                )
        );


        TableColumn<PurchaseItem, Integer> quantityColumn =
                new TableColumn<>(
                        LanguageManager.get(
                                "quantity"
                        )
                );


        quantityColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "quantity"
                )
        );


        TableColumn<PurchaseItem, Double> priceColumn =
                new TableColumn<>(
                        LanguageManager.get(
                                "purchase.price"
                        )
                );


        priceColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "purchasePrice"
                )
        );


        TableColumn<PurchaseItem, Double> subtotalColumn =
                new TableColumn<>(
                        LanguageManager.get(
                                "purchase.subtotal"
                        )
                );


        subtotalColumn.setCellValueFactory(
                new PropertyValueFactory<>(
                        "subtotal"
                )
        );


        itemsTable.getColumns().addAll(

                productColumn,

                quantityColumn,

                priceColumn,

                subtotalColumn

        );


        itemsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        itemsTable.setPlaceholder(

                new Label(
                        LanguageManager.get(
                                "purchase.empty.items"
                        )
                )

        );


        itemsTable.setPrefHeight(250);

    }
    private void createForm() {

        GridPane grid = new GridPane();

        grid.setPadding(new Insets(20));

        grid.setHgap(15);

        grid.setVgap(15);


        supplierComboBox.setPrefWidth(300);

        supplierComboBox.setItems(
                supplierController.getSuppliers()
        );


        notesArea.setPrefRowCount(4);


        saveButton.getStyleClass()
                .add("primary-button");


        addItemButton.getStyleClass()
                .add("primary-button");


        grid.add(

                new Label(
                        LanguageManager.get(
                                "supplier"
                        )
                ),

                0,

                0

        );


        grid.add(
                supplierComboBox,
                1,
                0
        );


        grid.add(

                new Label(
                        LanguageManager.get(
                                "purchase.items"
                        )
                ),

                0,

                1

        );


        grid.add(
                itemsTable,
                1,
                1
        );


        HBox itemButtons = new HBox(10);


        itemButtons.getChildren().addAll(

                addItemButton,

                removeItemButton

        );


        grid.add(
                itemButtons,
                1,
                2
        );


        grid.add(
                totalLabel,
                1,
                3
        );


        grid.add(

                new Label(
                        LanguageManager.get(
                                "purchase.notes"
                        )
                ),

                0,

                4

        );


        grid.add(
                notesArea,
                1,
                4
        );


        HBox buttons = new HBox(10);


        buttons.getChildren().addAll(

                saveButton,

                cancelButton

        );


        grid.add(
                buttons,
                1,
                5
        );


        cancelButton.setOnAction(
                e -> close()
        );


        root.setCenter(grid);

    }


    private void registerEvents() {


        addItemButton.setOnAction(e -> {


            PurchaseItemDialog dialog =
                    new PurchaseItemDialog();


            dialog.showAndWait();


            if (dialog.isSaved()) {


                itemsTable.getItems().add(

                        dialog.getPurchaseItem()

                );


                updateTotal();

            }

        });


        removeItemButton.setOnAction(e -> {


            PurchaseItem selectedItem =

                    itemsTable
                            .getSelectionModel()
                            .getSelectedItem();


            if (selectedItem != null) {


                itemsTable.getItems()
                        .remove(selectedItem);


                updateTotal();

            }

        });


        saveButton.setOnAction(
                e -> savePurchase()
        );

    }


    private void updateTotal() {


        double total = 0;


        for (PurchaseItem item : itemsTable.getItems()) {


            total += item.getSubtotal();

        }


        totalLabel.setText(

                String.format(

                        LanguageManager.get(
                                "purchase.total"
                        ),

                        total

                )

        );

    }


    private void savePurchase() {


        try {


            if (supplierComboBox.getValue() == null) {


                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(
                                Alert.AlertType.WARNING
                        );


                alert.setHeaderText(null);


                alert.setContentText(

                        LanguageManager.get(
                                "alert.purchase.supplier"
                        )

                );


                alert.showAndWait();


                return;

            }


            if (itemsTable.getItems().isEmpty()) {


                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(
                                Alert.AlertType.WARNING
                        );


                alert.setHeaderText(null);


                alert.setContentText(

                        LanguageManager.get(
                                "alert.purchase.items"
                        )

                );


                alert.showAndWait();


                return;

            }


            Purchase purchase = new Purchase();


            purchase.setSupplierId(

                    supplierComboBox
                            .getValue()
                            .getSupplierId()

            );


            purchase.setSupplierName(

                    supplierComboBox
                            .getValue()
                            .getName()

            );


            purchase.setPurchaseDate(
                    LocalDateTime.now()
            );

            purchase.setEmployeeId(
                    SessionManager.requireCurrentEmployee().getEmployeeId()
            );


            purchase.setNotes(
                    notesArea.getText()
            );


            for (PurchaseItem item : itemsTable.getItems()) {


                purchase.addItem(item);

            }


            purchase.calculateTotalAmount();


            purchaseController.savePurchase(
                    purchase
            );


            Alert alert =
                    com.adam.warehouse.util.AlertUtil.create(
                            Alert.AlertType.INFORMATION
                    );


            alert.setHeaderText(null);


            alert.setContentText(

                    LanguageManager.get(
                            "purchase.saved"
                    )

            );


            alert.showAndWait();


            close();


        } catch (Exception ex) {


            Alert alert =
                    com.adam.warehouse.util.AlertUtil.create(
                            Alert.AlertType.ERROR
                    );


            alert.setHeaderText(

                    LanguageManager.get(
                            "error"
                    )

            );


            alert.setContentText(
                    com.adam.warehouse.util.LanguageManager.message(ex.getMessage())
            );


            alert.showAndWait();

        }

    }

}
