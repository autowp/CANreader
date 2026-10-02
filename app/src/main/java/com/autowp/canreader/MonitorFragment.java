package com.autowp.canreader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MonitorFragment extends ServiceConnectedFragment {

    private MonitorCanMessageListAdapter adapter;
    private RecyclerView recyclerView;
    private int lastContextMenuPosition = -1;
    private int lastMenuItemId = -1;

    private CanReaderService.OnMonitorChangedListener mOnMonitorChangedListener = new CanReaderService.OnMonitorChangedListener() {
        @Override
        public void handleMonitorUpdated() {
            FragmentActivity activity = getActivity();
            if (activity != null && adapter != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.updateItems(canReaderService.getMonitorFrames());
                    }
                });
            }
        }

        @Override
        public void handleMonitorUpdated(final MonitorCanMessage message) {
            FragmentActivity activity = getActivity();
            if (activity != null && adapter != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int position = adapter.getItems().indexOf(message);
                        if (position >= 0) {
                            adapter.notifyItemChanged(position);
                        }
                    }
                });
            }
        }

        @Override
        public void handleSpeedChanged(final double speed) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        View view = getView();
                        if (view == null) return;
                        TextView tv = view.findViewById(R.id.textViewMonitorSpeed);
                        if (tv != null) {
                            tv.setText(String.format(Locale.getDefault(), "%.2f frames/sec", speed));
                        }
                    }
                });
            }
        }
    };

    public MonitorFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monitor, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewMonitor);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);

        // Создаём adapter ДО подключения к сервису, чтобы afterConnect() мог его использовать
        adapter = new MonitorCanMessageListAdapter(
                getActivity().getApplicationContext(),
                new ArrayList<>()
        );
        recyclerView.setAdapter(adapter);

        Button buttonMonitorClear = view.findViewById(R.id.buttonMonitorClear);
        buttonMonitorClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canReaderService != null) {
                    canReaderService.clearMonitor();
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        registerForContextMenu(recyclerView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.monitor_item_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        lastMenuItemId = item.getItemId();
        int position = lastContextMenuPosition;
        lastContextMenuPosition = -1;
        if (position >= 0 && adapter != null && position < adapter.getItemCount()) {
            if (lastMenuItemId == R.id.action_monitor_delete) {
                canReaderService.removeMonitor(position);
                return true;
            } else if (lastMenuItemId == R.id.action_monitor_copy) {
                MonitorCanMessage message = adapter.getItem(position);
                if (message != null) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity()
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("CAN message", message.getCanMessage().toString());
                    clipboard.setPrimaryClip(clip);
                    return true;
                }
            } else if (lastMenuItemId == R.id.action_monitor_focus) {
                MonitorCanMessage message = adapter.getItem(position);
                if (message != null) {
                    Intent intent = new Intent(getActivity(), MessageActivity.class);
                    intent.putExtra(MessageActivity.EXTRA_CAN_ID, message.getCanMessage().getId());
                    startActivity(intent);
                    return true;
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        lastContextMenuPosition = -1;
        
        // Загружаем данные при каждом onResume на случай если afterConnect уже был вызван
        if (adapter != null && canReaderService != null) {
            adapter.updateItems(canReaderService.getMonitorFrames());
        }
    }

    @Override
    protected void afterConnect() {
        canReaderService.addListener(mOnMonitorChangedListener);
        
        // Обновляем adapter данными из сервиса (adapter может быть null если afterConnect вызван раньше onCreateView)
        if (adapter != null) {
            adapter.updateItems(canReaderService.getMonitorFrames());
        }
    }

    @Override
    protected void beforeDisconnect() {
        canReaderService.removeListener(mOnMonitorChangedListener);
        adapter = null;
    }
}
