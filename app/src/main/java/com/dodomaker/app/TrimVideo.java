package com.dodomaker.app;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import life.knowledge4.videotrimmer.K4LVideoTrimmer;
import life.knowledge4.videotrimmer.interfaces.OnTrimVideoListener;

public class TrimVideo extends AppCompatActivity implements OnTrimVideoListener {

    private K4LVideoTrimmer mVideoTrimmer;
    private FFmpeg ffmpeg;
    private Dialog dialog;
    private ProgressBar mProgressBar;
    private TextView mprogressTxtView, mloadingTxtView;

    private static final String TAG = "TrimVideo";
    final String EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH";
    private static final String FILEPATH = "filepath";
    private static final int EDIT_PICTURE = 100;

    private String title, combinedVideoFileString, templateFileString, path, editedPicFileString, thuglifeAudioFileString;
    private double audio_skipped_length;
    private int max_trim, startMs, endMs, editstyle;
    private long videoLengthInMillis;
    private Uri trimmed_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim_video);

        setTitle("Trim Video");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent extraIntent = getIntent();
        path = "";
        String trimlength = "";
        String editstyle_style = "";

        if (extraIntent != null) {
            path = extraIntent.getStringExtra(EXTRA_VIDEO_PATH);
            title = extraIntent.getStringExtra("title");
            editstyle_style = extraIntent.getStringExtra("editstyle");
            trimlength = extraIntent.getStringExtra("trimlength");
        }

        mVideoTrimmer = ((K4LVideoTrimmer) findViewById(R.id.timeLine));
        if (mVideoTrimmer != null && path != null && trimlength != null && editstyle_style != null) {
            editstyle = Integer.valueOf(editstyle_style);
            max_trim = Integer.valueOf(trimlength);
            if (max_trim != 0) {
                mVideoTrimmer.setMaxDuration(max_trim);
            }
            else {
                mVideoTrimmer.setMaxDuration(86400);
            }

            mVideoTrimmer.setOnTrimVideoListener(this);
            //mVideoTrimmer.setDestinationPath("/storage/emulated/0/DCIM/CameraCustom/");
            mVideoTrimmer.setVideoURI(Uri.parse(path));
        }

        loadFFMpegBinary();

    }


    private void executeCombineVideoCommand() {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );

        String filePrefix = title + "_VID_";
        String fileExtn = ".mp4";
        String trimmedRealPath = path;
        String templateRealPath = templateFileString;

        Log.d(TAG, "trimmedRealPath : " + trimmedRealPath);
        Log.d(TAG, "templateRealPath : " + templateRealPath);

        int fileNo = 0;
        File dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }
        
        combinedVideoFileString = dest.getAbsolutePath();
        videoLengthInMillis = getTemplateVideoLength() + (endMs - startMs);
     
        /*String[] complexCommand = {
                "-i",
                trimmedRealPath,
                "-i",
                templateRealPath,
                "-strict",
                "experimental",
                "-filter_complex",
                "[0:v]scale=iw*min(1920/iw\\,1080/ih):ih*min(1920/iw\\,1080/ih), pad=1920:1080:(1920-iw*min(1920/iw\\,1080/ih))/2:(1080-ih*min(1920/iw\\,1080/ih))/2,setsar=1:1[v0];[1:v] scale=iw*min(1920/iw\\,1080/ih):ih*min(1920/iw\\,1080/ih), pad=1920:1080:(1920-iw*min(1920/iw\\,1080/ih))/2:(1080-ih*min(1920/iw\\,1080/ih))/2,setsar=1:1[v1];[v0][0:a][v1][1:a] concat=n=2:v=1:a=1",
                "-ab", "48000", "-ac", "2", "-ar", "22050", "-s", "1920x1080", "-vcodec", "libx264", "-crf", "27", "-q", "4", "-preset", "ultrafast",
                combinedVideoFileString };*/

        String[] complexCommand = {
                "-ss",
                formatTimeString(startMs),
                "-t",
                formatTimeString(endMs - startMs),
                "-i",
                trimmedRealPath,
                "-i",
                templateRealPath,
                "-filter_complex",
                "[0:v]scale=iw*min(854/iw\\,480/ih):ih*min(854/iw\\,480/ih), pad=854:480:(854-iw*min(854/iw\\,480/ih))/2:(480-ih*min(854/iw\\,480/ih))/2,setsar=1:1[v0];[1:v]scale=854x480,setsar=1:1[v1];[v0][0:a][v1][1:a] concat=n=2:v=1:a=1",
                // "[0:v]scale=854x480,setsar=1:1[v0];[1:v]scale=iw*min(854/iw\\,480/ih):ih*min(854/iw\\,480/ih), pad=854:480:(854-iw*min(854/iw\\,480/ih))/2:(480-ih*min(854/iw\\,480/ih))/2,setsar=1:1[v1];[v0][0:a][v1][1:a] concat=n=2:v=1:a=1",
                "-ab", "48000", "-ac", "2", "-ar", "22050", "-s", "854x480", "-vcodec", "libx264", "-crf", "27", "-preset", "ultrafast",
                combinedVideoFileString };


        Log.d(TAG, "execFFmpegBinary " );
        execFFmpegBinary(complexCommand, combinedVideoFileString);
    }

    private void executeCombineVideoAndMergeAudioCommand() {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );
        File audioFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + title + ".mp3");

        Uri audioUri = Uri.parse(audioFile.toString());
        String audioPath = audioUri.toString();

        String filePrefix = title + "_VID_";
        String fileExtn = ".mp4";
        String trimmedRealPath = path;
        String templateRealPath = templateFileString;

        Log.d(TAG, "trimmedRealPath : " + trimmedRealPath);
        Log.d(TAG, "templateRealPath : " + templateRealPath);

        int fileNo = 0;
        File dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }

        combinedVideoFileString = dest.getAbsolutePath();
        videoLengthInMillis = getTemplateVideoLength() + (endMs - startMs);

       /* String[] complexCommand = {
                "-y",
                "-i",
                trimmedRealPath,
                "-i",
                templateRealPath,
                "-strict",
                "experimental",
                "-filter_complex",
                "[0:v]scale=iw*min(1920/iw\\,1080/ih):ih*min(1920/iw\\,1080/ih), pad=1920:1080:(1920-iw*min(1920/iw\\,1080/ih))/2:(1080-ih*min(1920/iw\\,1080/ih))/2,setsar=1:1[v0];[1:v] scale=iw*min(1920/iw\\,1080/ih):ih*min(1920/iw\\,1080/ih), pad=1920:1080:(1920-iw*min(1920/iw\\,1080/ih))/2:(1080-ih*min(1920/iw\\,1080/ih))/2,setsar=1:1[v1];[v0][0:a][v1][1:a] concat=n=2:v=1:a=1",
                "-ab", "48000", "-ac", "2", "-ar", "22050", "-s", "1920x1080", "-vcodec", "libx264", "-crf", "27", "-q", "4", "-preset", "ultrafast",
                combinedVideoFileString };*/

        String[] complexCommand = {
                "-ss",
                formatTimeString(startMs),
                "-t",
                formatTimeString(endMs - startMs),
                "-i",
                trimmedRealPath,
                "-i",
                templateRealPath,
                "-ss",
                String.valueOf(audio_skipped_length),
                "-i",
                audioPath,
                "-filter_complex",
                "[0:v]scale=iw*min(854/iw\\,480/ih):ih*min(854/iw\\,480/ih), pad=854:480:(854-iw*min(854/iw\\,480/ih))/2:(480-ih*min(854/iw\\,480/ih))/2,setsar=1:1[v0];[1:v]scale=854x480,setsar=1:1[v1];[v0][v1] concat=n=2:v=1:a=0 [out]",
                // "[0:v]scale=854x480,setsar=1:1[v0];[1:v]scale=iw*min(854/iw\\,480/ih):ih*min(854/iw\\,480/ih), pad=854:480:(854-iw*min(854/iw\\,480/ih))/2:(480-ih*min(854/iw\\,480/ih))/2,setsar=1:1[v1];[v0][0:a][v1][1:a] concat=n=2:v=1:a=1",
                "-map", "[out]",
                "-map", "2:a:0",
                "-c:v", "copy", "-c:a", "aac", "-shortest",
                "-ab", "48000", "-ac", "2", "-ar", "22050", "-s", "854x480", "-vcodec", "libx264", "-crf", "27", "-preset", "ultrafast",
                combinedVideoFileString };


        Log.d(TAG, "execFFmpegBinary " );
        execFFmpegBinary(complexCommand, combinedVideoFileString);
    }

    private void executeThugLifeVideoCommand() {
        File moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
        );


        File audioFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + "ThugLifeSong" + ".mp3");
        String audioRealPath = audioFile.toString();
        String filePrefix = title + "_VID_";
        String fileExtn = ".mp4";
        String trimmedRealPath = path;

        Log.d(TAG, "trimmedRealPath : " + trimmedRealPath);

        int fileNo = 0;
        File dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        while (dest.exists()) {
            fileNo++;
            dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
        }

        combinedVideoFileString = dest.getAbsolutePath();
        videoLengthInMillis = getTemplateVideoLength() + (endMs - startMs);

        String[] complexCommand = {
                "-ss",
                formatTimeString(startMs),
                "-t",
                formatTimeString(endMs - startMs),
                "-i",
                trimmedRealPath,
                "-loop", "1", "-framerate", "24", "-t", "5",
                "-i",
                editedPicFileString,
                "-t",
                "5",
                "-i",
                audioRealPath,
                "-filter_complex",
               // "[0:v]scale=iw*min(854/iw\\,480/ih):ih*min(854/iw\\,480/ih), pad=854:480:(854-iw*min(854/iw\\,480/ih))/2:(480-ih*min(854/iw\\,480/ih))/2,setsar=1:1[v0];[1:v]scale=854x480,setsar=1:1[v1];[v0][v1] concat=n=2:v=1:a=0 [out]",
                "[0:v]scale=iw*min(1920/iw\\,1080/ih):ih*min(1920/iw\\,1080/ih), pad=1920:1080:(1920-iw*min(1920/iw\\,1080/ih))/2:(1080-ih*min(1920/iw\\,1080/ih))/2,setsar=1:1[v0];[1:v] scale=iw*min(1920/iw\\,1080/ih):ih*min(1920/iw\\,1080/ih), pad=1920:1080:(1920-iw*min(1920/iw\\,1080/ih))/2:(1080-ih*min(1920/iw\\,1080/ih))/2,setsar=1:1[v1];[v0][0:a][v1][2:a] concat=n=2:v=1:a=1",
                //"-c:v", "copy", "-c:a", "aac", "-shortest",
                "-ab", "48000", "-ac", "2", "-ar", "22050", "-s", "854x480", "-vcodec", "libx264", "-crf", "27", "-preset", "ultrafast",
                combinedVideoFileString };


        Log.d(TAG, "execFFmpegBinary " );
        execFFmpegBinary(complexCommand, combinedVideoFileString);
    }

    /**
     * Executing ffmpeg binary
     */
    private void execFFmpegBinary(final String[] command, String filePath) {
        try {

            Pattern durationPattern = Pattern.compile("Duration: ([\\d\\w:]{8}[\\w.][\\d]+)");
            Pattern timePattern = Pattern.compile("time=([\\d\\w:]{8}[\\w.][\\d]+)");

            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : " + s);
                    dialog.dismiss();
                    Toast.makeText(TrimVideo.this, "Error combining videos", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output : " + s);
                    dialog.dismiss();

                    Intent intent = new Intent(TrimVideo.this, PreviewActivity.class);
                    intent.putExtra(FILEPATH, filePath);
                    intent.putExtra("title", title);
                    startActivity(intent);

                }

                @Override
                public void onProgress(String message) {

                    if (message.contains("speed")) {
                        long currentTime = getTimeInMillis(message, timePattern);
                        long percent = 100 * currentTime / videoLengthInMillis;
                        Log.d(TAG, "currentTime -> " + currentTime + "s % -> " + percent);
                        if (percent < 100) {
                            mProgressBar.setProgress((int) percent);
                            mprogressTxtView.setText(String.valueOf((int) percent));
                        }
                    }
                    Log.d(TAG, "onProgress : videoLengthInMillis " + videoLengthInMillis);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    showProgressDialog();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg " + command);
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    private Long getTemplateVideoLength() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(TrimVideo.this, Uri.parse(templateFileString));
        return Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
    }

    private Long getTimeInMillis(String message, Pattern pattern){
        Matcher matcher = pattern.matcher(message);
        matcher.find();
        String time = String.valueOf(matcher.group(1));
        String[] arrayTime = time.split("[:.]");
        Log.d(TAG, "time -> " + time);
        return TimeUnit.HOURS.toMillis(Long.parseLong(arrayTime[0]))
                + TimeUnit.MINUTES.toMillis(Long.parseLong(arrayTime[1]))
                + TimeUnit.SECONDS.toMillis(Long.parseLong(arrayTime[2]))
                + Long.parseLong(arrayTime[3]);
    }

    private void showProgressDialog() {
        dialog = new Dialog(TrimVideo.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.progress_percentage_dialog);

        mProgressBar = (ProgressBar) dialog.findViewById(R.id.progress_horizontal);
        mprogressTxtView = dialog.findViewById(R.id.progressTxtView);
        mloadingTxtView = (TextView) dialog.findViewById(R.id.loadingTxtView);

        mloadingTxtView.setText("Saving file");

        mProgressBar.setProgress(0);
        mprogressTxtView.setText(String.valueOf(0));

        dialog.show();

        Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawableResource(android.R.color.transparent);
    }

    /*private void deleteTrimmedFiles() {
        File trimmedFile =  new File(trimmed_uri.getPath());
        Log.d(TAG, "deleteTrimmedFiles = " + trimmedFile);
        Log.d(TAG, "deleteTrimmedFiles exists = " + trimmedFile.exists());
        if(trimmedFile.exists())
            trimmedFile.delete();
        Log.d(TAG, "deleteTrimmedFiles delete = " + trimmedFile.delete());
    }*/

    private void loadFFMpegBinary() {
        try {
            if (ffmpeg == null) {
                Log.d(TAG, "ffmpeg : era nulo");
                ffmpeg = FFmpeg.getInstance(this);
            }
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "ffmpeg : correct Loaded");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        } catch (Exception e) {
            Log.d(TAG, "EXception no controlada : " + e);
        }
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(TrimVideo.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Not Supported")
                .setMessage("Device Not Supported")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TrimVideo.this.finish();
                    }
                })
                .create()
                .show();

    }

    @Override
    public void onTrimStarted() {

    }

    @Override
    public void getResult(final Uri uri) {

        /*trimmed_uri = uri;

        File templateFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + title + ".mp4");
        templateFileString = templateFile.toString();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //executeCombineVideoCommand();
            }
        });*/

    }

    private String formatTimeString(int milliseconds) {

        long millis = milliseconds % 1000;
        long second = (milliseconds / 1000) % 60;
        long minute = (milliseconds / (1000 * 60)) % 60;
        long hour = (milliseconds / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d.%d", hour, minute, second, millis);
    }


    @Override
    public void onSave(int start_time, int end_time) {

        startMs = start_time;
        endMs = end_time;

        int intervalMilliSeconds = endMs - startMs;
        double second = (double) intervalMilliSeconds/ (double) 1000;
        Log.d(TAG, "second : " + second);

        audio_skipped_length = max_trim == 0 ? 0.0 : (double) max_trim - second < 0 ? 0.0 : (double) max_trim - second;

        File templateFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + title + ".mp4");
        templateFileString = templateFile.toString();
        Uri templateUri = Uri.parse(templateFile.toString());
        Log.d(TAG, "templateUri : " + templateUri);
        Log.d(TAG, "audio_skipped_length : " + audio_skipped_length);

        if (editstyle == 2) {
         //   mVideoTrimmer.destroy();
            Intent intent;
            intent = new Intent(TrimVideo.this, EditPicture.class);
            intent.putExtra(EXTRA_VIDEO_PATH, path);
            intent.putExtra("startMs", String.valueOf(startMs));
            intent.putExtra("endMs", String.valueOf(endMs));
            startActivityForResult(intent, EDIT_PICTURE);
        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (editstyle == 0) {
                        executeCombineVideoAndMergeAudioCommand();
                    }
                    else if (editstyle == 1) {
                        executeCombineVideoCommand();
                    }
                }
            });
        }
    }


    @Override
    public void cancelAction() {
        mVideoTrimmer.destroy();
        finish();
    }

    @Override
    public void onError(String message) {

    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == EDIT_PICTURE) {
                editedPicFileString = data.getStringExtra("editedpic");
                thuglifeAudioFileString = "android.resource://" + getPackageName() + "/" + R.raw.thuglifesong;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        executeThugLifeVideoCommand();
                    }
                });
            }
        }
    }

}
