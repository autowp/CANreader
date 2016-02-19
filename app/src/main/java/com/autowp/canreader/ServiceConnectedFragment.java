package com.autowp.canreader;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by autow on 14.02.2016.
 */
public abstract class ServiceConnectedFragment extends Fragment {
    protected CanReaderService canReaderService;
    protected boolean bound = false;

    ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            canReaderService = ((CanReaderService.TransferServiceBinder) binder).getService();
            bound = true;

            afterConnect();
        }

        public void onServiceDisconnected(ComponentName name) {
            beforeDisconnect();

            bound = false;
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent = new Intent(getActivity(), CanReaderService.class);

        getActivity().startService(intent);
        getActivity().bindService(intent, serviceConnection, AppCompatActivity.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (bound) {
            beforeDisconnect();
            getActivity().unbindService(serviceConnection);
            bound = false;
        }
    }

    abstract protected void afterConnect();

    abstract protected void beforeDisconnect();
}
