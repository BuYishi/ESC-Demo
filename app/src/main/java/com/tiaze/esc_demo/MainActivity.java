package com.tiaze.esc_demo;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private Toast toast;
    private PrintTool printTool;
    private final String tag = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(tag, "Build.MODEL: " + Build.MODEL);
        toast = Toast.makeText(this, null, Toast.LENGTH_SHORT);
        findViewById(R.id.enableBluetoothButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter.isEnabled()) {
                    toast.setText("蓝牙已启用");
                    toast.show();
                } else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                    startActivityForResult(intent, 0);
                    startActivity(intent);
                }
            }
        });
        findViewById(R.id.printTextButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preprocessPrint()) {
                    try {
                        printTool.printText("天择信上科技\n", 2, 2, PrintTool.ALIGNMENT_CENTER);
                        printTool.printText("天择信上科技\n", 1, 1, PrintTool.ALIGNMENT_CENTER);
                        StringBuilder contentsToPrint = new StringBuilder();
                        contentsToPrint.append("支付方式：微信支付(预授权)\n");
                        contentsToPrint.append("订单类型：押金结算后退款\n");
                        contentsToPrint.append("交易时间：2018-09-25 11:41:00\n");
                        contentsToPrint.append("订单号：tk20180925114100\n");
                        contentsToPrint.append("流水号：\n00000000000000000000\n");
                        contentsToPrint.append("-----------------------------\n");
                        printTool.printText(contentsToPrint.toString(), 1, 1, PrintTool.ALIGNMENT_LEFT);
                        printTool.printText("业务记录：\n", 1, 2, PrintTool.ALIGNMENT_LEFT);
                        contentsToPrint = new StringBuilder();
                        contentsToPrint.append("押金：￥0.01(09-25 11:41:00)\n");
                        contentsToPrint.append("消费：￥0.01\n");
                        contentsToPrint.append("结算退款：￥0.01(09-25 11:41:00)\n");
                        contentsToPrint.append("退款：￥0.01(09-25 11:41:00)\n");
                        contentsToPrint.append("-----------------------------\n");
                        printTool.printText(contentsToPrint.toString(), 1, 1, PrintTool.ALIGNMENT_LEFT);
                        printTool.printText("退款：￥0.01\n", 1, 2, PrintTool.ALIGNMENT_LEFT);
                        printTool.printText("备注：打印模板测试\n", 1, 1, PrintTool.ALIGNMENT_LEFT);
                    } catch (IOException ex) {
                        Log.e(tag, "ex: " + ex);
                        toast.setText("蓝牙连接异常，请检查");
                        toast.show();
                    }
                }
            }
        });
        findViewById(R.id.printBarcodeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preprocessPrint()) {
                    String barcodeData = "1234567890";
                    byte[] arrayOfByte = new byte[13 + barcodeData.length()];
                    // 设置条码高度
                    arrayOfByte[0] = 0x1D;
                    arrayOfByte[1] = 'h';
                    arrayOfByte[2] = 0x60; // 1到255

                    // 设置条码宽度
                    arrayOfByte[3] = 0x1D;
                    arrayOfByte[4] = 'w';
                    arrayOfByte[5] = 2; // 2到6

                    // 设置条码文字打印位置
                    arrayOfByte[6] = 0x1D;
                    arrayOfByte[7] = 'H';
                    arrayOfByte[8] = 2; // 0到3

                    // 打印39条码
                    arrayOfByte[9] = 0x1D;
                    arrayOfByte[10] = 'k';
                    arrayOfByte[11] = 0x45;
                    arrayOfByte[12] = ((byte) barcodeData.length());
                    System.arraycopy(barcodeData.getBytes(), 0, arrayOfByte, 13, barcodeData.getBytes().length);
                    try {
                        printTool.sendCommand(arrayOfByte);
                        printTool.sendCommand("\n".getBytes());
                    } catch (IOException ex) {
                        Log.e(tag, "ex: " + ex);
                        toast.setText("蓝牙连接异常，请检查");
                        toast.show();
                    }
                }
            }
        });
        findViewById(R.id.printQRCodeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preprocessPrint()) {
                    try {
//                        printTool.printText("\n\n\n\n", 1, 1, PrintTool.ALIGNMENT_LEFT);
                        printTool.printQRCode("王垠\nyinwang.org", 5, PrintTool.ALIGNMENT_CENTER);
                        printTool.printText("\n\n\n\n", 1, 1, PrintTool.ALIGNMENT_RIGHT);
                    } catch (IOException ex) {
                        Log.e(tag, "ex: " + ex);
                        toast.setText("蓝牙连接异常，请检查");
                        toast.show();
                    }
                }
            }
        });
        findViewById(R.id.printBitmapButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (preprocessPrint()) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getAssets().open("ic_launcher.png"));
                        printTool.printBitmap(bitmap, PrintTool.ALIGNMENT_LEFT);
                        printTool.printText("\n\n\n\n", 1, 1, PrintTool.ALIGNMENT_CENTER);
                    } catch (IOException ex) {
                        Log.e(tag, "ex: " + ex);
                        if (ex.getMessage().equals("Broken pipe")) {
                            try {
                                OutputStream outputStream = new BluetoothPrinterConnector().getOutputStream();
                                printTool.reset(outputStream);
                            } catch (IOException innerEx) {
                                Log.e(tag, "innerEx: " + innerEx);
                            }
                        } else {
                            toast.setText("蓝牙连接异常，请检查");
                            toast.show();
                        }
                    }
                }
            }
        });
        findViewById(R.id.printTemplateButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preprocessPrint()) {
                    try {
                        printTool.printTemplate(MainActivity.this);
                    } catch (IOException ex) {
                        Log.e(tag, "ex: " + ex);
                        toast.setText("蓝牙连接异常，请检查");
                        toast.show();
                    }
                }
            }
        });
//        try {
//            BluetoothPrinterConnector bluetoothPrinterConnector = new BluetoothPrinterConnector();
//            printTool = new PrintTool(bluetoothPrinterConnector.getOutputStream());
//        } catch (IOException ex) {
//            Toast.makeText(this, "未能连接蓝牙打印机，请检查设置", Toast.LENGTH_SHORT).show();
//            Log.e(tag, "ex: " + ex);
//        }
        preprocessPrint();
    }

    private boolean preprocessPrint() {
        if (printTool == null) {
            try {
                BluetoothPrinterConnector bluetoothPrinterConnector = new BluetoothPrinterConnector();
                printTool = new PrintTool(bluetoothPrinterConnector.getOutputStream());
            } catch (IOException ex) {
                Toast.makeText(this, "未能连接蓝牙打印机，请检查设置", Toast.LENGTH_SHORT).show();
                Log.e(tag, "ex: " + ex);
                return false;
            }
        }
        return true;
    }
}