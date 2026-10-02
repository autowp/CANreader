package com.autowp.canreader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.card.MaterialCardView;

import com.autowp.can.CanMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream (Поток) fragment - unified CAN stream viewer with recording.
 * Combines Stream + Tracer functionality:
 * - RX/TX chip filters
 * - Record/Pause/Stop controls
 * - Save/Open TRC files
 * - Real-time message display with timestamps
 */
public class StreamFragment extends Fragment {

    private RecyclerView recyclerView;
    private StreamMessageListAdapter adapter;
    private List<StreamMessage> messageList;

    private MaterialButton buttonRecord, buttonPause, buttonStop, buttonSave, buttonOpen, buttonClear;
    private Chip chipAll, chipRx, chipTx;
    private TextView tvStatus, tvCount;

    private boolean isRecording = false;
    private boolean isPaused = false;
    private int messageCount = 0;
    private StreamFilterMode filterMode = StreamFilterMode.ALL;

    private final ActivityResultLauncher<String> saveLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"), this::saveTrc);
    private final ActivityResultLauncher<String[]> openLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::openTrc);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stream, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        buttonRecord = view.findViewById(R.id.buttonRecord);
        buttonPause = view.findViewById(R.id.buttonPause);
        buttonStop = view.findViewById(R.id.buttonStop);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonOpen = view.findViewById(R.id.buttonOpen);
        buttonClear = view.findViewById(R.id.buttonClear);
        chipAll = view.findViewById(R.id.chipAll);
        chipRx = view.findViewById(R.id.chipRx);
        chipTx = view.findViewById(R.id.chipTx);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvCount = view.findViewById(R.id.tvCount);

        // Setup RecyclerView
        messageList = new ArrayList<>();
        adapter = new StreamMessageListAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup chip group - single selection
        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds == null || checkedIds.isEmpty()) {
                    chipAll.setChecked(true);
                } else if (checkedIds.size() == 1) {
                    int checkedId = checkedIds.get(0);
                    if (checkedId == R.id.chipAll) {
                        filterMode = StreamFilterMode.ALL;
                    } else if (checkedId == R.id.chipRx) {
                        filterMode = StreamFilterMode.RX_ONLY;
                    } else if (checkedId == R.id.chipTx) {
                        filterMode = StreamFilterMode.TX_ONLY;
                    }
                }
            });
        }

        // Record button
        buttonRecord.setOnClickListener(v -> startRecording());

        // Pause button
        buttonPause.setOnClickListener(v -> togglePause());

        // Stop button
        buttonStop.setOnClickListener(v -> stopRecording());

        // Clear button
        buttonClear.setOnClickListener(v -> clearMessages());

        // Save button
        buttonSave.setOnClickListener(v -> saveAsTrc());

        // Open button
        buttonOpen.setOnClickListener(v -> openTrcFile());
    }

    private void startRecording() {
        isRecording = true;
        isPaused = false;
        buttonRecord.setEnabled(false);
        buttonPause.setEnabled(true);
        buttonStop.setEnabled(true);
        tvStatus.setText(R.string.stream_recording);
        tvStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark));
    }

    private void togglePause() {
        if (isPaused) {
            isPaused = false;
            buttonPause.setText(R.string.stream_paused);
            tvStatus.setText(R.string.stream_recording);
        } else {
            isPaused = true;
            buttonPause.setText("Resume");
            tvStatus.setText(R.string.stream_paused);
        }
    }

    private void stopRecording() {
        isRecording = false;
        isPaused = false;
        buttonRecord.setEnabled(true);
        buttonPause.setEnabled(false);
        buttonPause.setText(R.string.stream_paused);
        buttonStop.setEnabled(false);
        tvStatus.setText(R.string.stream_idle);
        tvStatus.setTextColor(requireContext().getColor(android.R.color.darker_gray));
    }

    private void clearMessages() {
        messageList.clear();
        adapter.notifyDataSetChanged();
        messageCount = 0;
        tvCount.setText("0");
        buttonSave.setEnabled(false);
        buttonOpen.setEnabled(false);
    }

    private void saveAsTrc() {
        saveLauncher.launch("trace.trc");
    }

    private void saveTrc(@Nullable Uri uri) {
        if (uri == null || messageList.isEmpty()) {
            return;
        }
        try (OutputStream output = requireContext().getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("Unable to open destination");
            }
            // Convert StreamMessage to TracerMessage for export
            List<TracerMessage> tracerMessages = new ArrayList<>();
            for (StreamMessage msg : messageList) {
                tracerMessages.add(new TracerMessage(msg.frame, msg.timestamp));
            }
            TracerExporter.exportTrc(tracerMessages, output);
            Toast.makeText(requireContext(), "Saved: " + messageList.size() + " messages", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openTrcFile() {
        openLauncher.launch(new String[]{"*/*"});
    }

    private void openTrc(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        try (InputStream input = requireContext().getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Unable to open file");
            }
            List<TracerMessage> loaded = TracerImporter.importTrc(input);
            if (loaded != null && !loaded.isEmpty()) {
                messageList.clear();
                for (TracerMessage tm : loaded) {
                    messageList.add(new StreamMessage(tm.getTimestampMs(), "1", false, tm.getCanFrame()));
                }
                messageCount = messageList.size();
                tvCount.setText(String.valueOf(messageCount));
                adapter.notifyDataSetChanged();
                buttonSave.setEnabled(true);
                buttonOpen.setEnabled(true);
                Toast.makeText(requireContext(), "Loaded: " + messageCount + " messages", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Add a message to the stream
     */
    public void addMessage(StreamMessage message) {
        if (!isRecording) {
            return;
        }
        
        // Apply filter
        if (filterMode == StreamFilterMode.RX_ONLY && message.isTx) {
            return;
        }
        if (filterMode == StreamFilterMode.TX_ONLY && !message.isTx) {
            return;
        }
        
        messageList.add(message);
        messageCount++;
        tvCount.setText(String.valueOf(messageCount));
        adapter.notifyItemInserted(messageList.size() - 1);
        
        // Auto-scroll to bottom
        recyclerView.scrollToPosition(messageList.size() - 1);
        
        // Enable save after first message
        buttonSave.setEnabled(true);
        buttonOpen.setEnabled(true);
    }

    /**
     * Stream message data class
     */
    public static class StreamMessage {
        public final long timestamp;
        public final String channel;
        public final boolean isTx;
        public final CanMessage frame;

        public StreamMessage(long timestamp, String channel, boolean isTx, CanMessage frame) {
            this.timestamp = timestamp;
            this.channel = channel;
            this.isTx = isTx;
            this.frame = frame;
        }
    }

    /**
     * Stream message adapter
     */
    public static class StreamMessageListAdapter extends RecyclerView.Adapter<StreamMessageListAdapter.ViewHolder> {
        private final List<StreamMessage> messages;

        public StreamMessageListAdapter(List<StreamMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_stream, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StreamMessage msg = messages.get(position);
            
            // Format timestamp
            String timeStr = formatTimestamp(msg.timestamp);
            holder.tvTime.setText(timeStr);
            
            // Channel
            holder.tvChannel.setText(msg.channel);
            
            // Direction indicator
            holder.tvDirection.setText(msg.isTx ? "Tx" : "Rx");
            holder.tvDirection.setTextColor(msg.isTx 
                    ? holder.itemView.getContext().getColor(android.R.color.holo_blue_dark)
                    : holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
            
            // CAN ID
            holder.tvId.setText(String.format("0x%03X", msg.frame.getId()));
            
            // DLC
            holder.tvDlc.setText(String.valueOf(msg.frame.getDLC()));
            
            // Data
            holder.tvData.setText(formatData(msg.frame.getData()));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvChannel, tvDirection, tvId, tvDlc, tvData;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tvStreamTime);
                tvChannel = itemView.findViewById(R.id.tvStreamChannel);
                tvDirection = itemView.findViewById(R.id.tvStreamDirection);
                tvId = itemView.findViewById(R.id.tvStreamId);
                tvDlc = itemView.findViewById(R.id.tvStreamDlc);
                tvData = itemView.findViewById(R.id.tvStreamData);
            }
        }

        private String formatTimestamp(long timestamp) {
            // Format as HH:MM:SS.mmm
            long seconds = timestamp / 1000;
            long millis = timestamp % 1000;
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return String.format("%02d:%02d:%02d.%03d", hours, minutes, secs, millis);
        }

        private String formatData(byte[] data) {
            if (data == null || data.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (byte b : data) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(String.format("%02X", b & 0xFF));
            }
            return sb.toString();
        }
    }

    /**
     * Stream filter mode
     */
    public enum StreamFilterMode {
        ALL, RX_ONLY, TX_ONLY
    }
}
