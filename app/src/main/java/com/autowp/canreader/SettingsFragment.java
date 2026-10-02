package com.autowp.canreader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Settings fragment - application settings based on CarBusAnalyzer settings.
 * Categories: Appearance, Receive, Transmit, Stream & Tracer, Filters
 */
public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize settings
        initReceiveSettings(view);
        initTransmitSettings(view);
        initStreamSettings(view);
        initFilterSettings(view);
    }

    private void initReceiveSettings(View view) {
        SwitchMaterial swUniqueAggregator = view.findViewById(R.id.swUniqueAggregator);
        SwitchMaterial swHighlightChanged = view.findViewById(R.id.swHighlightChanged);
        TextInputEditText etClearAfter = view.findViewById(R.id.etClearAfter);

        // Load saved settings (TODO: Use SharedPreferences)
        // For now, set defaults
        swUniqueAggregator.setChecked(true);
        swHighlightChanged.setChecked(true);
        etClearAfter.setText("1000");

        // Save on change
        swUniqueAggregator.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });

        swHighlightChanged.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });
    }

    private void initTransmitSettings(View view) {
        SwitchMaterial swRestoreTransmit = view.findViewById(R.id.swRestoreTransmit);
        SwitchMaterial swTransmitWindow = view.findViewById(R.id.swTransmitWindow);

        swRestoreTransmit.setChecked(false);
        swTransmitWindow.setChecked(false);

        swRestoreTransmit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });

        swTransmitWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });
    }

    private void initStreamSettings(View view) {
        SwitchMaterial swMaxDelay = view.findViewById(R.id.swMaxDelay);
        SwitchMaterial swCopyHeader = view.findViewById(R.id.swCopyHeader);

        swMaxDelay.setChecked(false);
        swCopyHeader.setChecked(false);

        swMaxDelay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });

        swCopyHeader.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });
    }

    private void initFilterSettings(View view) {
        SwitchMaterial swFilterAutoEnabled = view.findViewById(R.id.swFilterAutoEnabled);
        SwitchMaterial swRestoreFilter = view.findViewById(R.id.swRestoreFilter);
        SwitchMaterial swSimpleMode = view.findViewById(R.id.swSimpleMode);

        swFilterAutoEnabled.setChecked(false);
        swRestoreFilter.setChecked(false);
        swSimpleMode.setChecked(false);

        swFilterAutoEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });

        swRestoreFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });

        swSimpleMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Save to SharedPreferences
        });
    }
}
