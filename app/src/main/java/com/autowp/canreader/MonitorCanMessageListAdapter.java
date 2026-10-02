package com.autowp.canreader;

import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.autowp.can.CanFrame;
import com.autowp.can.CanMessage;

import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for monitor CAN messages - MD3 style.
 * Uses ViewHolder pattern for optimal performance.
 */
public class MonitorCanMessageListAdapter extends RecyclerView.Adapter<MonitorCanMessageListAdapter.ViewHolder> {

    private Context context;
    private List<MonitorCanMessage> items;

    public MonitorCanMessageListAdapter(Context context, List<MonitorCanMessage> items) {
        this.context = context;
        this.items = new java.util.ArrayList<>(items);
    }

    public void updateItems(List<MonitorCanMessage> newItems) {
        items.clear();
        items.addAll(new java.util.ArrayList<>(newItems));
        notifyDataSetChanged();
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

    public MonitorCanMessage getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public List<MonitorCanMessage> getItems() {
        return items;
    }

    public void updateItem(int position, MonitorCanMessage message) {
        if (position >= 0 && position < items.size()) {
            items.set(position, message);
            notifyItemChanged(position);
        }
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId;
        TextView tvPeriod;
        TextView tvCount;
        TextView tvRtr;
        TextView tvDlc;
        TextView tvExtended;
        View rtrLine;
        View dataLine;
        TextView[] tvData = new TextView[CanFrame.MAX_DLC];
        ImageView ivComment;

        ViewHolder(View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.listitem_monitor_id);
            tvPeriod = itemView.findViewById(R.id.listitem_monitor_period);
            tvCount = itemView.findViewById(R.id.listitem_monitor_count);
            tvRtr = itemView.findViewById(R.id.listitem_monitor_rtr);
            tvDlc = itemView.findViewById(R.id.listitem_monitor_dlc);
            tvExtended = itemView.findViewById(R.id.listitem_monitor_extended);
            rtrLine = itemView.findViewById(R.id.listitem_monitor_rtr_line);
            dataLine = itemView.findViewById(R.id.listitem_monitor_data);
            ivComment = itemView.findViewById(R.id.listitem_monitor_comment_icon);

            tvData[0] = itemView.findViewById(R.id.listitem_monitor_data0);
            tvData[1] = itemView.findViewById(R.id.listitem_monitor_data1);
            tvData[2] = itemView.findViewById(R.id.listitem_monitor_data2);
            tvData[3] = itemView.findViewById(R.id.listitem_monitor_data3);
            tvData[4] = itemView.findViewById(R.id.listitem_monitor_data4);
            tvData[5] = itemView.findViewById(R.id.listitem_monitor_data5);
            tvData[6] = itemView.findViewById(R.id.listitem_monitor_data6);
            tvData[7] = itemView.findViewById(R.id.listitem_monitor_data7);
        }

        void bind(MonitorCanMessage message) {
            CanMessage canMessage = message.getCanMessage();

            // ID
            if (canMessage.isExtended()) {
                tvId.setText(String.format(Locale.ROOT, "%08X", canMessage.getId()));
                tvExtended.setVisibility(View.VISIBLE);
                tvExtended.setText("29");
            } else {
                tvId.setText(String.format(Locale.ROOT, "%03X", canMessage.getId()));
                tvExtended.setVisibility(View.GONE);
            }

            // Period and Count
            tvPeriod.setText(String.format(Locale.getDefault(), "%dms", message.getPeriod()));
            tvCount.setText(String.format(Locale.getDefault(), "%d", message.getCount()));

            // RTR line
            boolean isRtr = canMessage.isRTR();
            rtrLine.setVisibility(isRtr ? View.VISIBLE : View.GONE);
            dataLine.setVisibility(isRtr ? View.GONE : View.VISIBLE);

            if (isRtr) {
                tvDlc.setText(String.format(Locale.getDefault(), "%d", canMessage.getDLC()));
            } else {
                // Data bytes
                byte[] data = canMessage.getData();
                for (int i = 0; i < CanFrame.MAX_DLC; i++) {
                    if (i < data.length) {
                        tvData[i].setVisibility(View.VISIBLE);
                        boolean highlight = message.getChangeHolder(i).isHighlight();
                        tvData[i].setText(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
                        int color = highlight ? R.color.can_tertiary : R.color.can_on_surface;
                        tvData[i].setTextColor(ContextCompat.getColor(context, color));
                    } else {
                        tvData[i].setVisibility(View.GONE);
                    }
                }
            }

            // Comment icon - show if message has comment
            // TODO: Get comment from message or separate storage
            // For now, hide it
            ivComment.setVisibility(View.GONE);
        }
    }
}
