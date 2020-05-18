package com.dodomaker.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dodomaker.app.CustomAdapter.StickerAdapter;
import com.dodomaker.app.CustomObjectClass.Sticker;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.xiaopo.flying.sticker.BitmapStickerIcon;
import com.xiaopo.flying.sticker.DeleteIconEvent;
import com.xiaopo.flying.sticker.DrawableSticker;
import com.xiaopo.flying.sticker.FlipHorizontallyEvent;
import com.xiaopo.flying.sticker.StickerView;
import com.xiaopo.flying.sticker.TextSticker;
import com.xiaopo.flying.sticker.ZoomIconEvent;

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

import life.knowledge4.videotrimmer.view.TimeLineView;

import static life.knowledge4.videotrimmer.utils.TrimVideoUtils.stringForTime;

public class EditPicture extends AppCompatActivity {

    private GridView mstickerGridView;
    private LinearLayoutManager mLayoutManager;
    private List<Sticker> stickerlist;
    private StickerAdapter adapter;
    private StickerView stickerView;
    private TextSticker sticker;
    private TextView mtitle, mTextTime, mprogressTxtView;
    private ImageButton mcloseBtn;
    private Button mfilterBtn, mtextBtn, mglassesBtn, mcigaratteBtn, mchainsBtn, mbtSave, mbtCancel;
    private BottomSheetDialog mBottomSheetDialog;
    private ImageView mimage_view;
    private SeekBar mHolderTopView;
    private TimeLineView mTimeLineView;
    private Bitmap bitmap;
    private MediaMetadataRetriever retriever;
    private Dialog dialog;
    private ProgressBar mProgressBar;

    private static final String TAG = "EditPicture";
    public static final int PERM_RQST_CODE = 110;
    private int[] myGlassesArray = new int[]{R.drawable.glass_1, R.drawable.glass_2, R.drawable.glass_3, R.drawable.glass_4, R.drawable.glass_5, R.drawable.glass_6};
    private int[] myCigarattesArray = new int[]{R.drawable.cigaratte_1, R.drawable.cigaratte_2, R.drawable.cigaratte_3};
    private int[] myChainsArray = new int[]{R.drawable.chain_1, R.drawable.chain_2, R.drawable.chain_3, R.drawable.chain_4};
    private int[] myTextArray = new int[]{R.drawable.text_1, R.drawable.text_2, R.drawable.text_3, R.drawable.text_4};
    private int[] myFilterArray = new int[]{R.drawable.filter_1, R.drawable.filter_2, R.drawable.filter_3};
    private int clickedposition;
    private String path;
    private int startMs, endMs;

    final String EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH";
    final String thuflifeaudio = "https://memeclips.s3-ap-southeast-1.amazonaws.com/thuglifesong.mp3";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thug_life_edit);

        setTitle("Edit Picture");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Intent extraIntent = getIntent();
        path = "";
        String startMs_str = "";
        String endMs_str = "";
        String trimlength_str = "";

        if (extraIntent != null) {
            path = extraIntent.getStringExtra(EXTRA_VIDEO_PATH);
            startMs_str = extraIntent.getStringExtra("startMs");
            endMs_str = extraIntent.getStringExtra("endMs");
        }

        if (startMs_str != null && endMs_str != null && trimlength_str != null) {
            startMs = startMs_str.equals("") ? 0 : Integer.valueOf(startMs_str);
            endMs = endMs_str.equals("") ? 0 : Integer.valueOf(endMs_str);
        }
        Log.d(TAG, "path = " + path);
        Log.d(TAG, "startMs = " + startMs);
        Log.d(TAG, "endMs = " + endMs);

        stickerView = (StickerView) findViewById(R.id.sticker_view);
        mfilterBtn = (Button) findViewById(R.id.filterBtn);
        mtextBtn = (Button) findViewById(R.id.textBtn);
        mglassesBtn = (Button) findViewById(R.id.glassesBtn);
        mcigaratteBtn = (Button) findViewById(R.id.cigaratteBtn);
        mchainsBtn = (Button) findViewById(R.id.chainsBtn);
        mbtSave = (Button) findViewById(R.id.btSave);
        mbtCancel = (Button) findViewById(R.id.btCancel);
        mimage_view = findViewById(R.id.image_view);
        mHolderTopView = (SeekBar) findViewById(R.id.handlerTop);
        mTextTime = (TextView) findViewById(R.id.textTime);
        mTimeLineView = (TimeLineView) findViewById(R.id.timeLineView);

        //currently you can config your own icons and icon event
        //the event you can custom
        BitmapStickerIcon deleteIcon = new BitmapStickerIcon(ContextCompat.getDrawable(this,
                com.xiaopo.flying.sticker.R.drawable.sticker_ic_close_white_18dp),
                BitmapStickerIcon.LEFT_TOP);
        deleteIcon.setIconEvent(new DeleteIconEvent());

        BitmapStickerIcon zoomIcon = new BitmapStickerIcon(ContextCompat.getDrawable(this,
                com.xiaopo.flying.sticker.R.drawable.sticker_ic_scale_white_18dp),
                BitmapStickerIcon.RIGHT_BOTOM);
        zoomIcon.setIconEvent(new ZoomIconEvent());

        BitmapStickerIcon flipIcon = new BitmapStickerIcon(ContextCompat.getDrawable(this,
                com.xiaopo.flying.sticker.R.drawable.sticker_ic_flip_white_18dp),
                BitmapStickerIcon.RIGHT_TOP);
        flipIcon.setIconEvent(new FlipHorizontallyEvent());

        stickerView.setBackgroundColor(Color.WHITE);
        stickerView.setLocked(false);
        stickerView.setConstrained(true);

        /*sticker = new TextSticker(this);
        sticker.setDrawable(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.sticker_transparent_background));
        sticker.setText("Hello, world!");
        sticker.setTextColor(Color.BLACK);
        sticker.setTextAlign(Layout.Alignment.ALIGN_CENTER);
        sticker.resizeText();*/

        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);


        mbtSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSave();
            }
        });

        mbtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancel();
            }
        });

        mfilterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedposition = 0;
                setupBottomSheetDialog(mfilterBtn.getText().toString());
            }
        });

        mtextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedposition = 1;
                setupBottomSheetDialog(mtextBtn.getText().toString());
            }
        });

        mglassesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedposition = 2;
                setupBottomSheetDialog(mglassesBtn.getText().toString());
            }
        });

        mcigaratteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedposition = 3;
                setupBottomSheetDialog(mcigaratteBtn.getText().toString());
            }
        });

        mchainsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedposition = 4;
                setupBottomSheetDialog(mchainsBtn.getText().toString());
            }
        });


        stickerView.setOnStickerOperationListener(new StickerView.OnStickerOperationListener() {
            @Override
            public void onStickerAdded(@NonNull com.xiaopo.flying.sticker.Sticker sticker) {
                Log.d(TAG, "onStickerAdded");
            }

            @Override
            public void onStickerClicked(@NonNull com.xiaopo.flying.sticker.Sticker sticker) {
                //stickerView.removeAllSticker();
                if (sticker instanceof TextSticker) {
                    ((TextSticker) sticker).setTextColor(Color.RED);
                    stickerView.replace(sticker);
                    stickerView.invalidate();
                }
                Log.d(TAG, "onStickerClicked");
            }

            @Override
            public void onStickerDeleted(@NonNull com.xiaopo.flying.sticker.Sticker sticker) {
                Log.d(TAG, "onStickerDeleted");
            }

            @Override
            public void onStickerDragFinished(@NonNull com.xiaopo.flying.sticker.Sticker sticker) {
                Log.d(TAG, "onStickerDragFinished");
            }

            @Override
            public void onStickerTouchedDown(@NonNull com.xiaopo.flying.sticker.Sticker sticker) {
                Log.d(TAG, "onStickerTouchedDown");
            }

            @Override
            public void onStickerZoomFinished(@NonNull com.xiaopo.flying.sticker.Sticker sticker) {
                Log.d(TAG, "onStickerZoomFinished");
            }

            @Override
            public void onStickerFlipped(@NonNull com.xiaopo.flying.sticker.Sticker sticker) {
                Log.d(TAG, "onStickerFlipped");
            }

            @Override
            public void onStickerDoubleTapped(@NonNull com.xiaopo.flying.sticker.Sticker sticker) {
                Log.d(TAG, "onDoubleTapped: double tap will be with two click");
            }
        });

        mHolderTopView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onPlayerIndicatorSeekChanged(progress, fromUser);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStart();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStop(seekBar);
            }
        });

        setUpMargins();
        setImage(endMs);
        setTimeVideo(endMs);
        setTimeLine(path, startMs, endMs);
        setHolderThumbPosition(endMs);

    }

    private void setImage(int frameMs) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bitmap = retriever.getFrameAtTime(frameMs*1000);
                Log.d(TAG, "bitmap = " + bitmap);
                mimage_view.setImageBitmap(bitmap);
            }
        });
      //  bitmap = retriever.getFrameAtTime(frameMs*1000);
       // Log.d(TAG, "bitmap = " + bitmap);
      //  mimage_view.setImageBitmap(bitmap);
    }


    private void onPlayerIndicatorSeekChanged(int progress, boolean fromUser) {
    }

    private void onPlayerIndicatorSeekStart() {

    }

    private void onPlayerIndicatorSeekStop(@NonNull SeekBar seekBar) {
        int mDuration = endMs - startMs;
        int frame_timeMs = (int) ((mDuration * seekBar.getProgress()) / 1000L);
        Log.d(TAG, "progress = " + ((int) ((mDuration * seekBar.getProgress()) / 1000L)));
        setImage(frame_timeMs);
        setTimeVideo(frame_timeMs);
    }

    private void setTimeVideo(int position) {
        String seconds = "";
        mTextTime.setText(String.format("%s %s", stringForTime(position), seconds));
    }

    private void setTimeLine(String path, int mStartPosition, int mEndPosition) {
        mTimeLineView.setVideo(Uri.parse(path), mStartPosition, mEndPosition);
    }

    private void setHolderThumbPosition(int mEndPosition) {
        mHolderTopView.setProgress(mEndPosition);
    }

    private void setUpMargins() {
        int marge = 75;
        int widthSeek = mHolderTopView.getThumb().getMinimumWidth() / 2;

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mHolderTopView.getLayoutParams();
        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0);
        mHolderTopView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mTimeLineView.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mTimeLineView.setLayoutParams(lp);

    }


    private void setupBottomSheetDialog(String sticker_title) {

        mBottomSheetDialog = new BottomSheetDialog(EditPicture.this);
        View sheetView = getLayoutInflater().inflate(R.layout.pop_sticker_view, null);
        Context context= sheetView.getContext();

        mBottomSheetDialog.setContentView(sheetView);

        mtitle = (TextView) sheetView.findViewById(R.id.title);
        mcloseBtn = (ImageButton) sheetView.findViewById(R.id.closeBtn);
        mstickerGridView = (GridView) sheetView.findViewById(R.id.stickerGridView);

        stickerlist = new ArrayList<Sticker>();
        adapter = new StickerAdapter(context,stickerlist);
        mstickerGridView.setAdapter(adapter);

        mtitle.setText(sticker_title);
        getStickers(sticker_title, context);

        mcloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
            }
        });

        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });

        mstickerGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                if (clickedposition == 0) {
                    changeEffect(position);
                }
                else {
                    Log.d(TAG, "mstickerGridView = " + position);
                    addSticker(stickerlist.get(position).getimage());
                }
                mBottomSheetDialog.dismiss();
            }
        });

        mBottomSheetDialog.show();
    }

    private void getStickers(String title, Context context) {

        int[] stickerArray = new int[0];
        if (title.equals("Filter")) {
            stickerArray = myFilterArray;
        }
        else if (title.equals("Text")) {
            stickerArray = myTextArray;
        }
        else if (title.equals("Glasses")) {
            stickerArray = myGlassesArray;
        }
        else if (title.equals("Cigarattes")) {
            stickerArray = myCigarattesArray;
        }
        else if (title.equals("Chains")) {
            stickerArray = myChainsArray;
        }

        int totalstickers = stickerArray.length;

        for(int l=0; l<totalstickers; l++) {
            Sticker stickers = new Sticker();
            stickers.setimage(stickerArray[l]);
            stickerlist.add(stickers);
        }
        adapter.notifyDataSetChanged();

    }

    private void addSticker(int image) {
        Drawable drawable = ContextCompat.getDrawable(this, image);
        stickerView.addSticker(new DrawableSticker(drawable));
    }

    private void changeEffect(int position) {
        if (position == 0) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(1);

            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            mimage_view.setColorFilter(filter);
        }
        else if (position == 1) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);

            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            mimage_view.setColorFilter(filter);
        }
        else {
            final ColorMatrix matrixA = new ColorMatrix();
            // making image B&W
            matrixA.setSaturation(0);

            final ColorMatrix matrixB = new ColorMatrix();
            // applying scales for RGB color values
            matrixB.setScale(1f, .95f, .82f, 1.0f);
            matrixA.setConcat(matrixB, matrixA);

            final ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrixA);
            mimage_view.setColorFilter(filter);
        }

    }

    private void onSave() {

        String filePrefix = "ThugLife_PIC_";
        String fileExtn = ".jpg";

        int fileNo = 0;
        File dest = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DoDoApp", filePrefix + fileNo + fileExtn);
        while (dest.exists()) {
            fileNo++;
            dest = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DoDoApp", filePrefix + fileNo + fileExtn);
        }

        stickerView.save(dest);

        checkForAudioExist(dest);

    }

    private void onCancel() {
        showGoBackTemplateDialog();
    }

    private void checkForAudioExist(File dest) {

        File audioFile =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DoDoApp" + "/" + "ThugLifeSong" + ".mp3");

        if (audioFile.exists()) {
            Intent intent = new Intent();
            intent.putExtra("editedpic", dest.toString());
            setResult(RESULT_OK, intent);
            finish();
        }
        else {
            final DownloadAudioFileFromURL downloadTask = new DownloadAudioFileFromURL(EditPicture.this, ".mp3", "ThugLifeSong", dest);
            downloadTask.execute(thuflifeaudio);
        }
    }

    /*private void loadSticker() {
        Drawable drawable =
                ContextCompat.getDrawable(this, R.drawable.haizewang_215);
        Drawable drawable1 =
                ContextCompat.getDrawable(this, R.drawable.haizewang_23);
        stickerView.addSticker(new DrawableSticker(drawable));
        stickerView.addSticker(new DrawableSticker(drawable1), com.xiaopo.flying.sticker.Sticker.Position.BOTTOM | com.xiaopo.flying.sticker.Sticker.Position.RIGHT);

        Drawable bubble = ContextCompat.getDrawable(this, R.drawable.bubble);
        stickerView.addSticker(
                new TextSticker(getApplicationContext())
                        .setDrawable(bubble)
                        .setText("Sticker\n")
                        .setMaxTextSize(14)
                        .resizeText()
                , com.xiaopo.flying.sticker.Sticker.Position.TOP);
    }*/

    private void showProgressDialog() {
        dialog = new Dialog(EditPicture.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.progress_percentage_dialog);

        mProgressBar = (ProgressBar) dialog.findViewById(R.id.progress_horizontal);
        mprogressTxtView = dialog.findViewById(R.id.progressTxtView);
        TextView mloadingTxtView = (TextView) dialog.findViewById(R.id.loadingTxtView);

        mloadingTxtView.setText("Saving Picture");

        mProgressBar.setProgress(0);
        mprogressTxtView.setText(String.valueOf(0));

        dialog.show();

        Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawableResource(android.R.color.transparent);
    }

    public void testAdd(View view) {
        final TextSticker sticker = new TextSticker(this);
        sticker.setText("Hello, world!");
        sticker.setTextColor(Color.BLUE);
        sticker.setTextAlign(Layout.Alignment.ALIGN_CENTER);
        sticker.resizeText();

        stickerView.addSticker(sticker);
    }

    private void showGoBackTemplateDialog() {
        new AlertDialog.Builder(EditPicture.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Warning!")
                .setMessage("Current work progress will be deleted if you go back now. Are you sure?")
                .setCancelable(true)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goBackToSelectTemplate();
                    }
                })
                .setNeutralButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();

    }


    @Override
    public boolean onSupportNavigateUp(){
        showGoBackTemplateDialog();
        return true;
    }

    public void onBackPressed(){
        showGoBackTemplateDialog();
    }

    private void goBackToSelectTemplate() {
        Intent intent = new Intent(EditPicture.this, SelectTemplate.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    public class DownloadAudioFileFromURL extends AsyncTask<String, String, String> {

        private Context context;
        private String fileExtn;
        private String filePrefix;
        private File dest;

        public DownloadAudioFileFromURL(Context context, String fileExtn, String filePrefix, File dest) {
            this.context = context;
            this.fileExtn = fileExtn;
            this.filePrefix = filePrefix;
            this.dest = dest;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog();
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
            mProgressBar.setProgress(Integer.parseInt(progress[0]));
            mprogressTxtView.setText(String.valueOf(Integer.parseInt(progress[0])));
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "result = " + result);
            dialog.dismiss();
            if (result != null) {
                if (!result.isEmpty()) {
                    Toast.makeText(context, "#" + filePrefix + "Edit picture error: " + result, Toast.LENGTH_LONG).show();
                }
            }
            else {
               Intent intent = new Intent();
                intent.putExtra("editedpic", dest.toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

}
