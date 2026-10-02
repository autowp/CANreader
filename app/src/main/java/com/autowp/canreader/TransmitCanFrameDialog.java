package com.autowp.canreader;

import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import com.google.android.material.materialswitch.MaterialSwitch;

import com.autowp.Hex;
import com.autowp.can.CanFrame;
import com.autowp.can.CanFrameException;

import java.util.Locale;

/**
 * Created by Dmitry on 29.01.2016.
 */
public class TransmitCanFrameDialog extends DialogFragment implements View.OnClickListener {

    public static final String TRANSMIT_DIALOG_BUNDLE = "bundle";
    public static final String BUNDLE_EXTRA_POSITION = "position";
    private int position = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        requireDialog().setTitle(bundle == null
                ? R.string.action_new_transmit
                : R.string.action_transmit_edit);
        View v = inflater.inflate(R.layout.dialog_transmit, container);
        v.findViewById(R.id.switchExtended).setOnClickListener(this);
        v.findViewById(R.id.buttonOk).setOnClickListener(this);

        if (bundle != null) {
            if (bundle.containsKey(BUNDLE_EXTRA_POSITION)) {
                position = bundle.getInt(BUNDLE_EXTRA_POSITION);
            }
            try {
                TransmitCanFrame frame = TransmitCanFrame.fromBundle(bundle);
                populate(v, frame);
            } catch (CanFrameException e) {
                Log.e("TransmitCanFrameDialog", "Unable to load transmit frame", e);
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } else {
            setIdMaxLength(v);
            v.findViewById(R.id.transmitDialogDLC).setVisibility(View.GONE);
            v.findViewById(R.id.editTextData).setVisibility(View.VISIBLE);
        }

        return v;
    }

    private boolean validatePeriod()
    {
        EditText editTextPeriod = (EditText) getView().findViewById(R.id.editTextPeriod);
        String periodStr = editTextPeriod.getText().toString();
        if (periodStr.length() == 0) {
            return true;
        }
        int max = 3600000;
        try {
            int id = Integer.parseInt(periodStr);
            if (id > max) {
                editTextPeriod.setError(getString(R.string.validation_period_too_high, max));
                return false;
            }
            if (id < 0) {
                editTextPeriod.setError(getString(R.string.validation_period_negative));
                return false;
            }
        } catch (NumberFormatException e) {
            editTextPeriod.setError(getString(R.string.validation_invalid_number));
            return false;
        }

        return true;
    }

    private boolean validateDLC()
    {
        final EditText editTextDLC = (EditText)getView().findViewById(R.id.transmitDialogDLC);
        String str = editTextDLC.getText().toString();

        try {
            int id = Integer.parseInt(str, 16);
            if (id > CanFrame.MAX_DLC || id < CanFrame.MIN_DLC) {
                editTextDLC.setError(getString(
                        R.string.validation_dlc_range,
                        CanFrame.MIN_DLC,
                        CanFrame.MAX_DLC
                ));
                return false;
            }
        } catch (NumberFormatException e) {
            editTextDLC.setError(getString(R.string.validation_invalid_number));
            return false;
        }

        return true;
    }

    private boolean validateID()
    {
        final EditText editTextID = (EditText)getView().findViewById(R.id.editTextID);
        String str = editTextID.getText().toString();

        MaterialSwitch switchExtended = (MaterialSwitch)getView().findViewById(R.id.switchExtended);

        int max = switchExtended.isChecked() ? CanFrame.MAX_ID_29BIT : CanFrame.MAX_ID_11BIT;

        try {
            int id = Integer.parseInt(str, 16);
            if (id > max) {
                editTextID.setError(getString(
                        R.string.validation_can_id_max,
                        Integer.toHexString(max).toUpperCase(Locale.ROOT)
                ));
                return false;
            }
        } catch (NumberFormatException e) {
            editTextID.setError(getString(R.string.validation_invalid_number));
            return false;
        }

        return true;
    }

    public boolean validateData()
    {
        EditText editTextData = (EditText) getView().findViewById(R.id.editTextData);
        String str = editTextData.getText().toString();

        try {
            byte[] data = Hex.hexStringToByteArray(str);
            if (data.length <= 0) {
                editTextData.setError(getString(R.string.validation_data_empty));
                return false;
            }
            if (data.length > CanFrame.MAX_DLC) {
                editTextData.setError(getResources().getQuantityString(
                        R.plurals.validation_data_too_long,
                        CanFrame.MAX_DLC,
                        CanFrame.MAX_DLC
                ));
                return false;
            }
        } catch (IllegalArgumentException e) {
            editTextData.setError(getString(R.string.validation_hex_data));
            return false;
        }

        return true;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final EditText editTextID = view.findViewById(R.id.editTextID);
        editTextID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateID();
            }
        });

        EditText editTextPeriod = view.findViewById(R.id.editTextPeriod);
        editTextPeriod.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validatePeriod();
            }
        });

        final EditText editTextDLC = view.findViewById(R.id.transmitDialogDLC);
        editTextDLC.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateDLC();
            }
        });



        final EditText editTextData = view.findViewById(R.id.editTextData);
        editTextData.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateData();
            }
        });

        MaterialSwitch switchRTR = view.findViewById(R.id.switchRTR);
        switchRTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editTextDLC.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                editTextData.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            }
        });
    }

    public void populate(View view, TransmitCanFrame frame) {
        MaterialSwitch switchExt = (MaterialSwitch) view.findViewById(R.id.switchExtended);
        MaterialSwitch switchRTR = (MaterialSwitch) view.findViewById(R.id.switchRTR);
        EditText editTextID = (EditText) view.findViewById(R.id.editTextID);
        EditText editTextDLC = (EditText) view.findViewById(R.id.transmitDialogDLC);
        EditText editTextData = (EditText) view.findViewById(R.id.editTextData);
        EditText editTextPeriod = (EditText) view.findViewById(R.id.editTextPeriod);

        CanFrame canFrame = frame.getCanFrame();
        int id = canFrame.getId();
        if (canFrame.isExtended()) {
            editTextID.setText(String.format(Locale.ROOT, "%08X", id));
        } else {
            editTextID.setText(String.format(Locale.ROOT, "%03X", id));
        }

        switchRTR.setChecked(canFrame.isRTR());
        if (canFrame.isRTR()) {
            editTextDLC.setText(String.format(Locale.getDefault(), "%d", canFrame.getDLC()));
            editTextDLC.setVisibility(View.VISIBLE);
            editTextData.setVisibility(View.GONE);
        } else {
            editTextData.setText(Hex.byteArrayToHexString(canFrame.getData()));
            editTextDLC.setVisibility(View.GONE);
            editTextData.setVisibility(View.VISIBLE);
        }
        switchExt.setChecked(canFrame.isExtended());
        editTextPeriod.setText(String.format(Locale.getDefault(), "%d", frame.getPeriod()));

        setIdMaxLength(view);
    }

    public Bundle getBundle() throws CanFrameException {
        MaterialSwitch switchExt = (MaterialSwitch) getView().findViewById(R.id.switchExtended);
        MaterialSwitch switchRTR = (MaterialSwitch) getView().findViewById(R.id.switchRTR);
        EditText editTextID = (EditText) getView().findViewById(R.id.editTextID);
        EditText editTextData = (EditText) getView().findViewById(R.id.editTextData);
        EditText editTextPeriod = (EditText) getView().findViewById(R.id.editTextPeriod);
        EditText editTextDLC = (EditText) getView().findViewById(R.id.transmitDialogDLC);

        String idStr = editTextID.getText().toString();
        int id = 0;
        if (idStr.length() > 0) {
            id = Integer.parseInt(idStr, 16);
        }

        CanFrame canFrame;
        if (switchRTR.isChecked()) {
            String dlcStr = editTextDLC.getText().toString();
            byte dlc = 0;
            if (dlcStr.length() > 0) {
                dlc = Byte.parseByte(dlcStr, 16);
            }
            canFrame = new CanFrame(id, dlc, switchExt.isChecked());
        } else {
            byte[] data = Hex.hexStringToByteArray(editTextData.getText().toString());
            canFrame = new CanFrame(id, data, switchExt.isChecked());
        }
        String periodStr = editTextPeriod.getText().toString();
        int period = 0;
        if (periodStr.length() > 0) {
            period = Integer.parseInt(periodStr);
        }
        TransmitCanFrame frame = new TransmitCanFrame(canFrame, period);

        Bundle bundle = frame.toBundle();

        if (position >= 0) {
            bundle.putInt(BUNDLE_EXTRA_POSITION, position);
        }

        return bundle;
    }

    public void setIdMaxLength(View view)
    {
        MaterialSwitch switchExt = (MaterialSwitch) view.findViewById(R.id.switchExtended);
        EditText editText = (EditText) view.findViewById(R.id.editTextID);

        int maxLength = switchExt.isChecked() ? 8 : 3;
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
    }

    public void onClick(View v) {
        if (v.getId() == R.id.switchExtended) {
            setIdMaxLength(getView());
        } else if (v.getId() == R.id.buttonOk) {
            MaterialSwitch switchRTR = (MaterialSwitch) getView().findViewById(R.id.switchRTR);
            boolean isRTR = switchRTR.isChecked();
            boolean valid = validateID() && validatePeriod() && (isRTR ? validateDLC() : validateData());
            if (valid) {
                sendResult();
            }
        }
    }

    private void sendResult() {
        try {
            getParentFragmentManager().setFragmentResult(
                    TRANSMIT_DIALOG_BUNDLE,
                    getBundle()
            );
            dismiss();
        } catch (CanFrameException e) {
            Log.e("TransmitCanFrameDialog", "Unable to create transmit frame", e);
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
