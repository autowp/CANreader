package com.autowp.canreader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView tv = (TextView)findViewById(R.id.textViewVersion);
        tv.setText(String.format(Locale.getDefault(), getString(R.string.versions), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

    }
}
