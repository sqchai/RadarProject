package com.example.radarapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BleDeviceListViewAdapter extends BaseAdapter {
    private class ViewHolder {
        TextView deviceAddress;
        TextView deviceState;
    }

    private List<BluetoothDevice> bluetoothDevices;
    private LayoutInflater layoutInflater;

    BleDeviceListViewAdapter(Context context) {
        this.bluetoothDevices = new ArrayList<>();
        this.layoutInflater = LayoutInflater.from(context);
    }

    void addDevice(BluetoothDevice device) {
        if (!bluetoothDevices.contains(device)) {
            bluetoothDevices.add(device);
        }
    }

    void clear() {
        bluetoothDevices.clear();
    }

    @Override
    public int getCount() {
        return bluetoothDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return bluetoothDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint({"InflateParams", "SetTextI18n"})
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;

        if (view == null) {
            viewHolder = new ViewHolder();
            view = layoutInflater.inflate(R.layout.list_device_item, null);
            viewHolder.deviceAddress = view.findViewById(R.id.deviceAddress);
            viewHolder.deviceState = view.findViewById(R.id.deviceState);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice blueDevice = bluetoothDevices.get(position);

        String deviceAddress = blueDevice.getAddress();
        viewHolder.deviceAddress.setText("Address: " + deviceAddress);

        int deviceState = blueDevice.getBondState();
        if (deviceState == BluetoothDevice.BOND_NONE) {
            viewHolder.deviceState.setText("Status: Not Bounded");
        } else if (deviceState == BluetoothDevice.BOND_BONDING) {
            viewHolder.deviceState.setText("Status: Bounding");
        } else if (deviceState == BluetoothDevice.BOND_BONDED) {
            viewHolder.deviceState.setText("Status: Bounded");
        }
        return view;
    }
}