package com.adam.warehouse.view;

import com.adam.warehouse.controller.ActivityLogController;
import com.adam.warehouse.model.ActivityLog;
import com.adam.warehouse.util.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Searchable audit history for managers and administrators. */
public class ActivityLogView extends BorderPane {
    private final ActivityLogController controller = new ActivityLogController();
    private final TableView<ActivityLog> table = new TableView<>();
    private final TextField search = new TextField();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ActivityLogView() {
        setPadding(new Insets(24));
        Label title = new Label(LanguageManager.get("activity.title"));
        title.getStyleClass().add("page-title");
        search.setPromptText(LanguageManager.get("activity.search"));
        HBox.setHgrow(search, Priority.ALWAYS);
        Button refresh = new Button(LanguageManager.get("button.refresh"));
        refresh.getStyleClass().add("primary-button");
        refresh.setOnAction(event -> load());
        setTop(new HBox(12, title, search, refresh));
        columns();
        search.textProperty().addListener((observable, oldValue, value) -> table.setItems(controller.search(value)));
        load();
    }

    private void columns() {
        table.getColumns().addAll(List.of(
                column(LanguageManager.get("activity.column.time"), log -> log.getTimestamp() == null ? "" : TIME.format(log.getTimestamp())),
                column(LanguageManager.get("activity.column.employee"), ActivityLog::getEmployeeName),
                column(LanguageManager.get("activity.column.module"), log -> LanguageManager.code("activity.module", log.getModule())),
                column(LanguageManager.get("activity.column.action"), log -> LanguageManager.code("activity.action", log.getAction())),
                column(LanguageManager.get("activity.column.description"), log -> LanguageManager.activityDescription(log.getDescription()))));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(LanguageManager.get("activity.empty")));
        setCenter(table);
    }

    private TableColumn<ActivityLog, String> column(String name, java.util.function.Function<ActivityLog, String> value) {
        TableColumn<ActivityLog, String> column = new TableColumn<>(name);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }
    private void load() { table.setItems(controller.getLogs()); }
}
