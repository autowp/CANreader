package com.autowp.canreader;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.apache.commons.configuration.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int ACTION_PICK_TXL_TO_SHARE = 1;
    protected CanReaderService canReaderService;
    protected boolean bound = false;

    private List<TransmitCanFrame> mTxListToLoad = null;

    ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            canReaderService = ((CanReaderService.TransferServiceBinder) binder).getService();
            bound = true;

            if (mTxListToLoad != null) {
                canReaderService.setTransmitFrames(mTxListToLoad);
                mTxListToLoad = null;
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        Intent intent = getIntent();
        String action = intent.getAction();

        if (action.compareTo(Intent.ACTION_VIEW) == 0) {
            String scheme = intent.getScheme();
            ContentResolver resolver = getContentResolver();

            if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                Uri uri = intent.getData();

                Log.v("tag" , "Content intent detected: " + action + " : " + intent.getDataString() + " : " + intent.getType());
                try {
                    InputStream input = resolver.openInputStream(uri);

                    mTxListToLoad = TxListFile.read(input);

                    input.close();

                } catch (ConfigurationException | IOException e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            else if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
                Uri uri = intent.getData();
                String name = uri.getLastPathSegment();

                Log.v("tag" , "File intent detected: " + action + " : " + intent.getDataString() + " : " + intent.getType() + " : " + name);
                try {
                    InputStream input = resolver.openInputStream(uri);

                    mTxListToLoad = TxListFile.read(input);

                    input.close();

                } catch (ConfigurationException | IOException e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            else if (scheme.compareTo("http") == 0) {
                // TODO Import from HTTP!
            }
            else if (scheme.compareTo("ftp") == 0) {
                // TODO Import from FTP!
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connection: {
                Intent intent = new Intent(this, ConnectionActivity.class);
                startActivity(intent);
                return true;
            }

            case R.id.action_settings:
                return true;

            case R.id.action_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            }

            case R.id.action_export_tx_list: {
                if (bound) {

                    try {
                        File filesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

                        for (int i=1; true; i++) {
                            String filename = String.format("tx-list-%d.%s", i, TxListFile.EXTENSION);
                            File file = new File(filesDir, filename);

                            /*if (!file.canWrite()) {
                                throw new IOException("Can't write file " + file.getAbsolutePath());
                            }*/

                            if (!file.exists()) {
                                file.createNewFile();
                                FileOutputStream outputStream = new FileOutputStream(file);
                                TxListFile.write(outputStream, canReaderService.getTransmitFrames());

                                String message = String.format(getString(R.string.message_file_saved_to_documents), file.getName());

                                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                                toast.show();
                                break;
                            }
                        }



                    } catch (ConfigurationException | IOException e) {
                        e.printStackTrace();

                        Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                break;
            }

            case R.id.action_share_tx_list: {

                if (bound) {

                    try {
                        File privateRootDir = getCacheDir();
                        File txlDir = new File(privateRootDir, "txl");
                        if (!txlDir.exists()) {
                            txlDir.mkdirs();
                        }

                        File file = new File(txlDir, "tx-share." + TxListFile.EXTENSION);

                        file.createNewFile();


                        if (!file.canWrite()) {
                            throw new IOException("Can't write file " + file.getAbsolutePath());
                        }

                        FileOutputStream outputStream = new FileOutputStream(file);

                        TxListFile.write(outputStream, canReaderService.getTransmitFrames());

                        Uri fileUri = FileProvider.getUriForFile(
                                MainActivity.this,
                                "com.autowp.canreader.txlfileprovider",
                                file);

                        Intent shareIntent = new Intent();
                        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.setType("*/*");
                        startActivity(Intent.createChooser(shareIntent, "Share Tx list"));

                    } catch (ConfigurationException | IOException e) {
                        e.printStackTrace();

                        Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                break;
            }

            case R.id.action_import_tx_list: {

                if (bound) {
                    Intent intent = new Intent(this, TxlPickerActivity.class);
                    startActivityForResult(intent, ACTION_PICK_TXL_TO_SHARE);
                }

                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {return;}

        if (requestCode == ACTION_PICK_TXL_TO_SHARE) {

            if (resultCode == Activity.RESULT_OK) {

                try {

                    String filename = data.getStringExtra(TxlPickerActivity.EXTRA_FILENAME);

                    File file = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS), filename);

                    FileInputStream inputStream = new FileInputStream(file);

                    List<TransmitCanFrame> list = TxListFile.read(inputStream);

                    inputStream.close();

                    canReaderService.setTransmitFrames(list);

                } catch (IOException | ConfigurationException e) {
                    e.printStackTrace();

                    Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Intent intent = new Intent(this, CanReaderService.class);

        startService(intent);
        bindService(intent, serviceConnection, AppCompatActivity.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
}
