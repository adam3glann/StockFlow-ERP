package com.adam.warehouse.view;



import com.adam.warehouse.controller.CategoryController;

import com.adam.warehouse.enums.DialogMode;

import com.adam.warehouse.model.Category;

import com.adam.warehouse.util.LanguageManager;

import com.adam.warehouse.view.dialog.CategoryDialog;

import javafx.geometry.Insets;

import javafx.scene.control.*;

import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.BorderPane;

import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;



public class CategoryView extends BorderPane {



    private final CategoryController categoryController;



    private final TableView<Category> categoryTable;



    private final TextField searchField;



    private final Button addButton;

    private final Button editButton;

    private final Button deactivateButton;

    private final Button refreshButton;



    public CategoryView() {



        categoryController = new CategoryController();



        categoryTable = new TableView<>();



        searchField = new TextField();



        addButton = new Button(

                LanguageManager.get("button.add.category")

        );



        editButton = new Button(

                LanguageManager.get("edit")

        );



        deactivateButton = new Button(

                LanguageManager.get("deactivate")

        );



        refreshButton = new Button(

                LanguageManager.get("refresh")

        );



        setPadding(new Insets(15));



        createTop();



        createTable();



        createBottom();



        registerEvents();



        loadCategories();



    }



    private void createTop() {



        Label title = new Label(

                "📁 " + LanguageManager.get("category.title")

        );



        title.getStyleClass().add("page-title");



        searchField.setPromptText(

                LanguageManager.get("category.search")

        );



        HBox.setHgrow(

                searchField,

                Priority.ALWAYS

        );



        addButton.getStyleClass().add("primary-button");



        HBox top = new HBox(10);



        top.setPadding(new Insets(10));



        top.getChildren().addAll(



                title,



                searchField,



                addButton



        );



        setTop(top);



    }



    private void createTable() {



        TableColumn<Category, Integer> idColumn =

                new TableColumn<>(

                        LanguageManager.get("category.id")

                );



        idColumn.setCellValueFactory(

                new PropertyValueFactory<>("categoryId")

        );



        TableColumn<Category, String> nameColumn =

                new TableColumn<>(

                        LanguageManager.get("category.name")

                );



        nameColumn.setCellValueFactory(

                new PropertyValueFactory<>("name")

        );



        TableColumn<Category, String> descriptionColumn =

                new TableColumn<>(

                        LanguageManager.get("category.description")

                );



        descriptionColumn.setCellValueFactory(

                new PropertyValueFactory<>("description")

        );



        categoryTable.getColumns().addAll(



                idColumn,



                nameColumn,



                descriptionColumn



        );



        categoryTable.setColumnResizePolicy(

                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN

        );



        categoryTable.setPrefHeight(500);



        setCenter(categoryTable);



    }



    private void createBottom() {



        HBox bottom = new HBox(10);



        bottom.setPadding(new Insets(10));



        bottom.getChildren().addAll(



                editButton,



                deactivateButton,



                refreshButton



        );



        setBottom(bottom);



    }



    private void loadCategories() {



        categoryTable.setItems(

                categoryController.getCategories()

        );



    }



    private void registerEvents() {



        refreshButton.setOnAction(e ->

                loadCategories()

        );



        searchField.textProperty().addListener((obs, oldValue, newValue) -> {



            if (newValue == null || newValue.isBlank()) {



                loadCategories();



                return;



            }



            categoryTable.getItems().clear();



            for (Category category : categoryController.getCategories()) {



                if (category.getName().toLowerCase()

                        .contains(newValue.toLowerCase())) {



                    categoryTable.getItems().add(category);



                }



            }



        });



        addButton.setOnAction(e -> {



            CategoryDialog dialog =

                    new CategoryDialog(DialogMode.ADD);



            dialog.setOnCategorySaved(this::loadCategories);



            dialog.showAndWait();



        });



        editButton.setOnAction(e -> {



            Category selectedCategory =

                    categoryTable.getSelectionModel().getSelectedItem();



            if (selectedCategory == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.category.select")

                );



                alert.showAndWait();



                return;



            }



            CategoryDialog dialog =

                    new CategoryDialog(DialogMode.EDIT);



            dialog.setCategory(selectedCategory);



            dialog.setOnCategorySaved(this::loadCategories);



            dialog.showAndWait();



        });

        deactivateButton.setOnAction(e -> {



            Category selectedCategory =

                    categoryTable.getSelectionModel().getSelectedItem();



            if (selectedCategory == null) {



                Alert alert =

                        com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.WARNING);



                alert.setHeaderText(null);



                alert.setContentText(

                        LanguageManager.get("alert.category.select")

                );



                alert.showAndWait();



                return;



            }



            Alert confirmation =

                    com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.CONFIRMATION);



            confirmation.setTitle(

                    LanguageManager.get("alert.category.deactivate.title")

            );



            confirmation.setHeaderText(

                    LanguageManager.get("alert.category.deactivate.header")

            );



            confirmation.setContentText(

                    String.format(

                            LanguageManager.get("alert.category.deactivate.confirm"),

                            selectedCategory.getName()

                    )

            );



            confirmation.showAndWait().ifPresent(response -> {



                if (response == ButtonType.OK) {



                    categoryController.deactivateCategory(

                            selectedCategory.getCategoryId()

                    );



                    loadCategories();



                    Alert success =

                            com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.INFORMATION);



                    success.setHeaderText(null);



                    success.setContentText(

                            LanguageManager.get("alert.category.deactivated")

                    );



                    success.showAndWait();



                }



            });



        });



        categoryTable.setRowFactory(tv -> {



            TableRow<Category> row = new TableRow<>();



            row.setOnMouseClicked(event -> {



                if (event.getClickCount() == 2 && !row.isEmpty()) {



                    CategoryDialog dialog =

                            new CategoryDialog(DialogMode.EDIT);



                    dialog.setCategory(row.getItem());



                    dialog.setOnCategorySaved(this::loadCategories);



                    dialog.showAndWait();



                }



            });



            return row;



        });



    }



}