package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.SupplierController;
import com.adam.warehouse.enums.DialogMode;
import com.adam.warehouse.model.Supplier;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class SupplierDialog extends BaseDialog {

    private final SupplierController supplierController;

    private final DialogMode mode;

    private Supplier supplier;

    private Runnable onSupplierSaved;

    private final TextField nameField;
    private final TextField phoneField;
    private final TextField emailField;
    private final TextField addressField;

    private final Button saveButton;
    private final Button cancelButton;


    public SupplierDialog(DialogMode mode) {

        super(
                mode == DialogMode.ADD
                        ? LanguageManager.get("supplier.dialog.add")
                        : LanguageManager.get("supplier.dialog.edit")
        );

        this.mode = mode;

        supplierController = new SupplierController();

        nameField = new TextField();
        phoneField = new TextField();
        emailField = new TextField();
        addressField = new TextField();

        saveButton = new Button(

                mode == DialogMode.ADD

                        ? LanguageManager.get("button.add.supplier")

                        : LanguageManager.get("button.update.supplier")

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


        nameField.setPrefWidth(250);
        phoneField.setPrefWidth(250);
        emailField.setPrefWidth(250);
        addressField.setPrefWidth(250);


        grid.add(
                new Label(
                        LanguageManager.get("supplier.name")
                ),
                0,
                0
        );

        grid.add(
                nameField,
                1,
                0
        );


        grid.add(
                new Label(
                        LanguageManager.get("phone")
                ),
                0,
                1
        );

        grid.add(
                phoneField,
                1,
                1
        );


        grid.add(
                new Label(
                        LanguageManager.get("email")
                ),
                0,
                2
        );

        grid.add(
                emailField,
                1,
                2
        );


        grid.add(
                new Label(
                        LanguageManager.get("address")
                ),
                0,
                3
        );

        grid.add(
                addressField,
                1,
                3
        );


        saveButton.getStyleClass()
                .add("primary-button");


        GridPane buttons = new GridPane();

        buttons.setHgap(10);


        buttons.add(
                saveButton,
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


        saveButton.setOnAction(
                e -> saveSupplier()
        );


        cancelButton.setOnAction(
                e -> close()
        );


        root.setCenter(grid);

    }


    public void setSupplier(Supplier supplier) {

        this.supplier = supplier;


        if (supplier == null) {

            return;

        }


        nameField.setText(
                supplier.getName()
        );

        phoneField.setText(
                supplier.getPhone()
        );

        emailField.setText(
                supplier.getEmail()
        );

        addressField.setText(
                supplier.getAddress()
        );

    }


    private void saveSupplier() {

        try {


            if (nameField.getText().isBlank()
                    || phoneField.getText().isBlank()) {


                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);


                alert.setHeaderText(null);


                alert.setContentText(
                        LanguageManager.get(
                                "alert.supplier.required"
                        )
                );


                alert.showAndWait();


                return;

            }


            if (mode == DialogMode.ADD) {

                addSupplier();

            } else {

                updateSupplier();

            }


            if (onSupplierSaved != null) {

                onSupplierSaved.run();

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


    private void addSupplier() {

        Supplier supplier = new Supplier();


        supplier.setName(
                nameField.getText()
        );


        supplier.setPhone(
                phoneField.getText()
        );


        supplier.setEmail(
                emailField.getText()
        );


        supplier.setAddress(
                addressField.getText()
        );


        supplier.setActive(true);


        supplierController.addSupplier(supplier);


        Alert alert =
                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);


        alert.setHeaderText(null);


        alert.setContentText(
                LanguageManager.get(
                        "supplier.added"
                )
        );


        alert.showAndWait();

    }


    private void updateSupplier() {

        supplier.setName(
                nameField.getText()
        );


        supplier.setPhone(
                phoneField.getText()
        );


        supplier.setEmail(
                emailField.getText()
        );


        supplier.setAddress(
                addressField.getText()
        );


        supplierController.updateSupplier(supplier);


        Alert alert =
                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);


        alert.setHeaderText(null);


        alert.setContentText(
                LanguageManager.get(
                        "supplier.updated"
                )
        );


        alert.showAndWait();

    }


    public void setOnSupplierSaved(
            Runnable onSupplierSaved
    ) {

        this.onSupplierSaved = onSupplierSaved;

    }

}