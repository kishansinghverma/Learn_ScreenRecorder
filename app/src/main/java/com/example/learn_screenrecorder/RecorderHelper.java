package com.example.learn_screenrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class RecorderHelper extends Service {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(300, getNotification());

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        final MediaRecorder mediaRecorder = new MediaRecorder();
        initRecorder(mediaRecorder, display, displayMetrics);

        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final MediaProjection mediaProjection = projectionManager.getMediaProjection(RESULT_OK, intent);
        final VirtualDisplay virtualDisplay = createVirtualDisplay(mediaProjection, mediaRecorder, displayMetrics);


        mediaRecorder.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();

                virtualDisplay.release();
                mediaProjection.stop();
                stopSelf();
            }
        },5000);


        return START_STICKY;
    }

    public VirtualDisplay createVirtualDisplay(MediaProjection projection, MediaRecorder recorder, DisplayMetrics metrics) {
        VirtualDisplay display= projection.createVirtualDisplay("Mirror Display", metrics.widthPixels, metrics.heightPixels,
                metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY, recorder.getSurface(), null, null);
        return display;
    }

    public void initRecorder(MediaRecorder recorder, Display display, DisplayMetrics metrics) {
        try {
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(Environment.getExternalStorageDirectory() + "/temp.3gp");
            recorder.setVideoSize(metrics.widthPixels, metrics.heightPixels);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setVideoFrameRate(16);
            recorder.setVideoEncodingBitRate(1024*1000);

            int rotation = display.getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            recorder.setOrientationHint(orientation);
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Notification getNotification(){
        String channelId = "Critical Service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(channelId, "Important Service", NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle("Listener Service")
                .build();
        return notification;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
