package com.example.radarapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class BleConnection extends AppCompatActivity {
    private ConnectionToolbox connectionToolbox = ConnectionToolbox.getInstance();

    private Resources resources;

    // Mark that if the user chooses an item to abort the scanning process.
    private boolean isScanAborted = false;

    private TextView bluetoothStatusTextView;
    private Button scanBleButton;
    private TextView bluetoothDevicesTextView;
    private ListView bluetoothDeviceListView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_connection);

        resources = getApplicationContext().getResources();

        bluetoothStatusTextView = findViewById(R.id.bluetoothStatusTextView);
        scanBleButton = findViewById(R.id.scanBleButton);
        bluetoothDevicesTextView = findViewById(R.id.bluetoothDevicesTextView);
        bluetoothDeviceListView = findViewById(R.id.bluetoothDeviceList);

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        connectionToolbox.mBluetoothAdapter = mBluetoothManager == null ? null : mBluetoothManager.getAdapter();

        connectionToolbox.bleDeviceListViewAdapter = new BleDeviceListViewAdapter(this);

        connectionToolbox.serviceUUID = UUID.fromString(resources.getString(R.string.TARGET_DEVICE_SERVICE_UUID));
        connectionToolbox.readUUID = UUID.fromString(resources.getString(R.string.TARGET_DEVICE_READ_UUID));
        connectionToolbox.writeUUID = UUID.fromString(resources.getString(R.string.TARGET_DEVICE_WRITE_UUID));

        if (mBluetoothManager == null || connectionToolbox.mBluetoothAdapter == null) {
            Toast.makeText(this, "Your device doesn't have bluetooth module!", Toast.LENGTH_SHORT).show();
            finish();
        }

        connectionToolbox.mGattCallback.setAppBleConnection(this);

        scanBleButton.setOnClickListener(view -> {
            if (!connectionToolbox.mBluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, resources.getInteger(R.integer.BLE_ENABLE_REQUEST_CODE));
            } else {
                isScanAborted = false;
                startOrStopDiscovery(true);
            }
            runOnUiThread(() -> {
                connectionToolbox.bleDeviceListViewAdapter.clear();
                updateScanningResult();
                bluetoothStatusTextView.setText("Scanning...");
                scanBleButton.setEnabled(false);
            });
        });

        bluetoothDeviceListView.setOnItemClickListener((adapterView, view, i, l) -> {
            isScanAborted = true;
            connectionToolbox.targetDevice = (BluetoothDevice) connectionToolbox.bleDeviceListViewAdapter.getItem(i);
            connectToTargetDevice(connectionToolbox.targetDevice);
        });
    }

    private void connectToTargetDevice(BluetoothDevice device) {
        startOrStopDiscovery(false);
        connectionToolbox.mBluetoothGatt = device.connectGatt(getApplicationContext(), false, connectionToolbox.mGattCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == resources.getInteger(R.integer.BLE_ENABLE_REQUEST_CODE)) {
            if (resultCode == Activity.RESULT_OK) {
                startOrStopDiscovery(true);
            } else {
                if (!connectionToolbox.mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(this, "Needed to open bluetooth module to scan or refresh!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startOrStopDiscovery(false);
        unregisterReceiver(receiver);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED &&
                        resources.getString(R.string.TARGET_DEVICE_NAME).equals(device.getName()) &&
                        device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                    runOnUiThread(() -> {
                        connectionToolbox.bleDeviceListViewAdapter.addDevice(device);
                        updateScanningResult();
                    });
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                runOnUiThread(() -> {
                    if (isScanAborted) {
                        bluetoothStatusTextView.setText("Connecting to the target device...");
                        bluetoothDeviceListView.setEnabled(false);
                    } else {
                        bluetoothStatusTextView.setText("Scan Finished!");
                        scanBleButton.setEnabled(true);
                    }
                });
            }
        }
    };

    @SuppressLint("SetTextI18n")
    private void updateScanningResult() {
        if (connectionToolbox.bleDeviceListViewAdapter.getCount() == 1) {
            bluetoothDevicesTextView.setText("There's 1 heart radar device nearby.");
        } else if (connectionToolbox.bleDeviceListViewAdapter.getCount() == 0) {
            bluetoothDevicesTextView.setText("There's no heart radar device nearby.");
        } else {
            bluetoothDevicesTextView.setText("There's " + connectionToolbox.bleDeviceListViewAdapter.getCount() + " heart radar devices nearby.");
        }
        bluetoothDeviceListView.setAdapter(connectionToolbox.bleDeviceListViewAdapter);
    }

    // true for start and false for stop
    private void startOrStopDiscovery(boolean enable) {
        if (enable) {
            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            registerReceiver(receiver, filter);

            if (connectionToolbox.mBluetoothAdapter.isDiscovering()) {
                connectionToolbox.mBluetoothAdapter.cancelDiscovery();
            }
            connectionToolbox.mBluetoothAdapter.startDiscovery();
        } else {
            isScanAborted = true;
            connectionToolbox.mBluetoothAdapter.cancelDiscovery();
        }
    }
}
