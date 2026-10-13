package com.adam.warehouse.view;



import com.adam.warehouse.model.Employee;
import com.adam.warehouse.controller.LoginController;

import com.adam.warehouse.session.SessionManager;

import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.util.PermissionManager;

import javafx.geometry.Pos;

import javafx.scene.Scene;

import javafx.scene.control.Button;

import javafx.scene.control.Label;

import javafx.scene.layout.BorderPane;

import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;

import javafx.scene.layout.VBox;

import javafx.scene.text.Font;

import javafx.scene.text.FontWeight;

import javafx.stage.Stage;



public class DashboardView extends BorderPane {



    private final BorderPane contentPane = new BorderPane();



    public DashboardView() {



        createTop();



        createLeft();



        setCenter(contentPane);



        showDashboard();



    }



    private void createTop() {



        Label title = new Label(
                LanguageManager.get("app.title")
        );



        title.setFont(

                Font.font(

                        "Segoe UI",

                        FontWeight.BOLD,

                        24

                )

        );



        Employee employee = SessionManager.getCurrentEmployee();



        String employeeName =

                employee == null

                        ? LanguageManager.get("user.guest")

                        : employee.getFullName();



        String employeeRole =

                employee == null

                        ? ""

                        : employee.getRoleName();



        Label name = new Label(employeeName);



        name.getStyleClass().add("user-name");



        Label role = new Label(employeeRole);



        role.getStyleClass().add("user-role");



        VBox userDetails = new VBox(

                2,

                name,

                role

        );



        Button logoutButton =

                new Button(

                        LanguageManager.get("button.logout")

                );



        logoutButton.getStyleClass().add(

                "primary-button"

        );



        logoutButton.setOnAction(event ->

                returnToLogin()

        );



        HBox top = new HBox(

                16,

                title,

                userDetails,

                logoutButton

        );



        top.setAlignment(Pos.CENTER_LEFT);



        HBox.setHgrow(

                title,

                Priority.ALWAYS

        );



        top.getStyleClass().add(

                "top-bar"

        );



        setTop(top);



    }



    private void createLeft() {



        VBox menu = new VBox(4);



        menu.getStyleClass().add("sidebar");



        Button dashboard =

                createMenuButton(

                        LanguageManager.get("menu.dashboard")

                );



        Button products =

                createMenuButton(

                        LanguageManager.get("menu.products")

                );



        Button categories =

                createMenuButton(

                        LanguageManager.get("menu.categories")

                );



        Button suppliers =

                createMenuButton(

                        LanguageManager.get("menu.suppliers")

                );



        Button customers =

                createMenuButton(

                        LanguageManager.get("menu.customers")

                );



        Button purchases =

                createMenuButton(

                        LanguageManager.get("menu.purchases")

                );



        Button sales =

                createMenuButton(

                        LanguageManager.get("menu.sales")

                );



        Button employees =

                createMenuButton(

                        LanguageManager.get("menu.employees")

                );



        Button reports =

                createMenuButton(

                        LanguageManager.get("menu.reports")

                );



        Button settings =

                createMenuButton(

                        LanguageManager.get("menu.settings")

                );

        Button profile = createMenuButton(LanguageManager.get("menu.profile"));
        Button inventory = createMenuButton(LanguageManager.get("menu.inventory"));
        Button activity = createMenuButton(LanguageManager.get("menu.activity"));
        Button returns = createMenuButton(LanguageManager.get("menu.returns"));
        Button purchaseReturns = createMenuButton(LanguageManager.get("menu.purchase.returns"));



        dashboard.setOnAction(event ->

                showDashboard()

        );



        products.setOnAction(event ->

                showProducts()

        );



        categories.setOnAction(event ->

                showCategories()

        );



        suppliers.setOnAction(event ->

                showSuppliers()

        );



        customers.setOnAction(event ->

                showCustomers()

        );



        purchases.setOnAction(event ->

                showPurchases()

        );



        sales.setOnAction(event ->

                showSales()

        );



        employees.setOnAction(event ->

                showEmployees()

        );



        reports.setOnAction(event ->

                showReports()

        );



        settings.setOnAction(event ->

                showSettings()

        );

        profile.setOnAction(event -> showProfile());
        inventory.setOnAction(event -> showInventory());
        activity.setOnAction(event -> showActivity());
        returns.setOnAction(event -> showReturns());
        purchaseReturns.setOnAction(event -> showPurchaseReturns());

        // Navigation is a convenience layer; services enforce the same policy.
        products.setVisible(PermissionManager.canManageProducts());
        products.setManaged(PermissionManager.canManageProducts());
        inventory.setVisible(PermissionManager.canManageProducts());
        inventory.setManaged(PermissionManager.canManageProducts());
        categories.setVisible(PermissionManager.canManageCategories());
        categories.setManaged(PermissionManager.canManageCategories());
        suppliers.setVisible(PermissionManager.canManageSuppliers());
        suppliers.setManaged(PermissionManager.canManageSuppliers());
        customers.setVisible(PermissionManager.canManageCustomers());
        customers.setManaged(PermissionManager.canManageCustomers());
        purchases.setVisible(PermissionManager.canManagePurchases());
        purchases.setManaged(PermissionManager.canManagePurchases());
        sales.setVisible(PermissionManager.canMakeSales());
        sales.setManaged(PermissionManager.canMakeSales());
        employees.setVisible(PermissionManager.canManageEmployees());
        employees.setManaged(PermissionManager.canManageEmployees());
        reports.setVisible(PermissionManager.canViewReports());
        reports.setManaged(PermissionManager.canViewReports());
        settings.setVisible(PermissionManager.canOpenSettings());
        settings.setManaged(PermissionManager.canOpenSettings());
        activity.setVisible(PermissionManager.canViewReports());
        activity.setManaged(PermissionManager.canViewReports());
        returns.setVisible(PermissionManager.canManageReturns());
        returns.setManaged(PermissionManager.canManageReturns());
        purchaseReturns.setVisible(PermissionManager.canManagePurchaseReturns());
        purchaseReturns.setManaged(PermissionManager.canManagePurchaseReturns());



        menu.getChildren().addAll(



                dashboard,

                inventory,

                products,

                categories,

                suppliers,

                customers,

                purchases,

                sales,

                returns,

                purchaseReturns,

                employees,

                reports,

                activity,

                profile,

                settings



        );



        setLeft(menu);



    }



    private Button createMenuButton(String text) {



        Button button = new Button(text);



        button.getStyleClass().add(

                "sidebar-button"

        );



        return button;



    }



    private void showDashboard() {



        contentPane.setCenter(

                new DashboardHomeView()

        );



    }



    private void showProducts() {



        contentPane.setCenter(

                new ProductView()

        );



    }



    private void showCategories() {



        contentPane.setCenter(

                new CategoryView()

        );



    }



    private void showSuppliers() {



        contentPane.setCenter(

                new SupplierView()

        );



    }



    private void showCustomers() {



        contentPane.setCenter(

                new CustomerView()

        );



    }

    private void showPurchases() {



        contentPane.setCenter(

                new PurchaseView()

        );



    }



    private void showSales() {



        contentPane.setCenter(

                new SaleView()

        );



    }



    private void showReturns() {

        contentPane.setCenter(
                new ReturnView()
        );

    }

    private void showPurchaseReturns() {

        contentPane.setCenter(
                new PurchaseReturnView()
        );

    }

    private void showEmployees() {



        contentPane.setCenter(

                new EmployeeView()

        );



    }



    private void showReports() {



        contentPane.setCenter(

                new ReportView()

        );



    }



    private void showSettings() {



        contentPane.setCenter(

                new SettingsView()

        );



    }

    private void showProfile() {
        contentPane.setCenter(new ProfileView());
    }

    private void showInventory() {
        contentPane.setCenter(new InventoryView());
    }

    private void showActivity() {
        contentPane.setCenter(new ActivityLogView());
    }



    private void returnToLogin() {



        new LoginController().logout();



        Stage stage = (Stage) getScene().getWindow();



        Scene loginScene = new Scene(

                new LoginView(stage),

                500,

                300

        );



        loginScene.getStylesheets().add(

                getClass()

                        .getResource("/styles/application.css")

                        .toExternalForm()

        );

        com.adam.warehouse.util.ThemeManager.applyTheme(loginScene);



        stage.setScene(loginScene);



        stage.setWidth(500);



        stage.setHeight(300);



        stage.setResizable(false);



        stage.centerOnScreen();



    }



}
