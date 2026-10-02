package com.autowp.canreader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for traced CAN messages.
 */
public class TracerMessageListAdapter extends RecyclerView.Adapter<TracerMessageListAdapter.ViewHolder> {

    private List<TracerMessage> items = new ArrayList<>();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    public void addMessage(TracerMessage message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    public void addMessages(List<TracerMessage> newMessages) {
        int start = items.size();
        items.addAll(newMessages);
        notifyItemRangeInserted(start, newMessages.size());
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public void setMessages(List<TracerMessage> messages) {
        items.clear();
        items.addAll(messages);
        notifyDataSetChanged();
    }

    public List<TracerMessage> getMessages() {
        return items;
    }

    public int getMessageCount() {
        return items.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_monitor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId;
        TextView tvPeriod;
        TextView tvCount;
        TextView tvRtr;
        TextView tvDlc;
        View rtrLine;
        View dataLine;
        TextView[] tvData = new TextView[8];

        ViewHolder(View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.listitem_monitor_id);
            tvPeriod = itemView.findViewById(R.id.listitem_monitor_period);
            tvCount = itemView.findViewById(R.id.listitem_monitor_count);
            tvRtr = itemView.findViewById(R.id.listitem_monitor_rtr);
            tvDlc = itemView.findViewById(R.id.listitem_monitor_dlc);
            rtrLine = itemView.findViewById(R.id.listitem_monitor_rtr_line);
            dataLine = itemView.findViewById(R.id.listitem_monitor_data);

            tvData[0] = itemView.findViewById(R.id.listitem_monitor_data0);
            tvData[1] = itemView.findViewById(R.id.listitem_monitor_data1);
            tvData[2] = itemView.findViewById(R.id.listitem_monitor_data2);
            tvData[3] = itemView.findViewById(R.id.listitem_monitor_data3);
            tvData[4] = itemView.findViewById(R.id.listitem_monitor_data4);
            tvData[5] = itemView.findViewById(R.id.listitem_monitor_data5);
            tvData[6] = itemView.findViewById(R.id.listitem_monitor_data6);
            tvData[7] = itemView.findViewById(R.id.listitem_monitor_data7);
        }

        void bind(TracerMessage message) {
            com.autowp.can.CanMessage canMessage = message.getCanMessage();

            // ID
            if (canMessage.isExtended()) {
                tvId.setText(String.format(Locale.ROOT, "%08X", canMessage.getId()));
            } else {
                tvId.setText(String.format(Locale.ROOT, "%03X", canMessage.getId()));
            }

            // Timestamp as period
            Date ts = message.getTimestamp();
            if (ts != null) {
                tvPeriod.setText(timeFormat.format(ts));
            } else {
                tvPeriod.setText("");
            }

            // Count = message index
            tvCount.setText(String.valueOf(getAdapterPosition() + 1));

            // RTR line
            boolean isRtr = canMessage.isRTR();
            rtrLine.setVisibility(isRtr ? View.VISIBLE : View.GONE);
            dataLine.setVisibility(isRtr ? View.GONE : View.VISIBLE);

            if (isRtr) {
                tvDlc.setText(String.format(Locale.getDefault(), "%d", canMessage.getDLC()));
            } else {
                // Data bytes
                byte[] data = canMessage.getData();
                for (int i = 0; i < 8; i++) {
                    if (i < data.length) {
                        tvData[i].setVisibility(View.VISIBLE);
                        tvData[i].setText(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
                    } else {
                        tvData[i].setVisibility(View.GONE);
                    }
                }
            }
        }
    }
}
