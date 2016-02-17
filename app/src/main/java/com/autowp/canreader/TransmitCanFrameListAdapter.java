package com.autowp.canreader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.autowp.can.CanFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransmitCanFrameListAdapter extends ArrayAdapter<TransmitCanFrame> {

    private boolean mConnected = false;

    public void setConnected(boolean connected) {
        this.mConnected = connected;
    }

    public interface OnChangeListener {
        void handleChange(int position, TransmitCanFrame frame);
    }

    public interface OnSingleShotListener {
        void handleSingleSot(int position, TransmitCanFrame frame);
    }

    private List<OnChangeListener> changeListeners = new ArrayList<>();

    private List<OnSingleShotListener> singleShotListeners = new ArrayList<>();

    private CompoundButton.OnCheckedChangeListener mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int position = (int) buttonView.getTag();
            TransmitCanFrame frame = getItem(position);
            if (frame.isEnabled() != isChecked) {
                frame.setEnabled(isChecked);
                triggerOnTransmitCanFrameChange(position, frame);
            }
        }
    };

    public void addListener(OnChangeListener listener) {
        changeListeners.add(listener);
    }

    public void removeListener(OnChangeListener listener) {
        changeListeners.remove(listener);
    }

    public void addListener(OnSingleShotListener listener) {
        singleShotListeners.add(listener);
    }

    public void removeListener(OnSingleShotListener listener) {
        singleShotListeners.remove(listener);
    }

    private void triggerOnTransmitCanFrameChange(int position, TransmitCanFrame frame) {
        for (OnChangeListener changeListener : changeListeners) {
            changeListener.handleChange(position, frame);
        }
    }

    private void triggerSingleShot(int position, TransmitCanFrame frame) {
        for (OnSingleShotListener listener : singleShotListeners) {
            listener.handleSingleSot(position, frame);
        }
    }

    public TransmitCanFrameListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public TransmitCanFrameListAdapter(Context context, int resource, List<TransmitCanFrame> items) {
        super(context, resource, items);
    }

    public void updateView(View v, TransmitCanFrame frame) {
        TextView textViewCount = (TextView) v.findViewById(R.id.listitem_transmit_count);
        textViewCount.setText(String.format(Locale.getDefault(), "%d", frame.getCount()));

        Switch switchEnabled = (Switch) v.findViewById(R.id.listitem_transmit_switch);
        if (frame.isEnabled() != switchEnabled.isChecked()) {
            switchEnabled.setOnCheckedChangeListener(null);
            switchEnabled.setChecked(frame.isEnabled());
        }

        switchEnabled.setOnCheckedChangeListener(mCheckedChangeListener);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.listitem_transmit, parent, false);
        }

        TransmitCanFrame frame = getItem(position);

        if (frame != null) {
            CanFrame canFrame = frame.getCanFrame();

            TextView textViewID = (TextView) v.findViewById(R.id.listitem_transmit_id);
            if (canFrame.isExtended()) {
                textViewID.setText(String.format("%08X", canFrame.getId()));
            } else {
                textViewID.setText(String.format("%03X", canFrame.getId()));
            }
            textViewID.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mConnected) {
                        TransmitCanFrame frame = getItem(position);
                        triggerSingleShot(position, frame);
                    }
                }
            });


            TextView textViewPeriod = (TextView) v.findViewById(R.id.listitem_transmit_period);
            textViewPeriod.setText(String.format(Locale.getDefault(), "%dms", frame.getPeriod()));

            byte dlc = canFrame.getDLC();

            View rtrLine = v.findViewById(R.id.listitem_transmit_rtr_line);
            View dataLine = v.findViewById(R.id.listitem_transmit_data);

            rtrLine.setVisibility(canFrame.isRTR() ? View.VISIBLE : View.GONE);
            dataLine.setVisibility(canFrame.isRTR() ? View.GONE : View.VISIBLE);

            if (canFrame.isRTR()) {

                TextView textViewData = (TextView) v.findViewById(R.id.listitem_transmit_dlc);
                textViewData.setText(String.format("%d", dlc));

            } else {

                byte[] data = canFrame.getData();

                int textViewDataID[] = {
                    R.id.listitem_transmit_data0,
                    R.id.listitem_transmit_data1,
                    R.id.listitem_transmit_data2,
                    R.id.listitem_transmit_data3,
                    R.id.listitem_transmit_data4,
                    R.id.listitem_transmit_data5,
                    R.id.listitem_transmit_data6,
                    R.id.listitem_transmit_data7,
                };

                for (int i=0; i<data.length; i++) {
                    TextView textViewData = (TextView) v.findViewById(textViewDataID[i]);
                    textViewData.setText(String.format("%02X", data[i]));
                }

                for (int i=data.length; i<CanFrame.MAX_DLC; i++) {
                    TextView textViewData = (TextView) v.findViewById(textViewDataID[i]);
                    textViewData.setText("");
                }
            }



            Switch switchEnabled = (Switch) v.findViewById(R.id.listitem_transmit_switch);
            switchEnabled.setEnabled(mConnected);
            switchEnabled.setTag(position);

            updateView(v, frame);
        }

        return v;
    }

}
