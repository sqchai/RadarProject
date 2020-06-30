package com.example.radarapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Message;
import android.widget.Toast;

class MyBluetoothGattCallback extends BluetoothGattCallback {
    private MainActivityDoctor app;
    private BleConnection appBleConnection;

    void setAppBleConnection(BleConnection appBleConnection) {
        this.appBleConnection = appBleConnection;
    }

    private ConnectionToolbox connectionToolbox = ConnectionToolbox.getInstance();

    MyBluetoothGattCallback(MainActivityDoctor app) {
        this.app = app;
    }

    private static final int newDataBufferSize = 50;
    private int[] newDataBuffer_ipos = new int[newDataBufferSize];
    private int[] newDataBuffer_ineg = new int[newDataBufferSize];
    private int[] newDataBuffer_qpos = new int[newDataBufferSize];
    private int[] newDataBuffer_qneg = new int[newDataBufferSize];
    private int[] newDataBuffer_ecg = new int[newDataBufferSize];
    private int newDataBufferPointer = 0;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            if (connectionToolbox.connectionMethod != ConnectionToolbox.CONNECTION_WIFI) {
                connectionToolbox.connectionMethod = ConnectionToolbox.CONNECTION_UNKNOWN;
            }
            connectionToolbox.mBluetoothGatt.close();
            app.uiUpdateHandler.sendEmptyMessage(R.integer.MSG_STOP);
            app.runOnUiThread(() -> {
                app.updateConnMethodTextView();
                app.showToast("Lose Bluetooth connection!", Toast.LENGTH_SHORT);
            });
            app.uiUpdateHandler.sendEmptyMessage(R.integer.MSG_SYSTEM_RESET);
        } else if (newState == BluetoothGatt.STATE_CONNECTED) {
            app.showToast("Successfully to connect to the target device!", Toast.LENGTH_SHORT);
            appBleConnection.finish();
            connectionToolbox.mBluetoothGatt.discoverServices();
            connectionToolbox.connectionMethod = ConnectionToolbox.CONNECTION_BLUETOOTH;
        } else {
            app.showToast("Cannot connect to the target device!", Toast.LENGTH_SHORT);
            connectionToolbox.connectionMethod = ConnectionToolbox.CONNECTION_UNKNOWN;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        BluetoothGattService service = gatt.getService(connectionToolbox.serviceUUID);
        if (service != null) {
            connectionToolbox.writeCharacteristic = service.getCharacteristic(connectionToolbox.writeUUID);
            connectionToolbox.readCharacteristic = service.getCharacteristic(connectionToolbox.readUUID);
        }
        int tryTimes = 0;
        while (++tryTimes <= 1000) {
            if (connectionToolbox.writeCharacteristic != null && connectionToolbox.readCharacteristic != null) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                app.showToast("Exception occurred: " + e.getMessage(), Toast.LENGTH_LONG);
            }
        }
        if (tryTimes > 100) {
            app.showToast("Cannot find the services!", Toast.LENGTH_LONG);
        }
        if (connectionToolbox.readCharacteristic != null) {
            connectionToolbox.setCharacteristicNotification(connectionToolbox.readCharacteristic);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        ConnectionToolbox.commandWriteFinish = true;
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // No Data
        // 12341234123412341234
        // missing 10
        String content = new String(characteristic.getValue()).trim();
        if (content.equals("WIFIFAILED")) {
            app.showToast("Wi-Fi Connection Failed!", Toast.LENGTH_LONG);
        } else if (content.startsWith("IP: ")) {
            String IP = content.substring(4);
            app.showToast("Got IP: " + IP, Toast.LENGTH_SHORT);

            Message msg_updateIP = new Message();
            Bundle bundle_updateIP = new Bundle();
            bundle_updateIP.putString("IP", IP);
            msg_updateIP.what = R.integer.MSG_UPDATE_IP;
            msg_updateIP.setData(bundle_updateIP);
            app.uiUpdateHandler.sendMessage(msg_updateIP);
        } else if (!content.equals("No Data")) {
            if (content.startsWith("missing")) {
                app.showToast(String.format("Missing %s records!", content.substring(8)), Toast.LENGTH_SHORT);
            } else {
                if (newDataBufferPointer == newDataBufferSize) {
                    newDataBufferPointer = 0;

                    // The buffer pointer and records pointer.
                    int bufPointer;

                    // Move the current records to leave room for the new records.
                    // Execute the code if needed.
                    if (AxesView.getListDatabufPointer() + newDataBufferSize > app.LIST_DATABUF_SIZE) {
                        int moveFromWhere = AxesView.getListDatabufPointer() + newDataBufferSize - app.LIST_DATABUF_SIZE;
                        for (int newPointer = 0, oldPointer = moveFromWhere; oldPointer < AxesView.getListDatabufPointer(); newPointer++, oldPointer++) {
                            app.ipos[newPointer] = app.ipos[oldPointer];
                            app.ineg[newPointer] = app.ineg[oldPointer];
                            app.qpos[newPointer] = app.qpos[oldPointer];
                            app.qneg[newPointer] = app.qneg[oldPointer];
                            app.idiff[newPointer] = app.idiff[oldPointer];
                            app.qdiff[newPointer] = app.qdiff[oldPointer];
                            app.ecg[newPointer] = app.ecg[oldPointer];
                        }
                        bufPointer = app.LIST_DATABUF_SIZE - newDataBufferSize;
                        AxesView.setListDatabufPointer(app.LIST_DATABUF_SIZE);
                    } else {
                        bufPointer = AxesView.getListDatabufPointer();
                        AxesView.setListDatabufPointer(AxesView.getListDatabufPointer() + newDataBufferSize);
                    }

                    // Records assignment.
                    for (int recordsPointer = 0; recordsPointer < newDataBufferSize; bufPointer++, recordsPointer++) {
                        app.ipos[bufPointer] = newDataBuffer_ipos[recordsPointer];
                        app.ineg[bufPointer] = newDataBuffer_ineg[recordsPointer];
                        app.qpos[bufPointer] = newDataBuffer_qpos[recordsPointer];
                        app.qneg[bufPointer] = newDataBuffer_qneg[recordsPointer];
                        app.ecg[bufPointer] = newDataBuffer_ecg[recordsPointer];
                        app.idiff[bufPointer] = app.ipos[bufPointer] - app.ineg[bufPointer];
                        app.qdiff[bufPointer] = app.qpos[bufPointer] - app.qneg[bufPointer];
                    }

                    // If save buffers full, save it to file.
                    if (app.saveDatabufPointer + newDataBufferSize > app.SAVE_DATABUF_SIZE) {
                        app.saveDataToFile();
                    }

                    for (int recordsPointer = 0; recordsPointer < newDataBufferSize; app.saveDatabufPointer++, recordsPointer++) {
                        app.saveDataIPos[app.saveDatabufPointer] = newDataBuffer_ipos[recordsPointer];
                        app.saveDataINeg[app.saveDatabufPointer] = newDataBuffer_ineg[recordsPointer];
                        app.saveDataQPos[app.saveDatabufPointer] = newDataBuffer_qpos[recordsPointer];
                        app.saveDataQNeg[app.saveDatabufPointer] = newDataBuffer_qneg[recordsPointer];
                        app.saveDataECG[app.saveDatabufPointer] = newDataBuffer_ecg[recordsPointer];
                    }
                } else {
                    newDataBuffer_ipos[newDataBufferPointer] = Integer.parseInt(content.substring(0, 4));
                    newDataBuffer_ineg[newDataBufferPointer] = Integer.parseInt(content.substring(4, 8));
                    newDataBuffer_qpos[newDataBufferPointer] = Integer.parseInt(content.substring(8, 12));
                    newDataBuffer_qneg[newDataBufferPointer] = Integer.parseInt(content.substring(12, 16));
                    newDataBuffer_ecg[newDataBufferPointer] = Integer.parseInt(content.substring(16, 20));
                    newDataBufferPointer++;
                }
            }
        }
    }
}
