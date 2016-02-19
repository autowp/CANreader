package com.autowp.canreader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.autowp.can.adapter.android.CanHackerFelhrUsb;
import com.autowp.can.adapter.android.CanHackerUsbException;

import java.util.ArrayList;
import java.util.List;

public class ConnectionActivity extends Activity {

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private static final String PREFENCES_BAUDRATE = "baudrate";

    private UsbBroadcastReceiver mUsbReceiver;
    private UsbDeviceSpinnerAdapter mSpinnerAdapter;
    private ArrayList<UsbDevice> mUsbDeviceList = new ArrayList<>();
    private Spinner mSpinner;
    private UsbManager mUsbManager;
    private Spinner mSpinnerBaudrate;

    private class Baudrate {
        private int value;
        private String name;

        public Baudrate(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName()
        {
            return name;
        }

        public String toString()
        {
            return name;
        }
    }

    private final class UsbBroadcastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            connectDevice(device);
                            refreshButtonsState();
                        }
                    }

                }
            }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    //UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    fillUsbDeviceList();
                    refreshButtonsState();
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    //UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    fillUsbDeviceList();
                    refreshButtonsState();
                }
            }
        }
    }

    private CanReaderService canReaderService;

    ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            canReaderService = ((CanReaderService.TransferServiceBinder) binder).getService();
            bound = true;
        }

        public void onServiceDisconnected(ComponentName name) {

            bound = false;
        }
    };
    boolean bound = false;

    private void connectDevice(UsbDevice device) {
        try {
            CanHackerFelhrUsb canClient = new CanHackerFelhrUsb(mUsbManager, device);
            Baudrate baudrate = (Baudrate)mSpinnerBaudrate.getSelectedItem();
            canReaderService.setSpeed(baudrate.getValue());
            canReaderService.setCanAdapter(canClient);
            refreshButtonsState();
        } catch (CanHackerUsbException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mSpinner = (Spinner) findViewById(R.id.spinner);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                refreshButtonsState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                refreshButtonsState();
            }
        });

        mSpinnerAdapter = new UsbDeviceSpinnerAdapter(this, R.layout.usbdevice_spinner_item, mUsbDeviceList);
        mSpinnerAdapter.setDropDownViewResource(R.layout.usbdevice_spinner_item);

        mSpinnerBaudrate = (Spinner) findViewById(R.id.spinnerBaudrate);

        List<Baudrate> baudrates = new ArrayList<>();
        baudrates.add(new Baudrate(10, "10 Kbit/s"));
        baudrates.add(new Baudrate(20, "20 Kbit/s"));
        baudrates.add(new Baudrate(50, "50 Kbit/s"));
        baudrates.add(new Baudrate(100, "100 Kbit/s"));
        baudrates.add(new Baudrate(125, "125 Kbit/s"));
        baudrates.add(new Baudrate(250, "250 Kbit/s"));
        baudrates.add(new Baudrate(500, "500 Kbit/s"));
        baudrates.add(new Baudrate(1000, "1 Mbit/s"));

        ArrayAdapter<Baudrate> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, baudrates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBaudrate.setAdapter(adapter);

        final SharedPreferences mPrefences = getPreferences(MODE_PRIVATE);
        int position = mPrefences.getInt(PREFENCES_BAUDRATE, 0);
        System.out.println("position");
        System.out.println(position);
        mSpinnerBaudrate.setSelection(position);


        mSpinnerBaudrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SharedPreferences.Editor ed = mPrefences.edit();
                ed.putInt(PREFENCES_BAUDRATE, position);
                ed.apply();
                System.out.println("position save");
                System.out.println(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                refreshButtonsState();
            }
        });



        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mUsbReceiver = new UsbBroadcastReceiver();
        registerReceiver(mUsbReceiver, filter);

        Button buttonDisconnect = (Button) findViewById(R.id.buttonDisconnect);
        buttonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canReaderService.setCanAdapter(null);
                refreshButtonsState();
            }
        });

        Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                UsbDevice device = (UsbDevice) mSpinner.getSelectedItem();

                if (!mUsbManager.hasPermission(device)) {
                    Intent intent = new Intent(ACTION_USB_PERMISSION);
                    PendingIntent pendindIntent = PendingIntent.getBroadcast(ConnectionActivity.this, 0, intent, 0);
                    mUsbManager.requestPermission(device, pendindIntent);
                } else {
                    connectDevice(device);
                }


            }
        });
    }

    private void refreshButtonsState() {
        Button btnConnect = (Button) findViewById(R.id.buttonConnect);
        Button btnDisconnect = (Button) findViewById(R.id.buttonDisconnect);

        UsbDevice selectedDevice = (UsbDevice) mSpinner.getSelectedItem();

        boolean connectEnabled = bound && !canReaderService.isConnected() && selectedDevice != null;
        boolean disconnectEnabled = bound && canReaderService.isConnected();

        btnConnect.setEnabled(connectEnabled);
        btnDisconnect.setEnabled(disconnectEnabled);
    }

    private void fillUsbDeviceList() {
        mUsbDeviceList.clear();
        for (final UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
            mUsbDeviceList.add(usbDevice);
        }

        mSpinnerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, CanReaderService.class);
        bindService(intent, serviceConnection, AppCompatActivity.BIND_AUTO_CREATE);

        fillUsbDeviceList();

        mSpinner.setAdapter(mSpinnerAdapter);

        refreshButtonsState();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    @Override
    protected void onStop() {
        if (mUsbReceiver != null) {
            unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
        }
        super.onStop();
    }
}
