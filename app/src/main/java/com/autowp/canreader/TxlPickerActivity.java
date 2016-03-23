package com.autowp.canreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class TxlPickerActivity extends Activity {

    public static final String EXTRA_FILENAME = "Filename";
    // The path to the root of this app's internal storage
    private File mDir;
    // Array of files in the images subdirectory
    ArrayList<File> mFiles = new ArrayList<>();

    private ListView mFileListView;

    public static String getFileExt(String fileName) {
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex < 0) {
            return null;
        }
        return fileName.substring(lastIndex + 1, fileName.length());

    }

    private void fillFiles(String dir)
    {
        File filesDir = Environment.getExternalStoragePublicDirectory(dir);
        for (File file : filesDir.listFiles()) {
            String ext = getFileExt(file.getName());
            if (ext != null && TxListFile.EXTENSION.equalsIgnoreCase(ext)) {
                mFiles.add(file);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txl_picker);

        fillFiles(Environment.DIRECTORY_DOCUMENTS);
        fillFiles(Environment.DIRECTORY_DOWNLOADS);

        String[] mFilenames = new String[mFiles.size()];
        for (int i=0; i<mFiles.size(); i++) {
            File file = mFiles.get(i);
            mFilenames[i] = file.getParentFile().getName() + "/" + file.getName();
        }

        setResult(Activity.RESULT_CANCELED, null);

        /*
         * Display the file names in the ListView mFileListView.
         * Back the ListView with the array mFilenames, which
         * you can create by iterating through mFiles and
         * calling File.getAbsolutePath() for each File
         */

        mFileListView = (ListView)findViewById(R.id.file_picker_listview);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, mFilenames);

        mFileListView.setAdapter(adapter);

        mFileListView.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                @Override
                /*
                 * When a filename in the ListView is clicked, get its
                 * content URI and send it to the requesting app
                 */
                public void onItemClick(AdapterView<?> adapterView,
                                        View view,
                                        int position,
                                        long rowId) {

                    File requestFile = mFiles.get(position);

                    Intent intent =
                            new Intent("com.autowp.canreader.ACTION_RETURN_FILE");
                    intent.putExtra(EXTRA_FILENAME, requestFile.getName());

                    setResult(Activity.RESULT_OK, intent);

                    finish();
                }
            });
    }
}
