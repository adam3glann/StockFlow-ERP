package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.CategoryController;
import com.adam.warehouse.enums.DialogMode;
import com.adam.warehouse.model.Category;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class CategoryDialog extends BaseDialog {

    private final CategoryController categoryController;

    private final DialogMode mode;

    private Category category;

    private Runnable onCategorySaved;

    private final TextField nameField;

    private final TextArea descriptionArea;

    private final Button saveButton;

    private final Button cancelButton;


    public CategoryDialog(DialogMode mode) {

        super(
                mode == DialogMode.ADD
                        ? LanguageManager.get("category.dialog.add")
                        : LanguageManager.get("category.dialog.edit")
        );

        this.mode = mode;

        categoryController = new CategoryController();

        nameField = new TextField();

        descriptionArea = new TextArea();

        saveButton = new Button(
                mode == DialogMode.ADD
                        ? LanguageManager.get("button.add.category")
                        : LanguageManager.get("button.update.category")
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

        descriptionArea.setPrefWidth(250);

        descriptionArea.setPrefRowCount(4);


        grid.add(
                new Label(
                        LanguageManager.get("category.name")
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
                        LanguageManager.get("category.description")
                ),
                0,
                1
        );

        grid.add(
                descriptionArea,
                1,
                1
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
                2
        );


        saveButton.setOnAction(
                e -> saveCategory()
        );


        cancelButton.setOnAction(
                e -> close()
        );


        root.setCenter(grid);

    }


    public void setCategory(Category category) {

        this.category = category;


        if (category == null) {

            return;

        }


        nameField.setText(
                category.getName()
        );


        descriptionArea.setText(
                category.getDescription()
        );

    }


    private void saveCategory() {

        try {


            if (nameField.getText().isBlank()) {


                Alert alert =
                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);


                alert.setHeaderText(null);


                alert.setContentText(
                        LanguageManager.get(
                                "alert.category.name.required"
                        )
                );


                alert.showAndWait();


                return;

            }


            if (mode == DialogMode.ADD) {

                addCategory();

            } else {

                updateCategory();

            }


            if (onCategorySaved != null) {

                onCategorySaved.run();

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


    private void addCategory() {


        Category category = new Category();


        category.setName(
                nameField.getText()
        );


        category.setDescription(
                descriptionArea.getText()
        );


        categoryController.addCategory(
                category
        );


        Alert alert =
                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);


        alert.setHeaderText(null);


        alert.setContentText(
                LanguageManager.get(
                        "category.added"
                )
        );


        alert.showAndWait();

    }


    private void updateCategory() {


        category.setName(
                nameField.getText()
        );


        category.setDescription(
                descriptionArea.getText()
        );


        categoryController.updateCategory(
                category
        );


        Alert alert =
                com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);


        alert.setHeaderText(null);


        alert.setContentText(
                LanguageManager.get(
                        "category.updated"
                )
        );


        alert.showAndWait();

    }


    public void setOnCategorySaved(
            Runnable onCategorySaved
    ) {

        this.onCategorySaved = onCategorySaved;

    }

}