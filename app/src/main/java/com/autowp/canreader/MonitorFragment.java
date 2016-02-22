package com.autowp.canreader;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class MonitorFragment extends ServiceConnectedFragment {

    private MonitorCanMessageListAdapter adapter;

    private ListView mListView;

    private CanReaderService.OnMonitorChangeListener mOnMonitorChangeListener = new CanReaderService.OnMonitorChangeListener() {
        @Override
        public void handleMonitorUpdated() {
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
        public void handleMonitorUpdated(final MonitorCanMessage message) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int start = mListView.getFirstVisiblePosition();
                        int end = mListView.getLastVisiblePosition();
                        for(int i=start; i<=end; i++)
                            if (message == mListView.getItemAtPosition(i)){
                                View view = mListView.getChildAt(i-start);
                                adapter.updateView(view, message);
                                break;
                            }
                    }
                });
            }
        }


    };

    public MonitorFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_monitor, container, false);

        mListView = (ListView)view.findViewById(R.id.listViewMonitor);
        if (adapter != null) {
            mListView.setAdapter(adapter);
        }

        Button buttonMonitorClear = (Button) view.findViewById(R.id.buttonMonitorClear);
        buttonMonitorClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canReaderService.clearMonitor();
            }
        });

        return view;
    }

    @Override
    protected void afterConnect() {
        canReaderService.addListener(mOnMonitorChangeListener);

        adapter = new MonitorCanMessageListAdapter(
                getActivity().getApplicationContext(),
                R.layout.listitem_monitor,
                canReaderService.getMonitorFrames()
        );

        mListView.setAdapter(adapter);
    }

    @Override
    protected void beforeDisconnect() {
        canReaderService.removeListener(mOnMonitorChangeListener);
        mListView.setAdapter(null);
        adapter = null;
    }
}
