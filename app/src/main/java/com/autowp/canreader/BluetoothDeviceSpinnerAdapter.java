package com.autowp.canreader;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

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
            v = vi.inflate(R.layout.usbdevice_spinner_item, null);
        }

        BluetoothDevice device = getItem(position);

        if (device != null) {
            TextView tvProductName = (TextView)v.findViewById(R.id.textViewProductName);
            TextView tvDeviceInto = (TextView)v.findViewById(R.id.textViewDeviceInfo);



            tvProductName.setText(device.getName());
            String deviceInfo = String.format(
                    "%s / %s",
                    device.getAddress(),
                    device.getBluetoothClass().toString()
            );
            tvDeviceInto.setText(deviceInfo);

        }

        return v;
    }
}
