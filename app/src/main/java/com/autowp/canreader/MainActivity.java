package com.autowp.canreader;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
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

public class MainActivity extends ServiceConnectedActivity {
    private static final int ACTION_PICK_TXL_TO_SHARE = 1;

    private List<TransmitCanFrame> mTxListToLoad = null;

    private CanReaderService.OnTransmitChangeListener mOnTransmitChangeListener = new CanReaderService.OnTransmitChangeListener() {

        @Override
        public void handleTransmitUpdated() {
            System.out.println("handleTransmitUpdated");
            //supportInvalidateOptionsMenu(); //TODO: invalidate only if add/remove
            invalidateOptionsMenu();
        }

        @Override
        public void handleTransmitUpdated(TransmitCanFrame frame) {
        }

        @Override
        public void handleSpeedChanged(double speed) {
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        boolean hasMessages = bound && (canReaderService.getTransmitFrames().size() > 0);

        /*Button buttonStartAll = (Button) getView().findViewById(R.id.buttonStartAll);
        buttonStartAll.setEnabled(isConnected && canReaderService.hasStoppedTransmits());

        Button buttonStopAll = (Button) getView().findViewById(R.id.buttonStopAll);
        buttonStopAll.setEnabled(isConnected && canReaderService.hasStartedTransmits());
*/

        menu.findItem(R.id.action_export_tx_list).setEnabled(hasMessages);
        menu.findItem(R.id.action_share_tx_list).setEnabled(hasMessages);


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
    protected void afterConnect() {
        if (mTxListToLoad != null) {
            canReaderService.setTransmitFrames(mTxListToLoad);
            mTxListToLoad = null;
        }

        canReaderService.addListener(mOnTransmitChangeListener);
    }

    @Override
    protected void beforeDisconnect() {
        canReaderService.removeListener(mOnTransmitChangeListener);
    }
}
