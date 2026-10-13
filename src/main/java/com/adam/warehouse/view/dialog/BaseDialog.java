package com.adam.warehouse.view.dialog;

import com.adam.warehouse.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class BaseDialog extends Stage {

    protected final BorderPane root;

    public BaseDialog(String title) {

        root = new BorderPane();

        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 650, 550);

        ThemeManager.applyTheme(scene);

        setTitle(title);

        setScene(scene);

        initModality(Modality.APPLICATION_MODAL);

        setResizable(false);

    }
}
