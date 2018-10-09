package com.tiaze.esc_demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class BluetoothPrinterConnector {
    private OutputStream outputStream;
    public static final String TAG = BluetoothPrinterConnector.class.getName();

    public BluetoothPrinterConnector() throws IOException {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.enable())
                throw new IOException("Cannot enable bluetooth");
        }
        Set<BluetoothDevice> bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
        Log.d(TAG, "bluetoothDeviceSet.size(): " + bluetoothDeviceSet.size());
        if (bluetoothDeviceSet.size() == 0)
            throw new IOException("No bonded devices");
        Iterator<BluetoothDevice> bluetoothDeviceIterator = bluetoothDeviceSet.iterator();
        while (bluetoothDeviceIterator.hasNext()) {
            BluetoothDevice bluetoothDevice = bluetoothDeviceIterator.next();
            if (bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.MISC) {
                BluetoothSocket bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.randomUUID());
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                break;
            }
        }
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}