package com.adam.warehouse.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;

public class BarcodeGenerator {

    private BarcodeGenerator() {
    }

    public static Image generateBarcode(String text) {

        try {

            BitMatrix matrix = new MultiFormatWriter().encode(
                    text,
                    BarcodeFormat.CODE_128,
                    300,
                    100
            );

            BufferedImage bufferedImage =
                    MatrixToImageWriter.toBufferedImage(matrix);

            return SwingFXUtils.toFXImage(bufferedImage, null);

        } catch (WriterException e) {

            throw new RuntimeException("Failed to generate barcode.", e);

        }

    }

}