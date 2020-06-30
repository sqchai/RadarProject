package com.example.radarapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import java.util.List;
import java.util.UUID;

final class ConnectionToolbox {
    private static ConnectionToolbox instance = new ConnectionToolbox();

    private ConnectionToolbox() {
    }

    static ConnectionToolbox getInstance() {
        return instance;
    }

    static final int CONNECTION_UNKNOWN = 0;
    static final int CONNECTION_BLUETOOTH = 1;
    static final int CONNECTION_WIFI = 2;

    int connectionMethod = CONNECTION_UNKNOWN;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothGatt mBluetoothGatt;
    BleDeviceListViewAdapter bleDeviceListViewAdapter;
    BluetoothGattCharacteristic readCharacteristic, writeCharacteristic;

    BluetoothDevice targetDevice;

    UUID readUUID, writeUUID, serviceUUID;

    MyBluetoothGattCallback mGattCallback;

    static boolean commandWriteFinish = false;

    void sendMessage(String sendValue) {
        if (writeCharacteristic != null) {
            Log.i(LOGTAG.LOGTAG, "MESSAGE: " + sendValue);
//            while (!commandWriteFinish) {
//            }
            writeCharacteristic.setValue(sendValue);
            boolean result = mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            Log.i(LOGTAG.LOGTAG, "result: " + result);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(LOGTAG.LOGTAG, "BREAK! writeCharacteristic = null!");
        }
    }

    void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(LOGTAG.LOGTAG, "BluetoothAdapter not initialized");
            return;
        }
        boolean isEnableNotification = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        if (isEnableNotification) {
            List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
            if (descriptorList != null && descriptorList.size() > 0) {
                for (BluetoothGattDescriptor descriptor : descriptorList) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }
    }
}
