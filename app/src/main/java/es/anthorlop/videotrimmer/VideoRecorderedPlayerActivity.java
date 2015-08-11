package es.anthorlop.videotrimmer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import es.anthorlop.videotrimmer.custom.RangeSeekBar;
import es.anthorlop.videotrimmer.custom.VideoControllerView;

public class VideoRecorderedPlayerActivity extends Activity
        implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, VideoControllerView.MediaPlayerControl {

    public static String TAG = "VideoRecorderedPlayerActivity";

    public static String VIDEO_PATH = "VideoPath";

    private MediaPlayer player;
    private VideoControllerView controller;

    RangeSeekBar<Integer> seekBar;

    @InjectView(R.id.videoSurfaceContainer)
    FrameLayout videoSurfaceContainer;

    @InjectView(R.id.videoSurface)
    SurfaceView videoSurface;

    @InjectView(R.id.cancelButton)
    Button cancelButton;

    @InjectView(R.id.continueButton)
    Button continueButton;

    @InjectView(R.id.rangeLayout)
    RelativeLayout rangeLayout;

    private ProgressDialog mProgressDialog;

    private String path;
    private String finalPath;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_player);

        ButterKnife.inject(this);

        if (getIntent().getExtras() != null) {
            path = getIntent().getExtras().getString(VIDEO_PATH);
            init();
        }



        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteVideo();
                finish();
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // comprobamos si se debe editar el video
                if (controller.getMinPosition() > 0 || controller.getMaxPosition() < getDuration()) {

                    mProgressDialog = new ProgressDialog(VideoRecorderedPlayerActivity.this);
                    mProgressDialog.setMessage("Preparando vídeo ...");
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();

                    new PostTrimAsynkTask(path).execute("");

                } else {

                    goBackAndSendVideo();

                }
            }
        });

        videoSurfaceContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.onclick();
            }
        });
    }

    private void goBackAndReopenVideo() {

        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(VIDEO_PATH, finalPath);
        resultIntent.putExtras(bundle);
        setResult(RESULT_CANCELED, resultIntent);
        VideoRecorderedPlayerActivity.this.finish();

    }

    private void goBackAndSendVideo() {

        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(VIDEO_PATH, path);
        resultIntent.putExtras(bundle);
        setResult(RESULT_OK, resultIntent);
        VideoRecorderedPlayerActivity.this.finish();

    }

    public void onPause() {
        super.onPause();
    }

    private void init() {
        SurfaceHolder videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);

        player = new MediaPlayer();
        controller = new VideoControllerView(this);

        try {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(this, Uri.parse(path));
            player.setOnPreparedListener(this);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            player.setDisplay(holder);
            player.prepareAsync();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        /*player.reset();
        player.release();*/
    }

    // Implement MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        controller.setMediaPlayer(this);

        controller.setMinPosition(0);
        controller.setMaxPosition(getDuration());

        seekTo(1);

        seekBar = new RangeSeekBar<Integer>(0, getDuration(), this);
        seekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                controller.setMinPosition(minValue);
                controller.setMaxPosition(maxValue);

                if (getCurrentPosition() < minValue) {
                    seekTo(minValue);
                }

                if (getCurrentPosition() > maxValue) {
                    seekTo(maxValue);
                }

                controller.show();
            }
        });

        rangeLayout.addView(seekBar);

        if (player.getVideoWidth() > player.getVideoHeight()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        controller.setAnchorView(videoSurfaceContainer, player.getVideoWidth(), player.getVideoHeight());

        controller.show();

        //player.start();
    }
    // End MediaPlayer.OnPreparedListener

    // Implement VideoMediaController.MediaPlayerControl
    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return player.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public void pause() {
        player.pause();
    }

    @Override
    public void seekTo(int i) {
        player.seekTo(i);
    }

    @Override
    public void start() {
        player.start();
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void toggleFullScreen() {
        //обрабатываем нажатие на кнопку увеличения видео в весь экран
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) videoSurfaceContainer .getLayoutParams();
        params.width = metrics.widthPixels;
        params.height = metrics.heightPixels;
        params.leftMargin = 0;
        videoSurfaceContainer.setLayoutParams(params);
    }
    // End VideoMediaController.MediaPlayerControl

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.reset(this);

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

    }

    private FileDataSourceImpl file = null;
    private FileOutputStream fos = null;
    private WritableByteChannel fc = null;

    private void trimVideo(String path, String finalPath, double startTime, double endTime) {

        Movie movie = null;
        try {
            file = new FileDataSourceImpl(path);


            movie = new MovieCreator()
                        .build(file);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());
        // remove all tracks we will create new tracks from the old

        boolean timeCorrected = false;

        // Here we try to find a track that has sync samples. Since we can only
        // start decoding
        // at such a sample we SHOULD make sure that the start of the new
        // fragment is exactly
        // such a frame
        for (Track track : tracks) {
            if (track.getSyncSamples() != null
                    && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    // This exception here could be a false positive in case we
                    // have multiple tracks
                    // with sync samples at exactly the same positions. E.g. a
                    // single movie containing
                    // multiple qualities of the same video (Microsoft Smooth
                    // Streaming file)

                    throw new RuntimeException(
                            "The startTime has already been corrected by another track with SyncSample. Not Supported.");

                }
            }
        }

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            long startSample = -1;
            long endSample = -1;

            for (int i = 0; i < track.getSampleDurations().length; i++) {
                if (currentTime <= startTime) {

                    // current sample is still before the new starttime
                    startSample = currentSample;
                }
                if (currentTime <= endTime) {
                    // current sample is after the new start time and still before the new endtime
                    endSample = currentSample;
                } else {
                    // current sample is after the end of the cropped video
                    break;
                }
                currentTime += (double) track.getSampleDurations()[i] / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }

            movie.addTrack(new CroppedTrack(track, startSample, endSample));

            Container out = new DefaultMp4Builder().build(movie);
            //MovieHeaderBox mvhd = Path.getPath(out, "moov/mvhd");
            //mvhd.setMatrix(Matrix.ROTATE_180);

            File dst = new File(finalPath);

            try {
                fos = new FileOutputStream(dst);

                fc = fos.getChannel();

                try {
                    out.writeContainer(fc);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        deleteVideo();

    }

    private void deleteVideo() {
        File mp4 = new File(path);
        if (mp4.exists() && mp4.isFile()) {
            mp4.delete();
        }
    }


    private class PostTrimAsynkTask extends AsyncTask<String, Void, Boolean> {

        private String mPath;

        public PostTrimAsynkTask(String path) {
            this.mPath = path;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            try {

                finalPath = mPath.replace(".mp4", "_edited.mp4");

                trimVideo(mPath, finalPath, controller.getMinPosition()/1000L, controller.getMaxPosition()/1000L);

                return true;

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result){

            try {
                fc.close();
                fos.close();
                file.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }

            if (result) {
                goBackAndReopenVideo();
                Toast.makeText(VideoRecorderedPlayerActivity.this, "Video editado correctamente",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(VideoRecorderedPlayerActivity.this, "Error al editar el vídeo",
                        Toast.LENGTH_LONG).show();
            }

            mProgressDialog.dismiss();

        }
    }



}