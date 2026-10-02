package com.autowp.canreader;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.autowp.can.CanMessage;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

/**
 * Bit Map Dialog - visualizes CAN message bits.
 * Based on CarBusAnalyzer's TfrmMsgBitMap.
 * Shows each bit (0-7) for each data byte with MSB→LSB orientation.
 */
public class BitMapDialog extends Dialog {

    private CanMessage message;
    private TextView tvId, tvData, tvDlc, tvHexValue, tvDecValue;
    private LinearLayout bitGrid, byteLabels;

    public BitMapDialog(@NonNull Context context, CanMessage message) {
        super(context);
        this.message = message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.bit_map);
        setContentView(R.layout.dialog_bit_map);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Initialize views
        tvId = findViewById(R.id.tvBitMapId);
        tvData = findViewById(R.id.tvBitMapData);
        tvDlc = findViewById(R.id.tvBitMapDlc);
        tvHexValue = findViewById(R.id.tvBitMapHexValue);
        tvDecValue = findViewById(R.id.tvBitMapDecValue);
        bitGrid = findViewById(R.id.bitGrid);
        byteLabels = findViewById(R.id.byteLabels);

        // Populate data
        populateBitMap();
    }

    private void populateBitMap() {
        byte[] data = message.getData();
        int dlc = message.getDLC();

        // ID
        if (message.isExtended()) {
            tvId.setText(String.format("0x%08X", message.getId()));
        } else {
            tvId.setText(String.format("0x%03X", message.getId()));
        }

        // Data as hex string
        StringBuilder dataStr = new StringBuilder();
        for (byte b : data) {
            if (dataStr.length() > 0) dataStr.append(" ");
            dataStr.append(String.format("%02X", b & 0xFF));
        }
        tvData.setText(dataStr.toString());
        tvDlc.setText(String.format("DLC: %d bytes", dlc));

        // Total value (combine all bytes)
        long totalValue = 0;
        for (byte b : data) {
            totalValue = (totalValue << 8) | (b & 0xFF);
        }
        tvHexValue.setText(String.format("0x%016X", totalValue));
        tvDecValue.setText(String.valueOf(totalValue));

        // Clear previous bits
        bitGrid.removeAllViews();
        byteLabels.removeAllViews();

        // Create bit chips for each byte
        int totalBits = dlc * 8;
        int bitsPerRow = 8;

        for (int byteIndex = 0; byteIndex < dlc; byteIndex++) {
            byte b = data[byteIndex];

            // Add separator between bytes
            if (byteIndex > 0) {
                LinearLayout spacer = new LinearLayout(getContext());
                spacer.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                spacerParams.setMargins(8, 0, 8, 0);
                spacer.setLayoutParams(spacerParams);
                bitGrid.addView(spacer);

                LinearLayout labelSpacer = new LinearLayout(getContext());
                labelSpacer.setOrientation(LinearLayout.VERTICAL);
                labelSpacer.setLayoutParams(spacerParams);
                byteLabels.addView(labelSpacer);
            }

            // Add byte label
            TextView byteLabel = new TextView(getContext());
            byteLabel.setText(String.format("Byte %d", byteIndex));
            byteLabel.setTextColor(getContext().getColor(android.R.color.darker_gray));
            byteLabel.setTextSize(10f);
            byteLabel.setPadding(0, 0, 0, 4);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            labelParams.weight = bitsPerRow;
            labelParams.gravity = android.view.Gravity.CENTER;
            byteLabels.addView(byteLabel, labelParams);

            // Add 8 bits for this byte (MSB first)
            for (int bitPos = 7; bitPos >= 0; bitPos--) {
                int bitValue = (b >> bitPos) & 1;

                Chip chip = new Chip(getContext());
                chip.setText(String.valueOf(bitValue));
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setPadding(8, 4, 8, 4);

                // Color based on bit value
                if (bitValue == 1) {
                    chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            getContext().getColor(android.R.color.holo_blue_dark)));
                    chip.setTextColor(getContext().getColor(android.R.color.white));
                } else {
                    chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            getContext().getColor(android.R.color.transparent)));
                    chip.setTextColor(getContext().getColor(android.R.color.darker_gray));
                }

                // Chip size
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.weight = 1;
                params.setMargins(2, 2, 2, 2);
                chip.setLayoutParams(params);

                bitGrid.addView(chip);
            }
        }
    }
}
