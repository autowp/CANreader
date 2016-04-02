package com.autowp.canreader;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanAdapterException;
import com.autowp.can.CanBusSpecs;
import com.autowp.can.adapter.android.CanHackerFelhr;
import com.autowp.can.adapter.loopback.Loopback;
import com.autowp.can.adapter.udp.CanHackerUdp;

import java.util.ArrayList;
import java.util.List;

public class ConnectionActivity extends ServiceConnectedActivity implements CanReaderService.OnConnectionStateChangedListener {

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private static final String PREFENCES_BAUDRATE = "baudrate";
    private static final String PREFENCES_VID = "vid";
    private static final String PREFENCES_PID = "pid";
    private static final String PREFENCES_UART_BAUDRATE = "uart_baudrate";
    private static final String PREFENCES_CONNECTION = "connection";
    private static final String PREFENCES_HOSTNAME = "hostname";

    private UsbBroadcastReceiver mUsbReceiver;
    private UsbDeviceSpinnerAdapter mSpinnerAdapter;
    private ArrayList<UsbDevice> mUsbDeviceList = new ArrayList<>();
    private Spinner mSpinnerDevice;
    private UsbManager mUsbManager;
    private Spinner mSpinnerCanBaudrate;
    private Spinner mSpinnerUartBaudrate;

    private enum ConnectionType {
        USB, UDP, LOOPBACK;

        public static ConnectionType fromInteger(int x) {
            switch(x) {
                case 0:
                    return USB;
                case 1:
                    return UDP;
                case 2:
                    return LOOPBACK;
            }
            return null;
        }
    }

    private ConnectionType mConnection;

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

    private class UartBaudrate {
        private int value;
        private String name;

        public UartBaudrate(int value, String name) {
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
                            connectUsbDevice(device);
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

    private void connectLoopback()
    {
        CanBusSpecs canBusSpecs = new CanBusSpecs();
        Loopback loopback = new Loopback(canBusSpecs);
        canReaderService.setCanAdapter(loopback);
    }

    private void connectUdp()
    {
        CanBusSpecs canBusSpecs = new CanBusSpecs();
        Baudrate baudrate = (Baudrate) mSpinnerCanBaudrate.getSelectedItem();
        canBusSpecs.setSpeed(baudrate.getValue());
        CanHackerUdp udp = new CanHackerUdp(canBusSpecs);
        EditText etHost = (EditText)findViewById(R.id.etUdpHost);
        String hostname = etHost.getText().toString();
        udp.setHostname(hostname);
        canReaderService.setCanAdapter(udp);

        final SharedPreferences mPrefences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = mPrefences.edit();
        ed.putString(PREFENCES_HOSTNAME, hostname);
        ed.apply();
    }

    private void connectUsbDevice(UsbDevice device) {
        try {
            UartBaudrate uartBaudrate = (UartBaudrate)mSpinnerUartBaudrate.getSelectedItem();

            CanBusSpecs canBusSpecs = new CanBusSpecs();
            Baudrate baudrate = (Baudrate) mSpinnerCanBaudrate.getSelectedItem();
            canBusSpecs.setSpeed(baudrate.getValue());
            CanHackerFelhr adapter = new CanHackerFelhr(canBusSpecs, mUsbManager, device, uartBaudrate.getValue());

            canReaderService.setCanAdapter(adapter);
            refreshButtonsState();
        } catch (CanAdapterException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        final SharedPreferences mPrefences = getPreferences(MODE_PRIVATE);

        mConnection = ConnectionType.fromInteger(mPrefences.getInt(PREFENCES_CONNECTION, 0));
        if (mConnection == null) {
            mConnection = ConnectionType.USB;
        }
        switch (mConnection) {
            case USB:
                ((RadioButton)findViewById(R.id.btnUsbConnection)).setChecked(true);
                break;
            case UDP:
                ((RadioButton)findViewById(R.id.btnUdpConnection)).setChecked(true);
                break;
            case LOOPBACK:
                ((RadioButton)findViewById(R.id.btnLoopbackConnection)).setChecked(true);
                break;
        }

        mSpinnerDevice = (Spinner) findViewById(R.id.spinnerUsbDevice);

        mSpinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                refreshButtonsState();
                SharedPreferences.Editor ed = mPrefences.edit();
                UsbDevice device = (UsbDevice) mSpinnerDevice.getSelectedItem();
                ed.putInt(PREFENCES_VID, device.getVendorId());
                ed.putInt(PREFENCES_PID, device.getProductId());
                ed.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                refreshButtonsState();
            }
        });

        mSpinnerAdapter = new UsbDeviceSpinnerAdapter(this, R.layout.usbdevice_spinner_item, mUsbDeviceList);
        mSpinnerAdapter.setDropDownViewResource(R.layout.usbdevice_spinner_item);

        mSpinnerCanBaudrate = (Spinner) findViewById(R.id.spinnerBaudrate);

        List<Baudrate> baudrates = new ArrayList<>();
        baudrates.add(new Baudrate(10, "10 Kbit/s"));
        baudrates.add(new Baudrate(20, "20 Kbit/s"));
        baudrates.add(new Baudrate(50, "50 Kbit/s"));
        baudrates.add(new Baudrate(100, "100 Kbit/s"));
        baudrates.add(new Baudrate(125, "125 Kbit/s"));
        baudrates.add(new Baudrate(250, "250 Kbit/s"));
        baudrates.add(new Baudrate(500, "500 Kbit/s"));
        baudrates.add(new Baudrate(1000, "1 Mbit/s"));

        ArrayAdapter<Baudrate> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, baudrates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCanBaudrate.setAdapter(adapter);

        mSpinnerCanBaudrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SharedPreferences.Editor ed = mPrefences.edit();
                ed.putInt(PREFENCES_BAUDRATE, position);
                ed.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                refreshButtonsState();
            }
        });


        mSpinnerUartBaudrate = (Spinner) findViewById(R.id.spinnerUartBaudrate);

        List<UartBaudrate> uartBaudrates = new ArrayList<>();
        uartBaudrates.add(new UartBaudrate(2400, "2400 bit/s"));
        uartBaudrates.add(new UartBaudrate(4800, "4800 bit/s"));
        uartBaudrates.add(new UartBaudrate(9600, "9600 bit/s"));
        uartBaudrates.add(new UartBaudrate(19200, "19200 bit/s"));
        uartBaudrates.add(new UartBaudrate(38400, "38400 bit/s"));
        uartBaudrates.add(new UartBaudrate(57600, "57600 bit/s"));
        uartBaudrates.add(new UartBaudrate(115200, "115200 bit/s"));
        uartBaudrates.add(new UartBaudrate(230400, "230400 bit/s"));
        uartBaudrates.add(new UartBaudrate(460800, "460800 bit/s"));
        uartBaudrates.add(new UartBaudrate(921600, "921600 bit/s"));

        ArrayAdapter<UartBaudrate> uartAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, uartBaudrates);
        uartAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerUartBaudrate.setAdapter(uartAdapter);

        mSpinnerUartBaudrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SharedPreferences.Editor ed = mPrefences.edit();
                ed.putInt(PREFENCES_UART_BAUDRATE, position);
                ed.apply();
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

                switch (mConnection) {
                    case USB:
                        UsbDevice device = (UsbDevice) mSpinnerDevice.getSelectedItem();

                        if (!mUsbManager.hasPermission(device)) {
                            Intent intent = new Intent(ACTION_USB_PERMISSION);
                            PendingIntent pendindIntent = PendingIntent.getBroadcast(ConnectionActivity.this, 0, intent, 0);
                            mUsbManager.requestPermission(device, pendindIntent);
                        } else {
                            connectUsbDevice(device);
                        }
                        break;
                    case UDP:
                        connectUdp();
                        break;
                    case LOOPBACK:
                        connectLoopback();
                        break;
                }
            }
        });

        EditText etHost = (EditText)findViewById(R.id.etUdpHost);
        etHost.setText(mPrefences.getString(PREFENCES_HOSTNAME, ""));
    }

    private void refreshButtonsState() {
        System.out.println("refreshButtonsState");
        final Button btnConnect = (Button) findViewById(R.id.buttonConnect);
        final Button btnDisconnect = (Button) findViewById(R.id.buttonDisconnect);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.connection_progressbar);

        boolean deviceAvailable = true;
        switch (mConnection) {
            case USB:
                UsbDevice selectedDevice = (UsbDevice) mSpinnerDevice.getSelectedItem();
                deviceAvailable = selectedDevice != null;
                break;
            case UDP:
            case LOOPBACK:
                deviceAvailable = true;
        }


        CanAdapter.ConnectionState connection = CanAdapter.ConnectionState.DISCONNECTED;
        if (canReaderService != null) {
            connection = canReaderService.getConnectionState();
        }

        System.out.println(connection);

        final boolean connectEnabled = bound && (connection == CanAdapter.ConnectionState.DISCONNECTED) && deviceAvailable;
        final boolean disconnectEnabled = bound && (connection == CanAdapter.ConnectionState.CONNECTED);
        final boolean progressBarVisible = bound && (connection == CanAdapter.ConnectionState.CONNECTING || connection == CanAdapter.ConnectionState.DISCONNECTING);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConnect.setEnabled(connectEnabled);
                btnDisconnect.setEnabled(disconnectEnabled);
                progressBar.setVisibility(progressBarVisible ? View.VISIBLE : View.GONE);
            }
        });
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

        mSpinnerDevice.setAdapter(mSpinnerAdapter);

        final SharedPreferences mPrefences = getPreferences(MODE_PRIVATE);

        int lastVid = mPrefences.getInt(PREFENCES_VID, 0);
        int lastPid = mPrefences.getInt(PREFENCES_PID, 0);

        for (UsbDevice device : mUsbDeviceList) {
            boolean match = device.getVendorId() == lastVid && device.getProductId() == lastPid;
            if (match) {
                int position = mSpinnerAdapter.getPosition(device);
                mSpinnerDevice.setSelection(position);
                break;
            }
        }

        int position = mPrefences.getInt(PREFENCES_BAUDRATE, 0);
        mSpinnerCanBaudrate.setSelection(position);

        int uartPosition = mPrefences.getInt(PREFENCES_UART_BAUDRATE, 0);
        mSpinnerUartBaudrate.setSelection(uartPosition);

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
    protected void afterConnect() {
        canReaderService.addListener(this);

        refreshButtonsState();
    }

    @Override
    protected void beforeDisconnect() {
        canReaderService.removeListener(this);
        refreshButtonsState();
    }

    @Override
    public void handleConnectedStateChanged(CanAdapter.ConnectionState connection) {
        System.out.println("connectionactivity.handleConnectionStateChanged");
        refreshButtonsState();
    }

    @Override
    protected void onStop() {
        if (mUsbReceiver != null) {
            unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
        }
        super.onStop();
    }

    protected void refreshOptions()
    {
        boolean isUsb = mConnection == ConnectionType.USB;
        boolean isUdp = mConnection == ConnectionType.UDP;

        findViewById(R.id.spinnerBaudrate).setVisibility(isUsb || isUdp ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvBaudrate).setVisibility(isUsb || isUdp ? View.VISIBLE : View.GONE);

        findViewById(R.id.spinnerUsbDevice).setVisibility(isUsb ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvUsbDevice).setVisibility(isUsb ? View.VISIBLE : View.GONE);
        findViewById(R.id.spinnerUartBaudrate).setVisibility(isUsb ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvUartBaudrate).setVisibility(isUsb ? View.VISIBLE : View.GONE);

        findViewById(R.id.tvUdpHost).setVisibility(isUdp ? View.VISIBLE : View.GONE);
        findViewById(R.id.etUdpHost).setVisibility(isUdp ? View.VISIBLE : View.GONE);
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        if (checked) {
            switch (view.getId()) {
                case R.id.btnUsbConnection:
                    mConnection = ConnectionType.USB;
                    refreshOptions();
                    refreshButtonsState();
                    break;

                case R.id.btnUdpConnection:
                    mConnection = ConnectionType.UDP;
                    refreshOptions();
                    refreshButtonsState();
                    break;

                case R.id.btnLoopbackConnection:
                    mConnection = ConnectionType.LOOPBACK;
                    refreshOptions();
                    refreshButtonsState();
                    break;
            }
        }
    }
}
