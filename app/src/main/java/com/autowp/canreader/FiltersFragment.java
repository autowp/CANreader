package com.autowp.canreader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters fragment - Hardware and Software CAN filters
 * Based on CarBusAnalyzer's filter system:
 * - Hardware Filter (ID/Mask) - device-dependent
 * - Software Filter (Range) - ID range with exclusions
 * - Gateway - channel-to-channel message forwarding
 */
public class FiltersFragment extends Fragment {

    private TabLayout filterTabs;
    private MaterialButton buttonAddHwFilter, buttonAddSwFilter;
    private RecyclerView recyclerViewHwFilters, recyclerViewSwFilters;
    private View tvEmptyHwFilter;

    private HwFilterListAdapter hwFilterAdapter;
    private List<HwFilterItem> hwFilterList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        filterTabs = view.findViewById(R.id.filterTabs);
        buttonAddHwFilter = view.findViewById(R.id.buttonAddHwFilter);
        buttonAddSwFilter = view.findViewById(R.id.buttonAddSwFilter);
        recyclerViewHwFilters = view.findViewById(R.id.recyclerViewHwFilters);
        recyclerViewSwFilters = view.findViewById(R.id.recyclerViewSwFilters);
        tvEmptyHwFilter = view.findViewById(R.id.tvEmptyHwFilter);

        // Setup hardware filter list
        hwFilterList = new ArrayList<>();
        hwFilterAdapter = new HwFilterListAdapter(hwFilterList, this::onHwFilterClick);
        recyclerViewHwFilters.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewHwFilters.setAdapter(hwFilterAdapter);

        // Tab selection listener
        filterTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                hideTab(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Do nothing
            }
        });

        // Add hardware filter button
        buttonAddHwFilter.setOnClickListener(v -> showHwFilterDialog());

        // Add software filter button
        buttonAddSwFilter.setOnClickListener(v -> showSwFilterDialog());
    }

    private void showTab(int position) {
        viewById(R.id.cardHwFilter, View.GONE);
        viewById(R.id.cardSwFilter, View.GONE);
        viewById(R.id.cardGateway, View.GONE);

        switch (position) {
            case 0:
                viewById(R.id.cardHwFilter, View.VISIBLE);
                break;
            case 1:
                viewById(R.id.cardSwFilter, View.VISIBLE);
                break;
            case 2:
                viewById(R.id.cardGateway, View.VISIBLE);
                break;
        }
    }

    private void hideTab(int position) {
        switch (position) {
            case 0:
                viewById(R.id.cardHwFilter, View.GONE);
                break;
            case 1:
                viewById(R.id.cardSwFilter, View.GONE);
                break;
            case 2:
                viewById(R.id.cardGateway, View.GONE);
                break;
        }
    }

    private void viewById(int id, int visibility) {
        View v = getView().findViewById(id);
        if (v != null) v.setVisibility(visibility);
    }

    /**
     * Hardware filter item click handler
     */
    private void onHwFilterClick(HwFilterItem item) {
        showHwFilterDialog(item);
    }

    /**
     * Show hardware filter dialog
     */
    private void showHwFilterDialog(@Nullable HwFilterItem existingItem) {
        // TODO: Implement HW filter dialog with ID/Mask inputs
        // Based on CarBusAnalyzer's TdlgHWFilterEdit
        // Fields: Mask, Code (ID), 29bit filter checkbox, Enable checkbox
    }

    private void showHwFilterDialog() {
        showHwFilterDialog(null);
    }

    /**
     * Show software filter dialog
     */
    private void showSwFilterDialog() {
        // TODO: Implement SW filter dialog
        // Based on CarBusAnalyzer's TdlgRangeFilterEdit
        // Fields: From, To, Exclude list
    }

    /**
     * Hardware filter item
     */
    public static class HwFilterItem {
        public final int channel;
        public final int number;
        public final int id;
        public final int mask;
        public final boolean is29Bit;
        public boolean enabled;

        public HwFilterItem(int channel, int number, int id, int mask, boolean is29Bit, boolean enabled) {
            this.channel = channel;
            this.number = number;
            this.id = id;
            this.mask = mask;
            this.is29Bit = is29Bit;
            this.enabled = enabled;
        }
    }

    /**
     * Hardware filter list adapter
     */
    public static class HwFilterListAdapter 
            extends RecyclerView.Adapter<HwFilterListAdapter.ViewHolder> {

        private final List<HwFilterItem> items;
        private final OnFilterClickListener listener;

        public interface OnFilterClickListener {
            void onFilterClick(HwFilterItem item);
        }

        public HwFilterListAdapter(List<HwFilterItem> items, OnFilterClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_hw_filter, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HwFilterItem item = items.get(position);
            
            holder.tvChannel.setText(String.valueOf(item.channel));
            holder.tvNumber.setText(String.valueOf(item.number));
            holder.tvId.setText(String.format("0x%03X", item.id));
            holder.tvMask.setText(String.format("0x%08X", item.mask));
            holder.tv29Bit.setText(item.is29Bit ? "29" : "11");
            
            // Enable/disable indicator
            holder.swEnable.setChecked(item.enabled);
            holder.swEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.enabled = isChecked;
            });

            // Click to edit
            holder.itemView.setOnClickListener(v -> listener.onFilterClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvChannel, tvNumber, tvId, tvMask, tv29Bit;
            SwitchMaterial swEnable;

            ViewHolder(View itemView) {
                super(itemView);
                tvChannel = itemView.findViewById(R.id.tvFilterChannel);
                tvNumber = itemView.findViewById(R.id.tvFilterNumber);
                tvId = itemView.findViewById(R.id.tvFilterId);
                tvMask = itemView.findViewById(R.id.tvFilterMask);
                tv29Bit = itemView.findViewById(R.id.tvFilter29Bit);
                swEnable = itemView.findViewById(R.id.swFilterEnable);
            }
        }
    }
}
