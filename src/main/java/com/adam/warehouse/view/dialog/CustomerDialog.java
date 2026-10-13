package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.CustomerController;
import com.adam.warehouse.enums.DialogMode;
import com.adam.warehouse.model.Customer;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class CustomerDialog extends BaseDialog {

    private final CustomerController customerController;

    private final DialogMode mode;

    private Customer customer;

    private Runnable onCustomerSaved;

    private final TextField fullNameField;
    private final TextField phoneField;
    private final TextField emailField;
    private final TextArea addressArea;

    private final Button saveButton;
    private final Button cancelButton;


    public CustomerDialog(DialogMode mode) {

        super(
                mode == DialogMode.ADD
                        ? LanguageManager.get("customer.dialog.add")
                        : LanguageManager.get("customer.dialog.edit")
        );

        this.mode = mode;

        customerController = new CustomerController();

        fullNameField = new TextField();
        phoneField = new TextField();
        emailField = new TextField();
        addressArea = new TextArea();

        saveButton = new Button(
                mode == DialogMode.ADD
                        ? LanguageManager.get("button.add.customer")
                        : LanguageManager.get("button.update.customer")
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

        fullNameField.setPrefWidth(250);
        phoneField.setPrefWidth(250);
        emailField.setPrefWidth(250);

        addressArea.setPrefRowCount(3);
        addressArea.setPrefWidth(250);


        grid.add(
                new Label(
                        LanguageManager.get("customer.full.name")
                ),
                0,
                0
        );

        grid.add(
                fullNameField,
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
                addressArea,
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
                e -> saveCustomer()
        );


        cancelButton.setOnAction(
                e -> close()
        );


        root.setCenter(grid);

    }


    public void setCustomer(Customer customer) {

        this.customer = customer;


        if (customer == null) {

            return;

        }


        fullNameField.setText(
                customer.getFullName()
        );

        phoneField.setText(
                customer.getPhone()
        );

        emailField.setText(
                customer.getEmail()
        );

        addressArea.setText(
                customer.getAddress()
        );

    }


    private void saveCustomer() {

        try {

            if (fullNameField.getText().isBlank()
                    || phoneField.getText().isBlank()) {


                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);


                alert.setHeaderText(null);


                alert.setContentText(
                        LanguageManager.get(
                                "alert.customer.required"
                        )
                );


                alert.showAndWait();


                return;

            }


            if (!emailField.getText().isBlank()
                    && !emailField.getText().contains("@")) {


                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);


                alert.setHeaderText(null);


                alert.setContentText(
                        LanguageManager.get(
                                "alert.customer.email.invalid"
                        )
                );


                alert.showAndWait();


                return;

            }


            if (mode == DialogMode.ADD) {

                addCustomer();

            } else {

                updateCustomer();

            }


            if (onCustomerSaved != null) {

                onCustomerSaved.run();

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


    private void addCustomer() {

        Customer customer = new Customer();


        customer.setFullName(
                fullNameField.getText()
        );


        customer.setPhone(
                phoneField.getText()
        );


        customer.setEmail(
                emailField.getText()
        );


        customer.setAddress(
                addressArea.getText()
        );


        customer.setActive(true);


        customerController.addCustomer(customer);


        Alert alert =
                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);


        alert.setHeaderText(null);


        alert.setContentText(
                LanguageManager.get(
                        "customer.added"
                )
        );


        alert.showAndWait();

    }


    private void updateCustomer() {

        customer.setFullName(
                fullNameField.getText()
        );


        customer.setPhone(
                phoneField.getText()
        );


        customer.setEmail(
                emailField.getText()
        );


        customer.setAddress(
                addressArea.getText()
        );


        customerController.updateCustomer(customer);


        Alert alert =
                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);


        alert.setHeaderText(null);


        alert.setContentText(
                LanguageManager.get(
                        "customer.updated"
                )
        );


        alert.showAndWait();

    }


    public void setOnCustomerSaved(
            Runnable onCustomerSaved
    ) {

        this.onCustomerSaved = onCustomerSaved;

    }

}