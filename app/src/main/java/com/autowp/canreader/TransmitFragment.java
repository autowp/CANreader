package com.autowp.canreader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.autowp.can.CanClient;
import com.autowp.can.CanFrameException;

public class TransmitFragment extends ServiceConnectedFragment {
    private static final int REQUEST_CODE_NEW = 1;
    private static final int REQUEST_CODE_EDIT = 2;
    private TransmitCanFrameListAdapter adapter;

    private CanReaderService.OnStateChangeListener mOnStateChangeListener = new CanReaderService.OnStateChangeListener() {

        @Override
        public void handleStateChanged() {
            updateButtons();
            if (adapter != null) {
                adapter.setConnected(canReaderService.getConnectionState() == CanClient.ConnectionState.CONNECTED);
            }
        }
    };

    private CanReaderService.OnTransmitChangeListener mOnTransmitChangeListener = new CanReaderService.OnTransmitChangeListener() {
        @Override
        public void handleTransmitUpdated() {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public void handleTransmitUpdated(final TransmitCanFrame frame) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int start = mListView.getFirstVisiblePosition();
                        int end = mListView.getLastVisiblePosition();
                        for(int i=start; i<=end; i++)
                            if (frame == mListView.getItemAtPosition(i)){
                                View view = mListView.getChildAt(i-start);
                                adapter.updateView(view, frame);
                                break;
                            }
                    }
                });
            }
        }

        @Override
        public void handleSpeedChanged(final double speed) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv = (TextView)getView().findViewById(R.id.textViewTransmitSpeed2);
                        if (tv != null) {
                            tv.setText(String.format("%.2f frame/sec", speed));
                        }
                    }
                });
            }
        }
    };

    private ListView mListView;

    public TransmitFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //context = container.getContext();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_transmit, container, false);

        Button buttonNewTransmit = (Button) view.findViewById(R.id.buttonNewTransmit);
        buttonNewTransmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransmitCanFrameDialog newDialog = new TransmitCanFrameDialog();
                newDialog.setTargetFragment(TransmitFragment.this, REQUEST_CODE_NEW);
                newDialog.show(getFragmentManager(), "new_transmit");
            }
        });

        Button buttonStartAll = (Button) view.findViewById(R.id.buttonStartAll);
        buttonStartAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canReaderService.startAllTransmits();
                updateButtons();
            }
        });

        Button buttonStopAll = (Button) view.findViewById(R.id.buttonStopAll);
        buttonStopAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canReaderService.stopAllTransmits();
                updateButtons();
            }
        });

        Button buttonClear = (Button) view.findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canReaderService.clearTransmits();
                updateButtons();
            }
        });

        Button buttoReset = (Button) view.findViewById(R.id.buttonTransmitReset);
        buttoReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canReaderService.resetTransmits();
            }
        });

        return view;
    }

    private void updateButtons()
    {
        boolean isConnected = canReaderService.getConnectionState() == CanClient.ConnectionState.CONNECTED;

        Button buttonStartAll = (Button) getView().findViewById(R.id.buttonStartAll);
        buttonStartAll.setEnabled(isConnected && canReaderService.hasStoppedTransmits());

        Button buttonStopAll = (Button) getView().findViewById(R.id.buttonStopAll);
        buttonStopAll.setEnabled(isConnected && canReaderService.hasStartedTransmits());

        Button buttonClear = (Button) getView().findViewById(R.id.buttonClear);
        buttonClear.setEnabled(canReaderService.getTransmitFrames().size() > 0);

        Button buttoReset = (Button) getView().findViewById(R.id.buttonTransmitReset);
        buttoReset.setEnabled(canReaderService.getTransmitFrames().size() > 0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListView = (ListView)getView().findViewById(R.id.listViewTransmit);

        registerForContextMenu(mListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.listViewTransmit) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.transmit_item_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            switch (item.getItemId()) {
                case R.id.action_transmit_delete: {
                    canReaderService.remove(info.position);
                    updateButtons();
                    return true;
                }
                case R.id.action_transmit_edit: {
                    TransmitCanFrame frame = adapter.getItem(info.position);
                    if (frame != null) {

                        TransmitCanFrameDialog newDialog = new TransmitCanFrameDialog();
                        newDialog.setTargetFragment(TransmitFragment.this, REQUEST_CODE_EDIT);
                        Bundle bundle = frame.toBundle();
                        bundle.putInt(TransmitCanFrameDialog.BUNDLE_EXTRA_POSITION, info.position);
                        newDialog.setArguments(bundle);
                        newDialog.show(getFragmentManager(), "edit_transmit");
                    }
                    return true;
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        registerForContextMenu(mListView);
        if (canReaderService != null && adapter != null) {
            boolean isConnected = canReaderService.getConnectionState() == CanClient.ConnectionState.CONNECTED;
            adapter.setConnected(isConnected);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_CODE_NEW: {
                if (resultCode == 1) {

                    Bundle bundle = intent.getBundleExtra(TransmitCanFrameDialog.TRANSMIT_DIALOG_BUNDLE);

                    try {
                        TransmitCanFrame frame = TransmitCanFrame.fromBundle(bundle);
                        canReaderService.add(frame);
                    } catch (CanFrameException e) {
                        e.printStackTrace();
                    }

                    updateButtons();
                }
                break;
            }

            case REQUEST_CODE_EDIT: {
                if (resultCode == 1) {

                    Bundle bundle = intent.getBundleExtra(TransmitCanFrameDialog.TRANSMIT_DIALOG_BUNDLE);

                    //TransmitCanFrame frame = TransmitCanFrame.fromBundle(bundle);
                    int position = bundle.getInt(TransmitCanFrameDialog.BUNDLE_EXTRA_POSITION);
                    try {
                        TransmitCanFrame transmit = adapter.getItem(position);
                        if (transmit.isEnabled()) {
                            canReaderService.stopTransmit(transmit);
                            transmit.setFromBundle(bundle);
                            canReaderService.startTransmit(transmit);
                        } else {
                            transmit.setFromBundle(bundle);
                        }
                    } catch (CanFrameException e) {
                        e.printStackTrace();
                    }

                    adapter.notifyDataSetChanged();

                    updateButtons();
                }
                break;
            }
        }
    }

    @Override
    protected void afterConnect()
    {
        canReaderService.addListener(mOnTransmitChangeListener);
        canReaderService.addListener(mOnStateChangeListener);

        adapter = new TransmitCanFrameListAdapter(
                getActivity().getApplicationContext(),
                R.layout.listitem_transmit,
                canReaderService.getTransmitFrames()
        );
        adapter.addListener(new TransmitCanFrameListAdapter.OnChangeListener() {

            @Override
            public void handleChange(int position, TransmitCanFrame frame) {
                if (frame.isEnabled()) {
                    canReaderService.startTransmit(frame);
                } else {
                    canReaderService.stopTransmit(frame);
                }
                updateButtons();
            }
        });

        adapter.addListener(new TransmitCanFrameListAdapter.OnSingleShotListener() {
            @Override
            public void handleSingleSot(int position, TransmitCanFrame frame) {
                canReaderService.transmit(frame);
            }
        });

        boolean isConnected = canReaderService.getConnectionState() == CanClient.ConnectionState.CONNECTED;
        adapter.setConnected(isConnected);

        mListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        updateButtons();
    }

    @Override
    protected void beforeDisconnect() {
        canReaderService.removeListener(mOnTransmitChangeListener);
        
        mListView.setAdapter(null);
        adapter = null;

        updateButtons();
    }
}