package com.adam.warehouse.app;

import com.adam.warehouse.util.SettingsManager;
import com.adam.warehouse.util.ThemeManager;
import com.adam.warehouse.util.LanguageManager;
import com.adam.warehouse.config.DatabaseInitializer;
import com.adam.warehouse.dao.EmployeeDAO;
import com.adam.warehouse.view.LoginView;
import com.adam.warehouse.view.InitialAdminSetupView;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WarehouseApplication extends Application {

    @Override
    public void start(Stage primaryStage) {

        SettingsManager.load();

        try {
            DatabaseInitializer.initialize();
        } catch (Exception exception) {
            Alert alert = com.adam.warehouse.util.AlertUtil.create(Alert.AlertType.ERROR);
            alert.setTitle(LanguageManager.get("database.unavailable.title"));
            alert.setHeaderText(LanguageManager.get("database.unavailable.header"));
            alert.setContentText(databaseSetupMessage(exception));
            alert.showAndWait();
            primaryStage.close();
            return;
        }

        Scene scene;
        if (new EmployeeDAO().hasActiveAdministrator()) {
            scene = new Scene(new LoginView(primaryStage), 500, 300);
        } else {
            scene = new Scene(new InitialAdminSetupView(primaryStage), 560, 430);
        }

        ThemeManager.applyTheme(scene);

        primaryStage.setTitle(LanguageManager.get("app.title"));

        primaryStage.setScene(scene);

        primaryStage.setWidth(500);
        primaryStage.setHeight(300);

        primaryStage.setResizable(false);

        primaryStage.show();
    }

    private String databaseSetupMessage(Exception exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sqlException
                    && "28P01".equals(sqlException.getSQLState())) {
                return LanguageManager.get("database.password.error");
            }
        }
        return LanguageManager.get("database.setup.error");
    }

}
