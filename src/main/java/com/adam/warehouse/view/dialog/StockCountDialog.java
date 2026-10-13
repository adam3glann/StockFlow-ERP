package com.adam.warehouse.view.dialog;

import com.adam.warehouse.controller.StockCountController;
import com.adam.warehouse.model.StockCountItem;
import com.adam.warehouse.model.StockCountSession;
import com.adam.warehouse.util.AlertUtil;
import com.adam.warehouse.util.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Editable physical-count worksheet. Saving records counts and variances but does not change stock. */
public class StockCountDialog extends BaseDialog {
    private final StockCountController controller = new StockCountController();
    private final StockCountSession session;
    private final TableView<StockCountItem> table = new TableView<>();

    public StockCountDialog(StockCountSession session, List<StockCountItem> items) {
        super(String.format(LanguageManager.get("inventory.count.session.title"), session.getCountId()));
        this.session = session;
        setResizable(true);
        setWidth(1120);
        setHeight(700);
        createLayout(items);
    }

    private void createLayout(List<StockCountItem> items) {
        Label instructions = new Label(LanguageManager.get("inventory.count.instructions"));
        instructions.setWrapText(true);

        table.setEditable(true);
        table.getColumns().addAll(
                column(LanguageManager.get("product"), item -> item.getProductName()),
                column(LanguageManager.get("barcode"), item -> item.getBarcode()),
                column(LanguageManager.get("inventory.count.recorded"), item -> String.valueOf(item.getRecordedQuantity())),
                quantityColumn(),
                column(LanguageManager.get("inventory.count.variance"), item -> item.getCountedQuantity() == null ? "—" : String.valueOf(item.getCountedQuantity() - item.getRecordedQuantity())),
                reasonColumn());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getItems().setAll(items);
        table.setPlaceholder(new Label(LanguageManager.get("inventory.count.empty")));

        Button cancel = new Button(LanguageManager.get("cancel"));
        cancel.setOnAction(event -> close());
        Button save = new Button(LanguageManager.get("inventory.count.complete"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> completeCount());
        HBox actions = new HBox(10, cancel, save);
        actions.setPadding(new Insets(12, 0, 0, 0));
        VBox content = new VBox(12, instructions, table, actions);
        content.setPadding(new Insets(18));
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(content);
    }

    private TableColumn<StockCountItem, String> column(String title, java.util.function.Function<StockCountItem, String> value) {
        TableColumn<StockCountItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private TableColumn<StockCountItem, String> quantityColumn() {
        TableColumn<StockCountItem, String> column = column(LanguageManager.get("inventory.count.actual"),
                item -> item.getCountedQuantity() == null ? "" : String.valueOf(item.getCountedQuantity()));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            String value = event.getNewValue() == null ? "" : event.getNewValue().trim();
            try {
                event.getRowValue().setCountedQuantity(value.isEmpty() ? null : Integer.valueOf(value));
                if (event.getRowValue().getCountedQuantity() != null && event.getRowValue().getCountedQuantity() < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                event.getRowValue().setCountedQuantity(null);
                showError(LanguageManager.get("inventory.count.quantity.invalid"));
            }
            table.refresh();
        });
        return column;
    }

    private TableColumn<StockCountItem, String> reasonColumn() {
        TableColumn<StockCountItem, String> column = column(LanguageManager.get("inventory.count.reason"),
                item -> item.getReason() == null ? "" : item.getReason());
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> event.getRowValue().setReason(event.getNewValue()));
        return column;
    }

    private void completeCount() {
        try {
            controller.completeCount(session.getCountId(), table.getItems());
            Alert alert = AlertUtil.create(Alert.AlertType.INFORMATION);
            alert.setHeaderText(LanguageManager.get("inventory.count.saved.title"));
            long variances = table.getItems().stream()
                    .filter(item -> item.getCountedQuantity() != null && item.getCountedQuantity() != item.getRecordedQuantity())
                    .count();
            alert.setContentText(String.format(LanguageManager.get("inventory.count.saved.message"), variances));
            alert.showAndWait();
            close();
        } catch (RuntimeException exception) {
            showError(LanguageManager.message(exception.getMessage()));
        }
    }

    private void showError(String message) {
        Alert alert = AlertUtil.create(Alert.AlertType.ERROR);
        alert.setHeaderText(LanguageManager.get("inventory.count.title"));
        alert.setContentText(message);
        alert.showAndWait();
    }
}
