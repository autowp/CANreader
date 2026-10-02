package com.autowp.canreader;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Created by autow on 13.02.2016.
 */
public class UsbDeviceSpinnerAdapter extends ArrayAdapter<UsbDevice> {

    public UsbDeviceSpinnerAdapter(Context context, int resource, List<UsbDevice> objects) {
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

        UsbDevice device = getItem(position);

        if (device != null) {
            TextView tvProductName = (TextView)v.findViewById(R.id.textViewProductName);
            TextView tvDeviceInto = (TextView)v.findViewById(R.id.textViewDeviceInfo);

                String productName = device.getProductName();
                tvProductName.setText(productName == null ? device.getDeviceName() : productName);
                tvDeviceInto.setText(String.format(
                    Locale.ROOT,
                    "%s %04X/%04X, %s",
                    device.getManufacturerName() == null ? "" : device.getManufacturerName(),
                    device.getVendorId(),
                    device.getProductId(),
                    device.getDeviceName()
                ));

        }

        return v;
    }
}
