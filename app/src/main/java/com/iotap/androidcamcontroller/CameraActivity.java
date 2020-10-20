package com.iotap.androidcamcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.response.GetUpdatesResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity{
    private static final String CAMERA_ACTIVITY_ON_CREATE_TAG = "ON_CREATE";
    private static final String CAMERA_ACTIVITY_ON_RESUME_TAG = "ON_RESUME";
    private static final String CAMERA_ACTIVITY_ON_START_TAG = "ON_START";
    private static final String CAMERA_ACTIVITY_ON_PAUSE_TAG = "ON_PAUSE";
    private static final String CAMERA_ACTIVITY_ON_STOP_TAG = "ON_STOP";
    private static final String CAMERA_ACTIVITY_ON_DESTROY_TAG = "ON_DESTROY";
    private static final String CAMERA_ACTIVITY_PICTURE_CALLBACK_TAG = "PIC_CALLBACK";
    private static final String CAMERA_ACTIVITY_GET_MEDIA_TAG = "GET_MEDIA";

    private static final String LONG_POLLING_THREAD = "LONG_POLLING_THREAD";

    private CameraPreview mPreview;
    private Camera mCamera;
    private FrameLayout mCameraLayout;

    private TelegramBot bot;
    private HandlerThread hThread;
    private Handler handler;

    private final long eachSecond = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /************************* CAMERA SETTINGS ***********************/
        Log.d(CAMERA_ACTIVITY_ON_CREATE_TAG, "The device has a camera: " + String.valueOf(checkCameraHardware(this)));
        Log.d(CAMERA_ACTIVITY_ON_CREATE_TAG, "Try to get camera instance.................");
        setCameraInstance();
        if (mCamera == null) {
            Log.d(CAMERA_ACTIVITY_ON_CREATE_TAG, "Fail! Camera instance is not available");
            this.finish();
        }
        Log.d(CAMERA_ACTIVITY_ON_CREATE_TAG, "Success! Camera instance has been created");

        Log.d(CAMERA_ACTIVITY_ON_CREATE_TAG, "Try to create preview for the camera.................");
        mPreview = new CameraPreview(this, mCamera);
        if (mPreview == null) {
            Log.d(CAMERA_ACTIVITY_ON_CREATE_TAG, "Fail! Camera preview is not created");
            this.finish();
        }
        Log.d(CAMERA_ACTIVITY_ON_CREATE_TAG, "Success! Camera preview has been created");

        mCameraLayout = (FrameLayout) findViewById(R.id.camera_preview);
        mCameraLayout.addView(mPreview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        /************************* /CAMERA SETTINGS ***********************/

        /************************* TELEGRAM BOT SETTINGS ***********************/
        bot = new TelegramBot("1270963540:AAEuQ7n9g6TWi5jaMwCGE36dAMRzYiR_PkA");
        /************************* /TELEGRAM BOT SETTINGS ***********************/

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(CAMERA_ACTIVITY_ON_START_TAG, "Activity is Started");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(CAMERA_ACTIVITY_ON_RESUME_TAG, "Application resumed");
        /************************* CAMERA START ***********************/
        setCameraInstance();
        mCamera.setDisplayOrientation(90);
        mPreview.setCamera(mCamera);
        /************************* /CAMERA START ***********************/
        /************************* BOT START ***********************/
        hThread = new HandlerThread("Bot Thread");
        Log.d(CAMERA_ACTIVITY_ON_RESUME_TAG, "Creating background thread for bot");
        hThread.start();
        handler = new Handler(hThread.getLooper());
        Log.d(CAMERA_ACTIVITY_ON_RESUME_TAG, "Thread created: " + hThread.getThreadId());
        final GetUpdates getUpdates = new GetUpdates().limit(100).offset(0).timeout(0);
        Runnable longPoll = new Runnable() {
            @Override
            public void run() {
                GetUpdatesResponse updatesResponse = bot.execute(getUpdates);
                List<Update> updates = updatesResponse.updates();
                if (updates.size() > 0) {
                    Update update = updates.get(updates.size() - 1);
                    getUpdates.offset(update.updateId() + 1);
                    Log.d(CAMERA_ACTIVITY_ON_RESUME_TAG, "Telegram message text " + update.message().text());
                }
                handler.postDelayed(this, eachSecond);
            }
        };
        handler.postDelayed(longPoll, eachSecond);
        /************************* /BOT START ***********************/


    }

    @Override
    protected void onPause() {
        hThread.quit();

        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;

        super.onPause();
        Log.d(CAMERA_ACTIVITY_ON_PAUSE_TAG, "Activity is paused");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(CAMERA_ACTIVITY_ON_STOP_TAG, "Activity is stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(CAMERA_ACTIVITY_ON_DESTROY_TAG, "Activity is destroyed");
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    private void setCameraInstance(){
        if (mCamera != null)
            return;
        try {
            mCamera = Camera.open(0); // attempt to get a Camera instance
            Log.d(CAMERA_ACTIVITY_ON_RESUME_TAG, "Camera reopened");
        }
        catch (Exception e){
            e.printStackTrace();
            // Camera is not available (in use or does not exist)
        }
    }


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = null;
                pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null) {
                    Log.d(CAMERA_ACTIVITY_PICTURE_CALLBACK_TAG, "File is not created");
                    return;
                }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d(CAMERA_ACTIVITY_PICTURE_CALLBACK_TAG, "Picture is taken:" + pictureFile.getAbsolutePath());
                mCamera.startPreview();
            } catch (FileNotFoundException e) {
                Log.d(CAMERA_ACTIVITY_PICTURE_CALLBACK_TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(CAMERA_ACTIVITY_PICTURE_CALLBACK_TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){

        String storageState = Environment.getExternalStorageState();
        if (!storageState.equals(Environment.MEDIA_MOUNTED)) {
            Log.d(CAMERA_ACTIVITY_GET_MEDIA_TAG, "SD card state is:" + storageState);
            return null;
        }

        File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        Log.d(CAMERA_ACTIVITY_GET_MEDIA_TAG, "Storage absolute path: " + mediaStorageDir.getAbsolutePath());
        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(CAMERA_ACTIVITY_GET_MEDIA_TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        return mediaFile;
    }

}