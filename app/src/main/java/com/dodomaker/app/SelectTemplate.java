package com.dodomaker.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.dodomaker.app.CustomAdapter.TemplateAdapter;
import com.dodomaker.app.CustomObjectClass.Template;
import com.dodomaker.app.Helper.TinyDB;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SelectTemplate extends AppCompatActivity implements TemplateAdapter.ItemClickListener, OnPreparedListener {


    private RecyclerView mTemplateList;
    private LinearLayoutManager mLayoutManager;
    private TextView mtitle;
    private VideoView mvideoView;
    private ImageButton mcloseBtn;
    private View sheetView;
    private List<Template> templatelist;
    private TemplateAdapter adapter;
    private BottomSheetDialog mBottomSheetDialog;
    private RelativeLayout mdownloading_button_layout;
    private LinearLayout muse_or_download_button_layout;
    private ImageButton mdelete_button;
    private ProgressBar mprogress_bar;
    private Button mUseTemplateBtn;

    private static final int REQUEST_TAKE_GALLERY_VIDEO = 100;
    private static final String TAG = "SelectTemplate";
    final String EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH";
    private String selectedTitle = "";
    private int selectedTrimLength = 0, selectedEditStyle;
    private Integer selectedPosition;
    private String videoPathFromMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_template);

        setTitle("Choose Template");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTemplateList = (RecyclerView) findViewById(R.id.recycler_view);
        mTemplateList.setNestedScrollingEnabled(false);

        mLayoutManager = new LinearLayoutManager(this);
        templatelist = new ArrayList<Template>();
        mTemplateList.setLayoutManager(mLayoutManager);
        adapter = new TemplateAdapter(templatelist,SelectTemplate.this);
        adapter.setClickListener(this);
        mTemplateList.setAdapter(adapter);

        getTemplate();

        Intent extraIntent = getIntent();
        if (extraIntent != null) {
            videoPathFromMainActivity = extraIntent.getStringExtra(EXTRA_VIDEO_PATH);
        }

        if (videoPathFromMainActivity == null) {
            videoPathFromMainActivity = "";
        }

    }

    private void setupBottomSheetDialog(int position) {

        selectedPosition = position;
        selectedTitle = templatelist.get(position).gettitle();
        selectedTrimLength = templatelist.get(position).gettrimlength();
        selectedEditStyle = templatelist.get(position).geteditstyle();

        mBottomSheetDialog = new BottomSheetDialog(SelectTemplate.this);
        sheetView = getLayoutInflater().inflate(R.layout.pop_view, null);
        mBottomSheetDialog.setContentView(sheetView);

        mtitle = (TextView) sheetView.findViewById(R.id.title);
        mvideoView = (VideoView) sheetView.findViewById(R.id.videoView);
        mcloseBtn = (ImageButton) sheetView.findViewById(R.id.closeBtn);
        ImageButton mreplyBtn = (ImageButton) sheetView.findViewById(R.id.replayBtn);
        mdownloading_button_layout =  (RelativeLayout) sheetView.findViewById(R.id.downloading_button);
        muse_or_download_button_layout =  (LinearLayout) sheetView.findViewById(R.id.use_or_download_button_layout);
        mdelete_button =  (ImageButton) sheetView.findViewById(R.id.delete_button);
        mprogress_bar = (ProgressBar) sheetView.findViewById(R.id.progress_bar);
        mUseTemplateBtn = (Button) sheetView.findViewById(R.id.button);

        //If template is NOT downloading, show Download Template or Use Template
        if (!getDownloadPosition(SelectTemplate.this, position)) {
            muse_or_download_button_layout.setVisibility(View.VISIBLE);
            mdownloading_button_layout.setVisibility(View.GONE);
            //If template doenst have audio to merge, just check if video file exist
            if (templatelist.get(position).getsonglink().isEmpty()) {
                checkForVideoExist(position);
            }
            else {
                checkForVideoAndAudioExist(position);
            }
        }
        //If template IS downloading, show progress bar
        else {
            muse_or_download_button_layout.setVisibility(View.GONE);
            mdownloading_button_layout.setVisibility(View.VISIBLE);
            mprogress_bar.setIndeterminate(true);
        }

        mcloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
            }
        });

        mdelete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteTemplateDialog();
            }
        });

        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mvideoView.stopPlayback();
            }
        });

        mvideoView.setOnPreparedListener(SelectTemplate.this);
        mvideoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion() {
                mreplyBtn.setVisibility(View.VISIBLE);
                mreplyBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mreplyBtn.setVisibility(View.GONE);
                        mvideoView.restart();
                    }
                });
            }
        });

        mtitle.setText("#" + templatelist.get(position).gettitle());
        mBottomSheetDialog.show();
    }

    private void showDeleteTemplateDialog() {
        new AlertDialog.Builder(SelectTemplate.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Delete Template")
                .setMessage("Are you sure you want to delete this template?")
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTemplate();
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();

    }

    private void checkForVideoAndAudioExist(int position) {

        File videoFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + templatelist.get(position).gettitle() + ".mp4");
        File audioFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + templatelist.get(position).gettitle() + ".mp3");

        if (videoFile.exists() && audioFile.exists()) {
            mUseTemplateBtn.setText("Use Template");
            mdelete_button.setVisibility(View.VISIBLE);
            mvideoView.setVideoURI(Uri.parse(videoFile.toString()));
        }
        else {
            mUseTemplateBtn.setText("Download Template");
            mdelete_button.setVisibility(View.GONE);
            mvideoView.setVideoURI(Uri.parse(templatelist.get(position).getpathlink()));
        }

        mUseTemplateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (videoFile.exists() && audioFile.exists()) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        getPermission(100, position);
                    }
                    else {
                        mBottomSheetDialog.dismiss();
                        if (!videoPathFromMainActivity.isEmpty()) {
                            Intent intent;
                            intent = new Intent(SelectTemplate.this, TrimVideo.class);
                            intent.putExtra(EXTRA_VIDEO_PATH, videoPathFromMainActivity);
                            intent.putExtra("title", selectedTitle);
                            intent.putExtra("editstyle", String.valueOf(selectedEditStyle));
                            intent.putExtra("trimlength", String.valueOf(selectedTrimLength));
                            startActivity(intent);
                        }
                        else {
                            uploadVideo();
                        }
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= 23) {
                        getPermission(200, position);
                    }
                    else {
                        muse_or_download_button_layout.setVisibility(View.GONE);
                        mdownloading_button_layout.setVisibility(View.VISIBLE);
                        mprogress_bar.setIndeterminate(true);

                        if (!audioFile.exists()) {
                            if (templatelist.get(position).getsonglink().isEmpty()) {
                                downloadAudio(position, templatelist.get(position).getsonglink(), templatelist.get(position).gettitle());
                            }
                        }
                        if (!videoFile.exists()) {
                            downloadVideo(position, templatelist.get(position).getpathlink(), templatelist.get(position).gettitle());
                        }
                    }
                }
            }
        });
    }

    private void checkForVideoExist(int position) {

        File videoFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + templatelist.get(position).gettitle() + ".mp4");

        if (videoFile.exists()) {
            mUseTemplateBtn.setText("Use Template");
            mdelete_button.setVisibility(View.VISIBLE);
            mvideoView.setVideoURI(Uri.parse(videoFile.toString()));
        }
        else {
            mUseTemplateBtn.setText("Download Template");
            mdelete_button.setVisibility(View.GONE);
            mvideoView.setVideoURI(Uri.parse(templatelist.get(position).getpathlink()));
        }

        mUseTemplateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (videoFile.exists()) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        getPermission(100, position);
                    }
                    else {
                        mBottomSheetDialog.dismiss();
                        if (!videoPathFromMainActivity.isEmpty()) {
                            Intent intent;
                            intent = new Intent(SelectTemplate.this, TrimVideo.class);
                            intent.putExtra(EXTRA_VIDEO_PATH, videoPathFromMainActivity);
                            intent.putExtra("title", selectedTitle);
                            intent.putExtra("editstyle", String.valueOf(selectedEditStyle));
                            intent.putExtra("trimlength", String.valueOf(selectedTrimLength));
                            startActivity(intent);
                        }
                        else {
                            uploadVideo();
                        }
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= 23) {
                        getPermission(200, position);
                    }
                    else {
                        muse_or_download_button_layout.setVisibility(View.GONE);
                        mdownloading_button_layout.setVisibility(View.VISIBLE);
                        mprogress_bar.setIndeterminate(true);
                        downloadVideo(position, templatelist.get(position).getpathlink(), templatelist.get(position).gettitle());
                    }
                }
            }
        });
    }

    private boolean getDownloadPosition(Context mContext, int position)
    {
        Log.d(TAG, "getDownloadPosition = " + position);
        TinyDB tinyDB = new TinyDB(mContext);
        return tinyDB.getBoolean("downloadPosition" + position);
    }

    private void saveDownloadPosition(Context mContext, int position)
    {
        Log.d(TAG, "saveDownloadPosition = " + position);
        TinyDB tinyDB = new TinyDB(mContext);
        tinyDB.putBoolean("downloadPosition" + position, true);
    }

    private void removeDownloadPosition(Context mContext, int position)
    {
        Log.d(TAG, "removeDownloadPosition = " + position);
        TinyDB tinyDB = new TinyDB(mContext);
        tinyDB.remove("downloadPosition" + position);
    }

    private void deleteTemplate() {
        File templateVideoFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + selectedTitle + ".mp4");
        File templateAudioFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + selectedTitle + ".mp3");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "templateVideoFile = " + templateVideoFile);
                Log.d(TAG, "templateVideoFile exists = " + templateVideoFile.exists());
                if(templateVideoFile.exists())
                    templateVideoFile.delete();
                Log.d(TAG, "templateVideoFile delete = " + templateVideoFile.delete());

                Log.d(TAG, "templateAudioFile = " + templateAudioFile);
                Log.d(TAG, "templateAudioFile exists = " + templateAudioFile.exists());
                if(templateAudioFile.exists())
                    templateAudioFile.delete();
                Log.d(TAG, "templateAudioFile delete = " + templateAudioFile.delete());

                if (mBottomSheetDialog.isShowing()) {
                    mBottomSheetDialog.dismiss();
                    setupBottomSheetDialog(selectedPosition);
                }
                Toast.makeText(SelectTemplate.this, "#" + selectedTitle + " Template deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void downloadVideo(int position, String videolink, String title) {
        Toast.makeText(SelectTemplate.this, "Downloading File", Toast.LENGTH_LONG).show();

        saveDownloadPosition(SelectTemplate.this, position);

        final DownloadFileFromURL downloadTask = new DownloadFileFromURL(SelectTemplate.this, ".mp4", title, position);
        downloadTask.execute(videolink);

    }


    private void downloadAudio(int position, String songlink, String title) {

        final DownloadFileFromURL downloadTask = new DownloadFileFromURL(SelectTemplate.this, ".mp3", title, position);
        downloadTask.execute(songlink);

    }

    private void getPermission(int code, int position) {
        String[] params = null;
        String writeExternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String readExternalStorage = Manifest.permission.READ_EXTERNAL_STORAGE;

        int hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(this, writeExternalStorage);
        int hasReadExternalStoragePermission = ActivityCompat.checkSelfPermission(this, readExternalStorage);
        List<String> permissions = new ArrayList<String>();

        if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
            permissions.add(writeExternalStorage);
        if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
            permissions.add(readExternalStorage);

        if (!permissions.isEmpty()) {
            params = permissions.toArray(new String[permissions.size()]);
        }
        if (params != null && params.length > 0) {
            ActivityCompat.requestPermissions(SelectTemplate.this,
                    params,
                    code);
        } else
            if (code == 100) {
                if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing())
                    mBottomSheetDialog.dismiss();
                if (!videoPathFromMainActivity.isEmpty()) {
                    Intent intent;
                    intent = new Intent(SelectTemplate.this, TrimVideo.class);
                    intent.putExtra(EXTRA_VIDEO_PATH, videoPathFromMainActivity);
                    intent.putExtra("title", selectedTitle);
                    intent.putExtra("editstyle", String.valueOf(selectedEditStyle));
                    intent.putExtra("trimlength", String.valueOf(selectedTrimLength));
                    startActivity(intent);
                }
                else {
                    uploadVideo();
                }
            }
            else {
                Log.d(TAG, "PERMISSION HERE = " + code);
                if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
                    muse_or_download_button_layout.setVisibility(View.GONE);
                    mdownloading_button_layout.setVisibility(View.VISIBLE);
                    mprogress_bar.setIndeterminate(true);
                }
                if (templatelist.get(position).getsonglink().isEmpty()) {
                    downloadVideo(position, templatelist.get(position).getpathlink(), templatelist.get(position).gettitle());
                }
                else {
                    downloadAudio(position, templatelist.get(position).getsonglink(), templatelist.get(position).gettitle());
                    downloadVideo(position, templatelist.get(position).getpathlink(), templatelist.get(position).gettitle());
                }
            }
    }

    private void getTemplate() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("templates")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                Template talents = new Template();
                                talents.settitle(document.getId());
                                talents.setimage(document.getString("image"));
                                talents.setpathlink(document.getString("video"));
                                talents.setsonglink(document.getString("song"));
                                talents.settrimlength(document.getLong("trimlength").intValue());
                                talents.seteditstyle(document.getLong("editstyle").intValue());
                                templatelist.add(talents);
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void uploadVideo() {
        try {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        //Starts the video playback as soon as it is ready
        super.onDestroy();
        if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
            mBottomSheetDialog.dismiss();
        }
    }

    @Override
    public void onPause() {
        //Starts the video playback as soon as it is ready
        super.onPause();
        if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
            mBottomSheetDialog.dismiss();
        }
    }

    @Override
    public void onPrepared() {
        //Starts the video playback as soon as it is ready
        mvideoView.start();
    }

    @Override
    public void onTemplateClick(View view, int position) {
        setupBottomSheetDialog(position);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing())
                        mBottomSheetDialog.dismiss();
                    if (!videoPathFromMainActivity.isEmpty()) {
                        Intent intent;
                        intent = new Intent(SelectTemplate.this, TrimVideo.class);
                        intent.putExtra(EXTRA_VIDEO_PATH, videoPathFromMainActivity);
                        intent.putExtra("title", selectedTitle);
                        intent.putExtra("editstyle", String.valueOf(selectedEditStyle));
                        intent.putExtra("trimlength", String.valueOf(selectedTrimLength));
                        startActivity(intent);
                    }
                    else {
                        uploadVideo();
                    }
                }
            }
            break;
            case 200: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "PERMISSION_GRANTED");
                    if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
                        muse_or_download_button_layout.setVisibility(View.GONE);
                        mdownloading_button_layout.setVisibility(View.VISIBLE);
                        mprogress_bar.setIndeterminate(true);
                    }
                    if (templatelist.get(selectedPosition).getsonglink().isEmpty()) {
                        downloadVideo(selectedPosition, templatelist.get(selectedPosition).getpathlink(), templatelist.get(selectedPosition).gettitle());
                    }
                    else {
                        downloadAudio(selectedPosition, templatelist.get(selectedPosition).getsonglink(), templatelist.get(selectedPosition).gettitle());
                        downloadVideo(selectedPosition, templatelist.get(selectedPosition).getpathlink(), templatelist.get(selectedPosition).gettitle());
                    }
                }
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                final Uri selectedUri = data.getData();
                Log.d(TAG, "selectedUri = " + selectedUri);
                try {
                    if (selectedUri != null) {
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(getPath(this, selectedUri));
                        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                        if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
                            mBottomSheetDialog.dismiss();
                        }
                        Intent intent;
                        intent = new Intent(SelectTemplate.this, TrimVideo.class);
                        intent.putExtra(EXTRA_VIDEO_PATH, getPath(this, selectedUri));
                        intent.putExtra("title", selectedTitle);
                        intent.putExtra("editstyle", String.valueOf(selectedEditStyle));
                        intent.putExtra("trimlength", String.valueOf(selectedTrimLength));
                        startActivity(intent);
                    }
                } catch (final Throwable e) {
                    Log.d(TAG, "Failed selectedUri = " + selectedUri);
                    Toast.makeText(SelectTemplate.this, "Invalid file", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     */
    private String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri.
     */
    private String getDataColumn(Context context, Uri uri, String selection,
                                 String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public class DownloadFileFromURL extends AsyncTask<String, String, String> {

        private Context context;
        private String fileExtn;
        private String filePrefix;
        private int position;

        public DownloadFileFromURL(Context context, String fileExtn, String filePrefix, int position) {
            this.context = context;
            this.fileExtn = fileExtn;
            this.filePrefix = filePrefix;
            this.position = position;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {

                java.net.URL url = new URL(f_url[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lengthOfFile = connection.getContentLength();

                String path = Environment.getExternalStorageDirectory() + "/DoDoApp/";
                // Create the parent path
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }


                File dest = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DoDoApp", filePrefix + fileExtn);

                // download the file
                input = new BufferedInputStream(url.openStream());
                output = new FileOutputStream(dest);

                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    if (lengthOfFile > 0)
                        publishProgress("" + (int) ((total * 100) / lengthOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

            } catch (Exception e) {
                Log.e("Exception: ", e.getMessage());
                return e.toString();
            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                    Log.e("IOException: ", ignored.getMessage());
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            Log.d(TAG, "onProgressUpdate = " + Integer.parseInt(progress[0]));
            Log.d(TAG, "selectedPosition = " + selectedPosition + " //////////// downloadPosition = "  + position);
            //pd.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "result = " + result);
            if (result != null) {
                if (!result.isEmpty()) {
                    removeDownloadPosition(context, position);
                    if (mBottomSheetDialog.isShowing()) {
                        mBottomSheetDialog.dismiss();
                    }
                    Toast.makeText(context, "#" + filePrefix + "Template download error: " + result, Toast.LENGTH_LONG).show();
                }
            }
            else {
                Log.d(TAG, "fileExtn = " + fileExtn);
                if (fileExtn.equals(".mp4")) {
                    removeDownloadPosition(context, position);
                    if (mBottomSheetDialog.isShowing()) {
                        mBottomSheetDialog.dismiss();
                        setupBottomSheetDialog(position);
                    }
                    Toast.makeText(context, "#" + filePrefix + " Template downloaded", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
