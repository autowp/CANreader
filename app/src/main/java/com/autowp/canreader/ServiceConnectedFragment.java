package com.autowp.canreader;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by autow on 14.02.2016.
 */
public abstract class ServiceConnectedFragment extends Fragment {
    protected CanReaderService canReaderService;
    protected boolean bound = false;
    private boolean bindingRequested = false;

    ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (!bindingRequested || !isResumed()) {
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
            Intent intent = new Intent(requireActivity(), CanReaderService.class);
            requireActivity().startService(intent);
            bindingRequested = requireActivity().bindService(
                    intent, serviceConnection, AppCompatActivity.BIND_AUTO_CREATE);
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
            requireActivity().unbindService(serviceConnection);
        }
        bound = false;
        canReaderService = null;
    }

    abstract protected void afterConnect();

    abstract protected void beforeDisconnect();
}
