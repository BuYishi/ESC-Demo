package com.tiaze.esc_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class PrintTool {
    private OutputStream outputStream;
    private final String tag = "PrintTool";
    public static final byte ALIGNMENT_LEFT = 48;
    public static final byte ALIGNMENT_CENTER = 49;
    public static final byte ALIGNMENT_RIGHT = 50;

    public PrintTool(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void reset(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    //fontSize范围：1-8
    public void printText(String text, int fontWidth, int fontHeight, byte alignment) throws IOException {
        setAlignment(alignment);
        byte fontWidthByte, fontHeightByte;
        switch (fontWidth) {
            case 1:
                fontWidthByte = 0x00;
                break;
            case 2:
                fontWidthByte = 0x10;
                break;
            case 3:
                fontWidthByte = 0x20;
                break;
            case 4:
                fontWidthByte = 0x30;
                break;
            case 5:
                fontWidthByte = 0x40;
                break;
            case 6:
                fontWidthByte = 0x50;
                break;
            case 7:
                fontWidthByte = 0x60;
                break;
            default:
                fontWidthByte = 0x70;
        }
        switch (fontHeight) {
            case 1:
                fontHeightByte = 0x00;
                break;
            case 2:
                fontHeightByte = 0x01;
                break;
            case 3:
                fontHeightByte = 0x02;
                break;
            case 4:
                fontHeightByte = 0x03;
                break;
            case 5:
                fontHeightByte = 0x04;
                break;
            case 6:
                fontHeightByte = 0x05;
                break;
            case 7:
                fontHeightByte = 0x06;
                break;
            default:
                fontHeightByte = 0x07;
        }
        byte[] bytesForFontSize = {29, 33, (byte) (fontWidthByte | fontHeightByte)};
        byte[] textBytes = text.getBytes("GBK");
        byte[] buffer = new byte[bytesForFontSize.length + textBytes.length];
        System.arraycopy(bytesForFontSize, 0, buffer, 0, bytesForFontSize.length);
        System.arraycopy(textBytes, 0, buffer, bytesForFontSize.length, textBytes.length);
        outputStream.write(buffer);
//        blueToothUtils.sendData(buffer.length, buffer);
    }

    //unitSize范围：1-16
    public void printQRCode(String data, int unitSize, byte alignment) throws IOException {  //商睿通过指令打印二维码无法居右
        if (Build.MODEL.contains("A8") || Build.MODEL.contains("JICAI"))  //A8与集财打印二维码，须转换为图片后打印
            printQRCodeBitmap(data, unitSize * 30, alignment);
        else {
            setAlignment(alignment);
            byte[] bytesForQRCodeUnitSize = {29, 40, 107, 3, 0, 49, 67, (byte) unitSize};
            byte[] bytesForStoreQRCodeData = new byte[]{29, 40, 107, 0, 0, 49, 80, 48};
            byte[] qrcodeDataBytes = data.getBytes();
            int cmdDataLen = qrcodeDataBytes.length + 3;
            bytesForStoreQRCodeData[3] = (byte) (cmdDataLen % 256);
            bytesForStoreQRCodeData[4] = (byte) (cmdDataLen / 256);
            byte[] bytesForPrintQRCode = {29, 40, 107, 3, 0, 49, 81, 48};
            byte[] buffer = new byte[bytesForQRCodeUnitSize.length + bytesForStoreQRCodeData.length + qrcodeDataBytes.length + bytesForPrintQRCode.length];
            System.arraycopy(bytesForQRCodeUnitSize, 0, buffer, 0, bytesForQRCodeUnitSize.length);
            System.arraycopy(bytesForStoreQRCodeData, 0, buffer, bytesForQRCodeUnitSize.length, bytesForStoreQRCodeData.length);
            System.arraycopy(qrcodeDataBytes, 0, buffer, bytesForQRCodeUnitSize.length + bytesForStoreQRCodeData.length, qrcodeDataBytes.length);
            System.arraycopy(bytesForPrintQRCode, 0, buffer, bytesForQRCodeUnitSize.length + bytesForStoreQRCodeData.length + qrcodeDataBytes.length, bytesForPrintQRCode.length);
//            blueToothUtils.sendData(buffer.length, buffer);
            outputStream.write(buffer);
            if (Build.MODEL.contains("P1") || Build.MODEL.contains("V1s"))
                printText("\n", 1, 1, alignment);
        }
    }

    public void printBitmap(Bitmap bitmap, byte alignment) throws IOException {
        setAlignment(alignment);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int pixelRed;
        int pixelGreen;
        int pixelBlue;
        int[] pixels = new int[width * height];
        int arrayWidth = ((width - 1) / 8) + 1;
        int arrayLength = arrayWidth * height;
        byte[] dataArray = new byte[arrayLength + 8];
        dataArray[0] = 0x1D;
        dataArray[1] = 0x76;
        dataArray[2] = 0x30;
        dataArray[3] = 0x00;
        dataArray[4] = (byte) arrayWidth;
        dataArray[5] = (byte) (arrayWidth / 256);
        dataArray[6] = (byte) height;
        dataArray[7] = (byte) (height / 256);
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int indexByte = 8;
        dataArray[indexByte] = 0;
        int indexBit = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {//每一行进行转换，转换完成后，可能最后一个字节需要将数据移到高位
                //获取当前像素值的r部分
                pixelRed = Color.red(pixels[i * width + j]);
                //获取当前像素值的g部分
                pixelGreen = Color.green(pixels[i * width + j]);
                //获取当前像素值的b部分
                pixelBlue = Color.blue(pixels[i * width + j]);
                if ((pixelRed + pixelGreen + pixelBlue) < 384) {
                    dataArray[indexByte] += 0x01;
                }
                ++indexBit;
                if (indexBit < 8) {
                    dataArray[indexByte] *= 2;//相当于左移一位
                } else {
                    indexBit = 0;
                    ++indexByte;
                    if (indexByte < arrayLength) {
                        dataArray[indexByte] = 0;
                    }
                }
            }
            if (indexBit != 0) {
                while (indexBit < 8) {
                    dataArray[indexByte] *= 2;//相当于左移一位
                    ++indexBit;
                }
                indexBit = 0;
                ++indexByte;
                if (indexByte < arrayLength) {
                    dataArray[indexByte] = 0;
                }
            }
        }
        outputStream.write(dataArray);
//        blueToothUtils.sendData(dataArray.length, dataArray);
    }

    public void sendCommand(byte[] command) throws IOException {
        outputStream.write(command);
    }

    public void printTemplate(Context context) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(context.getAssets().open("icon_alipay.bmp"));
        printBitmap(bitmap, PrintTool.ALIGNMENT_RIGHT);
        printText("天择信上科技\n", 1, 2, PrintTool.ALIGNMENT_CENTER);
        printText("天择信上科技\n", 1, 1, PrintTool.ALIGNMENT_CENTER);
        printText("\n", 1, 1, PrintTool.ALIGNMENT_LEFT);
        if (Build.MODEL.contains("JICAI"))
            printText("\n", 1, 1, PrintTool.ALIGNMENT_LEFT);
        StringBuilder contentsToPrint = new StringBuilder();
        contentsToPrint.append("支付方式：微信支付(预授权)\n");
        contentsToPrint.append("订单类型：押金结算后退款\n");
        contentsToPrint.append("交易时间：2018-09-25 11:41:00\n");
        contentsToPrint.append("订单号：tk20180925114100\n");
        contentsToPrint.append("流水号：\n00000000000000000000\n");
        contentsToPrint.append("-----------------------------\n");
        printText(contentsToPrint.toString(), 1, 1, PrintTool.ALIGNMENT_LEFT);
        printText("业务记录：\n", 1, 2, PrintTool.ALIGNMENT_LEFT);
        contentsToPrint = new StringBuilder();
        contentsToPrint.append("押金：￥0.01(09-25 11:41:00)\n");
        contentsToPrint.append("消费：￥0.01\n");
        contentsToPrint.append("结算退款：￥0.01(09-25 11:41:00)\n");
        contentsToPrint.append("退款：￥0.01(09-25 11:41:00)\n");
        contentsToPrint.append("-----------------------------\n");
        printText(contentsToPrint.toString(), 1, 1, PrintTool.ALIGNMENT_LEFT);
        printText("退款：￥0.01\n", 1, 2, PrintTool.ALIGNMENT_LEFT);
        printText("备注：打印模板测试\n", 1, 1, PrintTool.ALIGNMENT_LEFT);
        printText("\n", 1, 1, PrintTool.ALIGNMENT_LEFT);
        if (Build.MODEL.contains("JICAI"))
            printText("\n", 1, 1, PrintTool.ALIGNMENT_LEFT);
        printQRCode("布伊什\nBu Ish", 5, PrintTool.ALIGNMENT_CENTER);
        if (Build.MODEL.contains("T2"))
            printText("\n\n", 1, 1, PrintTool.ALIGNMENT_CENTER);
        else printText("\n", 1, 1, PrintTool.ALIGNMENT_LEFT);
        contentsToPrint = new StringBuilder();
        contentsToPrint.append("收银员：天择信上科技\n");
        contentsToPrint.append("打印时间：2018-09-25 11:56:00\n");
        if (Build.MODEL.contains("A8"))
            contentsToPrint.append("\n\n\n\n");
        else
            contentsToPrint.append("\n\n\n");
        printText(contentsToPrint.toString(), 1, 1, PrintTool.ALIGNMENT_LEFT);
    }

    //size范围：1-16
    private void printQRCodeBitmap(String data, int size, byte alignment) throws IOException {
        try {
            HashMap<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 0);
            BitMatrix bitMatrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints);
            int[] colors = new int[size * size];
            for (int y = 0; y < size; ++y) {
                for (int x = 0; x < size; ++x)
                    colors[size * y + x] = bitMatrix.get(x, y) ? Color.RED : Color.WHITE;
            }
            Bitmap bitmap = Bitmap.createBitmap(colors, size, size, Bitmap.Config.RGB_565);
            printBitmap(bitmap, alignment);
        } catch (WriterException ex) {
            Log.e(tag, "ex: " + ex);
        }
    }

    private void setAlignment(byte alignment) throws IOException {
        byte[] bytesForAlignment = {27, 97, alignment};
//        blueToothUtils.sendData(bytesForAlignment.length, bytesForAlignment);
        outputStream.write(bytesForAlignment);
    }
}