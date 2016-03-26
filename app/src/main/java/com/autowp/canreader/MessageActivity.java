package com.autowp.canreader;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.autowp.can.CanFrame;
import com.autowp.can.CanMessage;

import java.util.Locale;

public class MessageActivity extends ServiceConnectedActivity implements CanReaderService.OnMonitorChangeListener {

    public static final String EXTRA_CAN_ID = "can_id";
    private int mId;

    private void applyMessage(MonitorCanMessage message) throws Exception {
        CanMessage canMessage = message.getCanMessage();

        TextView textViewID = (TextView) findViewById(R.id.activity_message_id);

        if (textViewID == null) {
            throw new Exception("Text view not found");
        }

        if (canMessage.isExtended()) {
            textViewID.setText(String.format("%08X", canMessage.getId()));
        } else {
            textViewID.setText(String.format("%03X", canMessage.getId()));
        }

        TextView textViewPeriod = (TextView) findViewById(R.id.activity_message_period);
        textViewPeriod.setText(String.format(Locale.getDefault(), "%dms", message.getPeriod()));

        TextView textViewCount = (TextView) findViewById(R.id.activity_message_count);
        textViewCount.setText(String.format(Locale.getDefault(), "%d", message.getCount()));

        byte dlc = canMessage.getDLC();

        View rtrLine = findViewById(R.id.activity_message_rtr_line);
        View dataLine = findViewById(R.id.activity_message_data);

        rtrLine.setVisibility(canMessage.isRTR() ? View.VISIBLE : View.GONE);
        dataLine.setVisibility(canMessage.isRTR() ? View.GONE : View.VISIBLE);

        if (canMessage.isRTR()) {

            TextView textViewData = (TextView) findViewById(R.id.activity_message_dlc);
            textViewData.setText(String.format("%d", dlc));

        } else {

            byte[] data = canMessage.getData();

            int textViewDataID[] = {
                    R.id.activity_message_data0,
                    R.id.activity_message_data1,
                    R.id.activity_message_data2,
                    R.id.activity_message_data3,
                    R.id.activity_message_data4,
                    R.id.activity_message_data5,
                    R.id.activity_message_data6,
                    R.id.activity_message_data7,
            };

            for (int i = 0; i < data.length; i++) {
                boolean highlight = message.getChangeHolder(i).isHighlight();
                TextView textViewData = (TextView) findViewById(textViewDataID[i]);
                textViewData.setText(String.format("%02X", data[i]));
                int color;
                if (highlight) {
                    color = android.R.color.holo_blue_dark;
                } else {
                    color = android.R.color.primary_text_dark;
                }
                textViewData.setTextColor(ContextCompat.getColor(MessageActivity.this, color));
            }

            for (int i = data.length; i < CanFrame.MAX_DLC; i++) {
                TextView textViewData = (TextView) findViewById(textViewDataID[i]);
                textViewData.setText("");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        mId = getIntent().getIntExtra(EXTRA_CAN_ID, 0);
    }

    @Override
    protected void afterConnect() {
        canReaderService.addListener(this);

        final MonitorCanMessage message = canReaderService.getMonitorCanMessage(mId);
        if (message != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        applyMessage(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    protected void beforeDisconnect() {
        canReaderService.removeListener(this);
    }

    @Override
    public void handleMonitorUpdated() {

    }

    @Override
    public void handleMonitorUpdated(final MonitorCanMessage message) {
        if (message.getCanMessage().getId() == mId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        applyMessage(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void handleSpeedChanged(double speed) {

    }

}
