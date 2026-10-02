package com.autowp.canreader;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;
import java.util.Locale;

/**
 * Marker Dialog - allows adding markers/breakpoints to trace/stream.
 * Based on CarBusAnalyzer's marker system.
 */
public class MarkerDialog extends Dialog {

    private OnMarkerAddListener listener;
    private TextView tvTimestamp;
    private TextInputEditText etLabel, etComment;
    private long timestamp;

    public interface OnMarkerAddListener {
        void onMarkerAdded(MarkerItem marker);
    }

    public static class MarkerItem {
        public final long timestamp;
        public final String label;
        public final String comment;
        public final Date date;

        public MarkerItem(long timestamp, String label, String comment) {
            this.timestamp = timestamp;
            this.label = label;
            this.comment = comment;
            this.date = new Date(timestamp);
        }
    }

    public MarkerDialog(@NonNull Context context, long timestamp, OnMarkerAddListener listener) {
        super(context);
        this.timestamp = timestamp;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.marker_add);
        setContentView(R.layout.dialog_marker);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Initialize views
        tvTimestamp = findViewById(R.id.tvMarkerTimestamp);
        etLabel = findViewById(R.id.etMarkerLabel);
        etComment = findViewById(R.id.etMarkerComment);
        MaterialButton buttonCancel = findViewById(R.id.buttonMarkerCancel);
        MaterialButton buttonSave = findViewById(R.id.buttonMarkerSave);

        // Display timestamp
        Date date = new Date(timestamp);
        tvTimestamp.setText(String.format("Time: %s", date.toLocaleString()));

        // Cancel button
        buttonCancel.setOnClickListener(v -> dismiss());

        // Save button
        buttonSave.setOnClickListener(v -> {
            String label = etLabel.getText() != null ? etLabel.getText().toString() : "";
            String comment = etComment.getText() != null ? etComment.getText().toString() : "";

            if (listener != null) {
                listener.onMarkerAdded(new MarkerItem(timestamp, label, comment));
            }
            dismiss();
        });
    }
}
