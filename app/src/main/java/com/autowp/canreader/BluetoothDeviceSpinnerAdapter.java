package com.autowp.canreader;

import android.Manifest;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Created by autowp on 13.02.2016.
 */
public class BluetoothDeviceSpinnerAdapter extends ArrayAdapter<BluetoothDevice> {

    public BluetoothDeviceSpinnerAdapter(Context context, int resource, List<BluetoothDevice> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.usbdevice_spinner_item, parent, false);
        }

        BluetoothDevice device = getItem(position);

        if (device != null) {
            TextView tvProductName = (TextView)v.findViewById(R.id.textViewProductName);
            TextView tvDeviceInto = (TextView)v.findViewById(R.id.textViewDeviceInfo);



                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && getContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                tvProductName.setText(R.string.bluetooth_permission_required);
                tvDeviceInto.setText("");
                return v;
                }

                String deviceName = device.getName();
                tvProductName.setText(deviceName == null ? device.getAddress() : deviceName);
                BluetoothClass bluetoothClass = device.getBluetoothClass();
            String deviceInfo = String.format(Locale.ROOT,
                    "%s / %s",
                    device.getAddress(),
                    bluetoothClass == null ? "" : bluetoothClass.toString()
            );
            tvDeviceInto.setText(deviceInfo);

        }

        return v;
    }
}
