package com.adam.warehouse.view.dialog;



import com.adam.warehouse.controller.EmployeeController;

import com.adam.warehouse.enums.DialogMode;

import com.adam.warehouse.enums.UserRole;

import com.adam.warehouse.model.Employee;

import com.adam.warehouse.util.LanguageManager;

import javafx.geometry.Insets;

import javafx.scene.control.*;

import javafx.scene.layout.GridPane;



public class EmployeeDialog extends BaseDialog {



    private final EmployeeController employeeController;



    private final DialogMode mode;



    private Employee employee;



    private Runnable onEmployeeSaved;





    private final TextField firstNameField;



    private final TextField lastNameField;



    private final TextField usernameField;



    private final PasswordField passwordField;





    private final ComboBox<UserRole> roleComboBox;





    private final TextField phoneField;



    private final TextField emailField;



    private final TextArea notesArea;





    private final Button saveButton;



    private final Button cancelButton;





    public EmployeeDialog(DialogMode mode) {



        super(

                mode == DialogMode.ADD

                        ? LanguageManager.get("employee.dialog.add")

                        : LanguageManager.get("employee.dialog.edit")

        );





        this.mode = mode;





        employeeController = new EmployeeController();





        firstNameField = new TextField();



        lastNameField = new TextField();



        usernameField = new TextField();



        passwordField = new PasswordField();





        roleComboBox = new ComboBox<>();





        phoneField = new TextField();



        emailField = new TextField();



        notesArea = new TextArea();





        saveButton = new Button(



                mode == DialogMode.ADD



                        ? LanguageManager.get("button.add.employee")



                        : LanguageManager.get("button.update.employee")



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







        notesArea.setPrefRowCount(4);



        notesArea.setPrefWidth(250);







        roleComboBox.getItems().addAll(



                UserRole.ADMIN,



                UserRole.MANAGER,



                UserRole.CASHIER,



                UserRole.STOREKEEPER



        );







        grid.add(



                new Label(

                        LanguageManager.get("employee.first.name")

                ),



                0,



                0



        );



        grid.add(firstNameField,1,0);







        grid.add(



                new Label(

                        LanguageManager.get("employee.last.name")

                ),



                0,



                1



        );



        grid.add(lastNameField,1,1);







        grid.add(



                new Label(

                        LanguageManager.get("employee.username")

                ),



                0,



                2



        );



        grid.add(usernameField,1,2);







        grid.add(



                new Label(

                        LanguageManager.get("employee.password")

                ),



                0,



                3



        );



        grid.add(passwordField,1,3);







        grid.add(



                new Label(

                        LanguageManager.get("employee.role")

                ),



                0,



                4



        );



        grid.add(roleComboBox,1,4);







        grid.add(



                new Label(

                        LanguageManager.get("phone")

                ),



                0,



                5



        );



        grid.add(phoneField,1,5);







        grid.add(



                new Label(

                        LanguageManager.get("email")

                ),



                0,



                6



        );



        grid.add(emailField,1,6);







        grid.add(



                new Label(

                        LanguageManager.get("employee.notes")

                ),



                0,



                7



        );



        grid.add(notesArea,1,7);







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

                8

        );





        saveButton.setOnAction(

                e -> saveEmployee()

        );





        cancelButton.setOnAction(

                e -> close()

        );





        root.setCenter(grid);



    }

    private void saveEmployee() {



        try {



            if (firstNameField.getText().isBlank()

                    || lastNameField.getText().isBlank()

                    || usernameField.getText().isBlank()

                    || (mode == DialogMode.ADD

                    && passwordField.getText().isBlank())

                    || roleComboBox.getValue() == null) {





                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);





                alert.setHeaderText(null);





                alert.setContentText(

                        LanguageManager.get(

                                "alert.employee.required"

                        )

                );





                alert.showAndWait();





                return;



            }





            if (mode == DialogMode.ADD) {



                addEmployee();



            } else {



                updateEmployee();



            }





            if (onEmployeeSaved != null) {



                onEmployeeSaved.run();



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







    private void addEmployee() {





        Employee employee = new Employee();





        employee.setFirstName(

                firstNameField.getText()

        );





        employee.setLastName(

                lastNameField.getText()

        );





        employee.setUsername(

                usernameField.getText()

        );





        employee.setPassword(

                passwordField.getText()

        );





        employee.setRole(

                roleComboBox.getValue()

        );





        employee.setPhone(

                phoneField.getText()

        );





        employee.setEmail(

                emailField.getText()

        );





        employee.setNotes(

                notesArea.getText()

        );





        employee.setActive(true);







        employeeController.addEmployee(employee);







        Alert alert =

                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);







        alert.setHeaderText(null);







        alert.setContentText(

                LanguageManager.get(

                        "employee.added"

                )

        );







        alert.showAndWait();



    }







    private void updateEmployee() {





        employee.setFirstName(

                firstNameField.getText()

        );





        employee.setLastName(

                lastNameField.getText()

        );





        employee.setUsername(

                usernameField.getText()

        );





        employee.setPassword(

                passwordField.getText()

        );





        employee.setRole(

                roleComboBox.getValue()

        );





        employee.setPhone(

                phoneField.getText()

        );





        employee.setEmail(

                emailField.getText()

        );





        employee.setNotes(

                notesArea.getText()

        );







        employeeController.updateEmployee(employee);







        Alert alert =

                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);







        alert.setHeaderText(null);







        alert.setContentText(

                LanguageManager.get(

                        "employee.updated"

                )

        );







        alert.showAndWait();



    }







    public void setEmployee(Employee employee) {





        this.employee = employee;







        if (employee == null) {



            return;



        }







        firstNameField.setText(

                employee.getFirstName()

        );





        lastNameField.setText(

                employee.getLastName()

        );





        usernameField.setText(

                employee.getUsername()

        );







        passwordField.clear();







        passwordField.setPromptText(

                LanguageManager.get(

                        "password.keep.current"

                )

        );







        roleComboBox.setValue(

                employee.getRole()

        );







        phoneField.setText(

                employee.getPhone()

        );







        emailField.setText(

                employee.getEmail()

        );







        notesArea.setText(

                employee.getNotes()

        );



    }







    public void setOnEmployeeSaved(

            Runnable onEmployeeSaved

    ) {



        this.onEmployeeSaved = onEmployeeSaved;



    }



}