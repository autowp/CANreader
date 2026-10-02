package com.autowp.canreader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.autowp.can.CanAdapter;
import com.autowp.can.CanFrameException;

import java.util.Locale;

public class TransmitFragment extends ServiceConnectedFragment
        implements CanReaderService.OnConnectionStateChangedListener,
        CanReaderService.OnTransmitChangeListener
{
    private TransmitCanFrameListAdapter adapter;
    private RecyclerView recyclerView;
    private Bundle pendingDialogResult;
    private int lastContextMenuPosition = -1;
    private int lastMenuItemId = -1;

    public TransmitFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener(
                TransmitCanFrameDialog.TRANSMIT_DIALOG_BUNDLE,
                this,
                (requestKey, result) -> handleTransmitDialogResult(result)
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transmit, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTransmit);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        
        // Создаём adapter ДО подключения к сервису, чтобы afterConnect() мог его использовать
        adapter = new TransmitCanFrameListAdapter(new java.util.ArrayList<>());
        recyclerView.setAdapter(adapter);

        Button buttonNewTransmit = view.findViewById(R.id.buttonNewTransmit);
        buttonNewTransmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransmitCanFrameDialog newDialog = new TransmitCanFrameDialog();
                newDialog.show(getParentFragmentManager(), "new_transmit");
            }
        });

        Button buttonStartAll = view.findViewById(R.id.buttonStartAll);
        buttonStartAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canReaderService != null) {
                    canReaderService.startAllTransmits();
                }
                updateButtons();
            }
        });

        Button buttonStopAll = view.findViewById(R.id.buttonStopAll);
        buttonStopAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canReaderService != null) {
                    canReaderService.stopAllTransmits();
                }
                updateButtons();
            }
        });

        return view;
    }

    private void updateButtons()
    {
        View view = getView();
        if (view == null) {
            return;
        }

        boolean serviceAvailable = bound && canReaderService != null;
        boolean isConnected = serviceAvailable
                && canReaderService.getConnectionState() == CanAdapter.ConnectionState.CONNECTED;

        view.findViewById(R.id.buttonNewTransmit).setEnabled(serviceAvailable);
        Button buttonStartAll = view.findViewById(R.id.buttonStartAll);
        buttonStartAll.setEnabled(isConnected && canReaderService.hasStoppedTransmits());

        Button buttonStopAll = view.findViewById(R.id.buttonStopAll);
        buttonStopAll.setEnabled(isConnected && canReaderService.hasStartedTransmits());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateButtons();
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(Menu menu, MenuInflater menuInflater) {
            }

            @Override
            public void onPrepareMenu(Menu menu) {
                boolean hasFrames = bound && canReaderService != null
                        && !canReaderService.getTransmitFrames().isEmpty();
                MenuItem resetAll = menu.findItem(R.id.action_transmit_reset_all);
                MenuItem clear = menu.findItem(R.id.action_transmit_clear);
                if (resetAll != null) {
                    resetAll.setEnabled(hasFrames);
                }
                if (clear != null) {
                    clear.setEnabled(hasFrames);
                }
            }

            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private boolean onContextItemSelectedForPosition(int position) {
        if (!isVisible() || !bound || canReaderService == null || adapter == null) {
            return false;
        }

        if (position < 0 || position >= adapter.getItemCount()) {
            return false;
        }

        if (lastMenuItemId == R.id.action_transmit_clear) {
            canReaderService.clearTransmits();
            updateButtons();
            return true;
        } else if (lastMenuItemId == R.id.action_transmit_reset_all) {
            canReaderService.resetTransmits();
            return true;
        }

        if (lastMenuItemId == R.id.action_transmit_delete) {
            canReaderService.removeTransmit(position);
            updateButtons();
            return true;
        } else if (lastMenuItemId == R.id.action_transmit_edit) {
            TransmitCanFrame frame = adapter.getItem(position);
            if (frame != null) {
                TransmitCanFrameDialog newDialog = new TransmitCanFrameDialog();
                Bundle bundle = frame.toBundle();
                bundle.putInt(TransmitCanFrameDialog.BUNDLE_EXTRA_POSITION, position);
                newDialog.setArguments(bundle);
                newDialog.show(getParentFragmentManager(), "edit_transmit");
            }
            return true;
        } else if (lastMenuItemId == R.id.action_transmit_reset) {
            TransmitCanFrame frame = adapter.getItem(position);
            if (frame != null) {
                canReaderService.resetTransmit(frame);
            }
            return true;
        } else if (lastMenuItemId == R.id.action_transmit_copy) {
            TransmitCanFrame frame = adapter.getItem(position);
            if (frame != null) {
                ClipboardManager clipboard = (ClipboardManager) getActivity()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("CAN frame", frame.getCanFrame().toString());
                clipboard.setPrimaryClip(clip);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        lastMenuItemId = item.getItemId();
        int position = lastContextMenuPosition;
        lastContextMenuPosition = -1;
        if (position >= 0) {
            return onContextItemSelectedForPosition(position);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.transmit_item_menu, menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        lastContextMenuPosition = -1;

        if (canReaderService != null && adapter != null) {
            boolean isConnected = canReaderService.getConnectionState() == CanAdapter.ConnectionState.CONNECTED;
            adapter.setConnected(isConnected);
            // Загружаем данные при каждом onResume на случай если afterConnect уже был вызван
            adapter.updateItems(canReaderService.getTransmitFrames());
        }
    }

    private void handleTransmitDialogResult(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        if (!bound || adapter == null) {
            pendingDialogResult = new Bundle(bundle);
            return;
        }

        try {
            if (!bundle.containsKey(TransmitCanFrameDialog.BUNDLE_EXTRA_POSITION)) {
                canReaderService.add(TransmitCanFrame.fromBundle(bundle));
                // Обновляем adapter после добавления
                adapter.updateItems(canReaderService.getTransmitFrames());
            } else {
                int position = bundle.getInt(TransmitCanFrameDialog.BUNDLE_EXTRA_POSITION, -1);
                if (position < 0 || position >= adapter.getItemCount()) {
                    Log.w("TransmitFragment", "Ignoring result for missing transmit frame at " + position);
                    return;
                }
                TransmitCanFrame transmit = adapter.getItem(position);
                if (transmit == null) {
                    return;
                }
                boolean wasEnabled = transmit.isEnabled();
                if (wasEnabled) {
                    canReaderService.stopTransmit(transmit);
                }
                transmit.setFromBundle(bundle);
                if (wasEnabled) {
                    canReaderService.startTransmit(transmit);
                }
                // Обновляем adapter после редактирования
                adapter.updateItems(canReaderService.getTransmitFrames());
            }
            updateButtons();
        } catch (CanFrameException e) {
            Log.e("TransmitFragment", "Unable to apply transmit frame", e);
        }
    }

    @Override
    protected void afterConnect()
    {
        canReaderService.addListener((CanReaderService.OnConnectionStateChangedListener) this);
        canReaderService.addListener((CanReaderService.OnTransmitChangeListener) this);

        // Обновляем adapter данными из сервиса
        if (adapter != null) {
            adapter.updateItems(canReaderService.getTransmitFrames());
            adapter.addListener(new TransmitCanFrameListAdapter.OnChangeListener() {
                @Override
                public void handleChange(int position, TransmitCanFrame frame) {
                    if (frame.isEnabled()) {
                        canReaderService.startTransmit(frame);
                    } else {
                        canReaderService.stopTransmit(frame);
                    }
                    updateButtons();
                }
            });

            adapter.addListener(new TransmitCanFrameListAdapter.OnSingleShotListener() {
                @Override
                public void handleSingleSot(int position, TransmitCanFrame frame) {
                    canReaderService.transmit(frame);
                }
            });

            boolean isConnected = canReaderService.getConnectionState() == CanAdapter.ConnectionState.CONNECTED;
            adapter.setConnected(isConnected);

            // Set long press listener on adapter
            adapter.setOnItemLongClickListener(new TransmitCanFrameListAdapter.OnItemLongClickListener() {
                @Override
                public void onItemLongClick(int position) {
                    lastContextMenuPosition = position;
                    getActivity().openContextMenu(recyclerView);
                }
            });

            updateButtons();
            if (pendingDialogResult != null) {
                Bundle result = pendingDialogResult;
                pendingDialogResult = null;
                handleTransmitDialogResult(result);
            }
        }
    }

    @Override
    protected void beforeDisconnect() {
        canReaderService.removeListener((CanReaderService.OnConnectionStateChangedListener) this);
        canReaderService.removeListener((CanReaderService.OnTransmitChangeListener) this);
        // Не очищаем adapter — данные сохраняются при повороте экрана

        updateButtons();
    }

    @Override
    public void handleConnectedStateChanged(CanAdapter.ConnectionState connection) {
        updateButtons();
        if (adapter != null) {
            adapter.setConnected(canReaderService.getConnectionState() == CanAdapter.ConnectionState.CONNECTED);
        }
    }

    @Override
    public void handleTransmitUpdated() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter != null && canReaderService != null) {
                        adapter.updateItems(canReaderService.getTransmitFrames());
                    }
                    activity.invalidateOptionsMenu();
                }
            });
        }
    }

    @Override
    public void handleTransmitUpdated(final TransmitCanFrame frame) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        return;
                    }
                    int position = adapter.getItems().indexOf(frame);
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
                    if (view == null) {
                        return;
                    }
                    TextView tv = view.findViewById(R.id.textViewTransmitSpeed2);
                    if (tv != null) {
                        tv.setText(String.format(Locale.getDefault(), "%.2f frames/sec", speed));
                    }
                }
            });
        }
    }
}
