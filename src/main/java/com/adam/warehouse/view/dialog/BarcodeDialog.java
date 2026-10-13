package com.adam.warehouse.view.dialog;

import com.adam.warehouse.model.Product;
import com.adam.warehouse.util.BarcodeGenerator;
import com.adam.warehouse.util.LanguageManager;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.print.PrinterJob;
import javafx.stage.FileChooser;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class BarcodeDialog extends BaseDialog {

    private final Product product;


    public BarcodeDialog(Product product) {

        super(
                LanguageManager.get(
                        "barcode.title"
                )
        );

        this.product = product;

        createUI();

    }


    private void createUI() {


        Label productName =
                new Label(
                        product.getName()
                );


        productName.getStyleClass()
                .add("page-title");


        ImageView barcodeImage =
                new ImageView(

                        BarcodeGenerator.generateBarcode(
                                product.getBarcode()
                        )

                );


        barcodeImage.setFitWidth(320);

        barcodeImage.setPreserveRatio(true);



        Label barcodeNumber =
                new Label(
                        product.getBarcode()
                );



        Button saveButton = new Button(LanguageManager.get("barcode.save.png"));
        saveButton.setOnAction(event -> savePng());

        Button printButton = new Button(LanguageManager.get("barcode.print"));
        printButton.getStyleClass().add("primary-button");
        printButton.setOnAction(event -> printLabel());

        Button closeButton =
                new Button(
                        LanguageManager.get(
                                "button.close"
                        )
                );


        closeButton.getStyleClass()
                .add("primary-button");


        closeButton.setOnAction(
                e -> close()
        );



        VBox content = new VBox(20);


        content.setPadding(
                new Insets(20)
        );


        content.getChildren().addAll(

                productName,

                barcodeImage,

                barcodeNumber,
                new javafx.scene.layout.HBox(10, saveButton, printButton, closeButton)

        );


        root.setCenter(content);

    }

    private void savePng() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LanguageManager.get("barcode.save.png"));
        chooser.setInitialFileName(product.getBarcode() + ".png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LanguageManager.get("barcode.image.filter"), "*.png"));
        File file = chooser.showSaveDialog(this);
        if (file == null) return;
        try {
            ImageIO.write(createLabelImage(), "png", file);
        } catch (IOException exception) {
            showError(com.adam.warehouse.util.LanguageManager.message(exception.getMessage()));
        }
    }

    private BufferedImage createLabelImage() {
        BufferedImage barcode = SwingFXUtils.fromFXImage(BarcodeGenerator.generateBarcode(product.getBarcode()), null);
        BufferedImage label = new BufferedImage(Math.max(barcode.getWidth() + 24, product.getName().length() * 10 + 24), barcode.getHeight() + 82,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = label.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, label.getWidth(), label.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.drawString(product.getName(), 12, 24);
            graphics.drawImage(barcode, 12, 32, null);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            graphics.drawString(product.getBarcode(), 12, barcode.getHeight() + 54);
        } finally {
            graphics.dispose();
        }
        return label;
    }

    private void printLabel() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showError(LanguageManager.get("barcode.printer.unavailable"));
            return;
        }
        if (job.showPrintDialog(this)) {
            VBox label = new VBox(new ImageView(SwingFXUtils.toFXImage(createLabelImage(), null)));
            label.setPadding(new Insets(12));
            if (!job.printPage(label)) {
                job.cancelJob();
                showError(LanguageManager.get("barcode.print.failed"));
            } else job.endJob();
        }
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
