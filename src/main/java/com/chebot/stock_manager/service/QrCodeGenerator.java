package com.chebot.stock_manager.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QrCodeGenerator {

    // Tamaño del QR en pixeles
    private static final int WIDTH = 250;
    private static final int HEIGHT = 250;

    /**
     * Genera un código QR para el texto dado y lo retorna como un array de bytes PNG.
     */
    public static byte[] generateQrCodeImage(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        // Genera la matriz de bits a partir del texto
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, WIDTH, HEIGHT);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();

        // Escribe la imagen PNG al stream de salida
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

        return pngOutputStream.toByteArray();
    }
}