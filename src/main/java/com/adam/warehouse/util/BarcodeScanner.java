package com.adam.warehouse.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Decodes a barcode from a photo, screenshot, or camera-captured image. */
public final class BarcodeScanner {
    private BarcodeScanner() { }

    public static String decode(Path imageFile) {
        if (imageFile == null) {
            throw new IllegalArgumentException("Choose a barcode image first.");
        }
        try {
            BufferedImage image = ImageIO.read(imageFile.toFile());
            if (image == null) {
                throw new IllegalArgumentException("The selected file is not a supported image.");
            }
            BinaryBitmap bitmap = new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            return new MultiFormatReader().decode(bitmap).getText();
        } catch (NotFoundException exception) {
            throw new IllegalArgumentException("No readable barcode was found in this image.");
        } catch (IOException exception) {
            throw new RuntimeException("Unable to read the barcode image.", exception);
        }
    }
}
