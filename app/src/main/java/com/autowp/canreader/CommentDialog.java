package com.autowp.canreader;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.autowp.can.CanMessage;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

/**
 * Comment Dialog - allows setting/editing comments for CAN messages.
 * Based on CarBusAnalyzer's comment system.
 */
public class CommentDialog extends Dialog {

    private CanMessage message;
    private OnCommentChangeListener listener;
    private TextView tvMsgId, tvMsgData;
    private TextInputEditText etComment;

    public interface OnCommentChangeListener {
        void onCommentChanged(CanMessage message, String comment);
    }

    public CommentDialog(@NonNull Context context, CanMessage message, OnCommentChangeListener listener) {
        super(context);
        this.message = message;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.comment_edit);
        setContentView(R.layout.dialog_comment);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Initialize views
        tvMsgId = findViewById(R.id.tvCommentMsgId);
        tvMsgData = findViewById(R.id.tvCommentMsgData);
        etComment = findViewById(R.id.etComment);
        MaterialButton buttonCancel = findViewById(R.id.buttonCommentCancel);
        MaterialButton buttonClear = findViewById(R.id.buttonCommentClear);
        MaterialButton buttonSave = findViewById(R.id.buttonCommentSave);

        // Populate message info
        if (message.isExtended()) {
            tvMsgId.setText(String.format("ID: 0x%08X", message.getId()));
        } else {
            tvMsgId.setText(String.format("ID: 0x%03X", message.getId()));
        }

        StringBuilder dataStr = new StringBuilder();
        byte[] data = message.getData();
        for (byte b : data) {
            if (dataStr.length() > 0) dataStr.append(" ");
            dataStr.append(String.format("%02X", b & 0xFF));
        }
        tvMsgData.setText(String.format("DLC: %d | Data: %s", message.getDLC(), dataStr.toString()));

        // Load existing comment (TODO: Load from storage)
        // For now, empty comment
        etComment.setText("");

        // Cancel button
        buttonCancel.setOnClickListener(v -> dismiss());

        // Clear button
        buttonClear.setOnClickListener(v -> etComment.setText(""));

        // Save button
        buttonSave.setOnClickListener(v -> {
            String comment = etComment.getText() != null ? etComment.getText().toString() : "";
            if (listener != null) {
                listener.onCommentChanged(message, comment);
            }
            dismiss();
        });
    }
}
