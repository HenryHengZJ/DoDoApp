package com.dodomaker.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.VideoView;

public class PreviewActivity extends AppCompatActivity implements OnPreparedListener {

    private VideoView videoView;
    private TextView mtitle;
    private static final String FILEPATH = "filepath";
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        setTitle("Save and Share");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent extraIntent = getIntent();
        path = "";
        String title = "";

        if (extraIntent != null) {
            path = extraIntent.getStringExtra(FILEPATH);
            title = extraIntent.getStringExtra("title");
        }

        videoView = (VideoView) findViewById(R.id.videoView);
        mtitle = (TextView) findViewById(R.id.title);
        Button mbutton = (Button) findViewById(R.id.button);
        Button mbuttonShare = (Button) findViewById(R.id.buttonShare);
        ImageButton mreplyBtn = (ImageButton) findViewById(R.id.replayBtn);

        mtitle.setText("#" + title);
        videoView.setOnPreparedListener(PreviewActivity.this);
        videoView.setVideoURI(Uri.parse(path));

        mbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(PreviewActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });

        mbuttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareVideo("Meme Video", path);
            }
        });

        videoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion() {
                mreplyBtn.setVisibility(View.VISIBLE);
                mreplyBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mreplyBtn.setVisibility(View.GONE);
                        videoView.restart();
                    }
                });
            }
        });
    }

    public void shareVideo(final String title, String path) {

        MediaScannerConnection.scanFile(PreviewActivity.this, new String[] { path },

                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Intent shareIntent = new Intent(
                                android.content.Intent.ACTION_SEND);
                        shareIntent.setType("video/*");
                        shareIntent.putExtra(
                                android.content.Intent.EXTRA_SUBJECT, title);
                        shareIntent.putExtra(
                                android.content.Intent.EXTRA_TITLE, title);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(Intent.createChooser(shareIntent, "Share Video"));

                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    public void onPrepared() {
        //Starts the video playback as soon as it is ready
        videoView.start();
    }

}
