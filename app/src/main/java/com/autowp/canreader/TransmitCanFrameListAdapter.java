package com.autowp.canreader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.autowp.can.CanFrame;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for transmit CAN frames - MD3 style.
 */
public class TransmitCanFrameListAdapter extends RecyclerView.Adapter<TransmitCanFrameListAdapter.ViewHolder> {

    private List<TransmitCanFrame> items;
    private boolean mConnected = false;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    private OnItemLongClickListener longClickListener;

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public interface OnChangeListener {
        void handleChange(int position, TransmitCanFrame frame);
    }

    public interface OnSingleShotListener {
        void handleSingleSot(int position, TransmitCanFrame frame);
    }

    private List<OnChangeListener> changeListeners = new ArrayList<>();
    private List<OnSingleShotListener> singleShotListeners = new ArrayList<>();

    public TransmitCanFrameListAdapter(List<TransmitCanFrame> items) {
        this.items = new ArrayList<>(items);
    }

    public void setConnected(boolean connected) {
        this.mConnected = connected;
        notifyDataSetChanged();
    }

    public void updateItems(List<TransmitCanFrame> newItems) {
        items.clear();
        items.addAll(new ArrayList<>(newItems));
        notifyDataSetChanged();
    }

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

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_transmit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public TransmitCanFrame getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public List<TransmitCanFrame> getItems() {
        return items;
    }

    public void clear() {
        int count = items.size();
        items.clear();
        if (count > 0) {
            notifyItemRangeRemoved(0, count);
        }
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
        MaterialSwitch swEnabled;

        ViewHolder(View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.listitem_transmit_id);
            tvPeriod = itemView.findViewById(R.id.listitem_transmit_period);
            tvCount = itemView.findViewById(R.id.listitem_transmit_count);
            tvRtr = itemView.findViewById(R.id.listitem_transmit_rtr);
            tvDlc = itemView.findViewById(R.id.listitem_transmit_dlc);
            tvExtended = itemView.findViewById(R.id.listitem_transmit_extended);
            rtrLine = itemView.findViewById(R.id.listitem_transmit_rtr_line);
            dataLine = itemView.findViewById(R.id.listitem_transmit_data);
            swEnabled = itemView.findViewById(R.id.listitem_transmit_switch);

            tvData[0] = itemView.findViewById(R.id.listitem_transmit_data0);
            tvData[1] = itemView.findViewById(R.id.listitem_transmit_data1);
            tvData[2] = itemView.findViewById(R.id.listitem_transmit_data2);
            tvData[3] = itemView.findViewById(R.id.listitem_transmit_data3);
            tvData[4] = itemView.findViewById(R.id.listitem_transmit_data4);
            tvData[5] = itemView.findViewById(R.id.listitem_transmit_data5);
            tvData[6] = itemView.findViewById(R.id.listitem_transmit_data6);
            tvData[7] = itemView.findViewById(R.id.listitem_transmit_data7);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (longClickListener != null) {
                        longClickListener.onItemLongClick(getAdapterPosition());
                    }
                    return true;
                }
            });
        }

        void bind(TransmitCanFrame frame, int position) {
            CanFrame canFrame = frame.getCanFrame();

            // ID
            if (canFrame.isExtended()) {
                tvId.setText(String.format(Locale.ROOT, "%08X", canFrame.getId()));
                tvExtended.setVisibility(View.VISIBLE);
                tvExtended.setText("29");
            } else {
                tvId.setText(String.format(Locale.ROOT, "%03X", canFrame.getId()));
                tvExtended.setVisibility(View.GONE);
            }

            // Single shot on ID click
            tvId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mConnected) {
                        triggerSingleShot(position, frame);
                    }
                }
            });

            // Period
            tvPeriod.setText(String.format(Locale.getDefault(), "%dms", frame.getPeriod()));

            // Count
            tvCount.setText(String.format(Locale.getDefault(), "%d", frame.getCount()));

            // RTR line
            boolean isRtr = canFrame.isRTR();
            rtrLine.setVisibility(isRtr ? View.VISIBLE : View.GONE);
            dataLine.setVisibility(isRtr ? View.GONE : View.VISIBLE);

            if (isRtr) {
                tvDlc.setText(String.format(Locale.getDefault(), "%d", canFrame.getDLC()));
            } else {
                // Data bytes
                byte[] data = canFrame.getData();
                for (int i = 0; i < CanFrame.MAX_DLC; i++) {
                    if (i < data.length) {
                        tvData[i].setVisibility(View.VISIBLE);
                        tvData[i].setText(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
                    } else {
                        tvData[i].setVisibility(View.GONE);
                    }
                }
            }

            // Switch
            swEnabled.setEnabled(mConnected);
            swEnabled.setTag(position);
            swEnabled.setOnCheckedChangeListener(null);
            swEnabled.setChecked(frame.isEnabled());
            swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Object tag = buttonView.getTag();
                if (!(tag instanceof Integer)) {
                    return;
                }
                int pos = (Integer) tag;
                if (pos < 0 || pos >= getItemCount()) {
                    return;
                }
                TransmitCanFrame f = getItem(pos);
                if (f != null && f.isEnabled() != isChecked) {
                    f.setEnabled(isChecked);
                    triggerOnTransmitCanFrameChange(pos, f);
                }
            });
        }
    }
}
