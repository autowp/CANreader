package com.autowp.canreader;

import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.AppBarLayout;

import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Main activity with Material Design 3 navigation.
 * Uses BottomNavigationView for phone, split-pane for tablet (>= 600dp).
 */
public class MainActivity extends ServiceConnectedActivity {
    private static final String TAG = "MainActivity";
    private static final String STATE_SELECTED_NAV = "selected_nav";
    private static final int MIN_TABLET_WIDTH_DP = 600;

    private List<TransmitCanFrame> mTxListToLoad = null;
    private int selectedNavId = R.id.fragment_monitor;
    private boolean isTablet = false;

    private final ActivityResultLauncher<String> createTxListLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/octet-stream"),
                    this::exportTxList
            );
    private final ActivityResultLauncher<String[]> openTxListLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::importTxList
            );

    private CanReaderService.OnTransmitChangeListener mOnTransmitChangeListener = new CanReaderService.OnTransmitChangeListener() {
        @Override
        public void handleTransmitUpdated() {
            runOnUiThread(MainActivity.this::invalidateOptionsMenu);
        }

        @Override
        public void handleTransmitUpdated(TransmitCanFrame frame) {}

        @Override
        public void handleSpeedChanged(double speed) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Detect if tablet
        isTablet = isDeviceTablet();
        
        // Set appropriate layout
        setContentView(isTablet ? R.layout.activity_main_tablet : R.layout.activity_main);

        // Setup Toolbar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        if (isTablet) {
            setupTabletLayout();
        } else {
            setupPhoneLayout();
        }

        // Initialize fragments
        initializeMainFragments();
        
        // Restore selected tab
        if (savedInstanceState != null) {
            selectedNavId = savedInstanceState.getInt(STATE_SELECTED_NAV, R.id.fragment_monitor);
        }
        
        showFragment(selectedNavId);

        // Handle intent for .txl file import
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && uri != null) {
            Log.v(TAG, "Importing transmit list from " + uri);
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    throw new IOException("Unable to open transmit list");
                }
                mTxListToLoad = TxListFile.read(input);
            } catch (ConfigurationException | IOException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Detect if device is tablet based on screen width.
     */
    private boolean isDeviceTablet() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) return false;
        
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        
        double widthInches = metrics.widthPixels / (double) metrics.xdpi;
        return widthInches >= MIN_TABLET_WIDTH_DP / 160.0;
    }

    private void setupPhoneLayout() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null) return;
        
        // Restore selected tab
        if (selectedNavId != 0) {
            bottomNav.setSelectedItemId(selectedNavId);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.fragment_monitor) {
                showFragment(R.id.fragment_monitor);
                return true;
            } else if (itemId == R.id.fragment_stream) {
                showFragment(R.id.fragment_stream);
                return true;
            } else if (itemId == R.id.fragment_transmit) {
                showFragment(R.id.fragment_transmit);
                return true;
            } else if (itemId == R.id.fragment_filters) {
                showFragment(R.id.fragment_filters);
                return true;
            }
            return false;
        });
    }

    private void setupTabletLayout() {
        // Tablet layout - hide bottom nav, show split pane
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
        
        // Show tablet split layout
        View tabletLayout = findViewById(R.id.tablet_split_layout);
        if (tabletLayout != null) {
            tabletLayout.setVisibility(View.VISIBLE);
        }
        
        // Hide phone fragment container
        View phoneContainer = findViewById(R.id.main_fragment_container);
        if (phoneContainer != null) {
            phoneContainer.setVisibility(View.GONE);
        }
    }

    private void initializeMainFragments() {
        androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
        
        // Check if fragments already exist
        if (fragmentManager.findFragmentByTag("monitor") == null) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.main_fragment_container, new MonitorFragment(), "monitor")
                    .commitNow();
        }
        if (fragmentManager.findFragmentByTag("stream") == null) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.main_fragment_container, new StreamFragment(), "stream")
                    .commitNow();
        }
        if (fragmentManager.findFragmentByTag("transmit") == null) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.main_fragment_container, new TransmitFragment(), "transmit")
                    .commitNow();
        }
        if (fragmentManager.findFragmentByTag("filters") == null) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.main_fragment_container, new FiltersFragment(), "filters")
                    .commitNow();
        }
        if (fragmentManager.findFragmentByTag("settings") == null) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.main_fragment_container, new SettingsFragment(), "settings")
                    .commitNow();
        }
    }

    private void showFragment(int fragmentId) {
        selectedNavId = fragmentId;
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction()
                .setReorderingAllowed(true);

        // Hide all fragments first
        for (Fragment f : fm.getFragments()) {
            tx.hide(f);
        }

        // Show selected fragment
        Fragment target = fm.findFragmentByTag(getFragmentTag(fragmentId));
        if (target != null) {
            tx.show(target).setMaxLifecycle(target, Lifecycle.State.RESUMED);
        }

        // Update toolbar title
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.my_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getFragmentTitle(fragmentId));
        }

        tx.commit();
    }

    private String getFragmentTag(int fragmentId) {
        if (fragmentId == R.id.fragment_monitor) return "monitor";
        if (fragmentId == R.id.fragment_stream) return "stream";
        if (fragmentId == R.id.fragment_transmit) return "transmit";
        if (fragmentId == R.id.fragment_filters) return "filters";
        if (fragmentId == R.id.fragment_settings) return "settings";
        return "monitor";
    }

    private String getFragmentTitle(int fragmentId) {
        if (fragmentId == R.id.fragment_monitor) return getString(R.string.nav_monitor);
        if (fragmentId == R.id.fragment_stream) return getString(R.string.nav_stream);
        if (fragmentId == R.id.fragment_transmit) return getString(R.string.nav_transmit);
        if (fragmentId == R.id.fragment_filters) return getString(R.string.nav_filters);
        if (fragmentId == R.id.fragment_settings) return getString(R.string.action_settings);
        return getString(R.string.app_name);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_NAV, selectedNavId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean hasMessages = bound && (canReaderService.getTransmitFrames().size() > 0);

        MenuItem exportItem = menu.findItem(R.id.action_export_tx_list);
        if (exportItem != null) exportItem.setEnabled(hasMessages);
        MenuItem shareItem = menu.findItem(R.id.action_share_tx_list);
        if (shareItem != null) shareItem.setEnabled(hasMessages);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_connection) {
            startActivity(new Intent(this, ConnectionActivity.class));
            return true;
        } else if (itemId == R.id.action_settings) {
            showSettingsFragment();
            return true;
        } else if (itemId == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        } else if (itemId == R.id.action_export_tx_list) {
            if (bound) {
                createTxListLauncher.launch("tx-list." + TxListFile.EXTENSION);
            }
            return true;
        } else if (itemId == R.id.action_share_tx_list) {
            if (bound) {
                try {
                    File txlDir = new File(getCacheDir(), "txl");
                    if (!txlDir.isDirectory() && !txlDir.mkdirs()) {
                        throw new IOException("Unable to create share directory");
                    }

                    File file = new File(txlDir, "tx-share." + TxListFile.EXTENSION);
                    try (OutputStream output = new FileOutputStream(file)) {
                        TxListFile.write(output, canReaderService.getTransmitFrames());
                    }

                    Uri fileUri = FileProvider.getUriForFile(
                            this,
                            "com.autowp.canreader.txlfileprovider",
                            file);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.setType("application/octet-stream");
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_tx_list)));
                } catch (ConfigurationException | IOException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        } else if (itemId == R.id.action_import_tx_list) {
            if (bound) {
                openTxListLauncher.launch(new String[]{"*/*"});
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportTxList(Uri uri) {
        if (uri == null || !bound) {
            return;
        }

        try {
            OutputStream output = getContentResolver().openOutputStream(uri);
            if (output == null) {
                throw new IOException("Unable to open destination");
            }
            try (OutputStream stream = output) {
                TxListFile.write(stream, canReaderService.getTransmitFrames());
            }
            Toast.makeText(this, R.string.tx_list_exported, Toast.LENGTH_SHORT).show();
        } catch (ConfigurationException | IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importTxList(Uri uri) {
        if (uri == null || !bound) {
            return;
        }

        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Unable to open selected file");
            }
            canReaderService.setTransmitFrames(TxListFile.read(input));
        } catch (ConfigurationException | IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void afterConnect() {
        if (mTxListToLoad != null) {
            canReaderService.setTransmitFrames(mTxListToLoad);
            mTxListToLoad = null;
        }

        canReaderService.addListener(mOnTransmitChangeListener);
        invalidateOptionsMenu();
    }

    @Override
    protected void beforeDisconnect() {
        canReaderService.removeListener(mOnTransmitChangeListener);
    }

    private void showSettingsFragment() {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction()
                .setReorderingAllowed(true);

        // Hide all main fragments
        for (Fragment f : fm.getFragments()) {
            tx.hide(f);
        }

        // Show or create Settings fragment
        SettingsFragment settings = (SettingsFragment) fm.findFragmentByTag("settings");
        if (settings == null) {
            settings = new SettingsFragment();
            tx.add(R.id.main_fragment_container, settings, "settings");
        } else {
            tx.show(settings);
        }

        // Update toolbar title
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.my_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.action_settings);
        }

        tx.commit();
    }
}
