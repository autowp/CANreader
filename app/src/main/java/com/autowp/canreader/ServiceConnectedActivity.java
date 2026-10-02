package com.autowp.canreader;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by autowp on 26.03.2016.
 */
public abstract class ServiceConnectedActivity extends AppCompatActivity {
    protected CanReaderService canReaderService;
    protected boolean bound = false;
    private boolean bindingRequested = false;

    ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (!bindingRequested || !getLifecycle().getCurrentState().isAtLeast(
                    androidx.lifecycle.Lifecycle.State.RESUMED)) {
                return;
            }
            canReaderService = ((CanReaderService.TransferServiceBinder) binder).getService();
            bound = true;

            afterConnect();
        }

        public void onServiceDisconnected(ComponentName name) {
            if (bound) {
                beforeDisconnect();
            }
            bound = false;
            canReaderService = null;
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();

        if (!bindingRequested) {
            Intent intent = new Intent(this, CanReaderService.class);
            startService(intent);
            bindingRequested = bindService(intent, serviceConnection, AppCompatActivity.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (bound) {
            beforeDisconnect();
        }
        if (bindingRequested) {
            bindingRequested = false;
            unbindService(serviceConnection);
        }
        bound = false;
        canReaderService = null;
    }

    abstract protected void afterConnect();

    abstract protected void beforeDisconnect();
}
