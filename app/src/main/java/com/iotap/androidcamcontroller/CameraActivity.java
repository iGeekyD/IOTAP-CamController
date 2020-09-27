package com.iotap.androidcamcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.FrameLayout;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends AppCompatActivity implements MqttCallback {
    private static final String CAMERA_ACTIVITY_ON_CREATE_TAG = "ON_CREATE";
    private static final String CAMERA_ACTIVITY_ON_RESUME_TAG = "ON_RESUME";
    private static final String CAMERA_ACTIVITY_ON_START_TAG = "ON_START";
    private static final String CAMERA_ACTIVITY_ON_PAUSE_TAG = "ON_PAUSE";
    private static final String CAMERA_ACTIVITY_ON_STOP_TAG = "ON_STOP";
    private static final String CAMERA_ACTIVITY_ON_DESTROY_TAG = "ON_DESTROY";
    private static final String CAMERA_ACTIVITY_PICTURE_CALLBACK_TAG = "PIC_CALLBACK";
    private static final String CAMERA_ACTIVITY_GET_MEDIA_TAG = "GET_MEDIA";


    static String topic        = "hello/world";
    static String content      = "Message from MqttPublishSample";
    static int qos             = 1;
    static String broker       = "tcp://192.168.1.18:1883";
    static String clientId     = "JavaSample";
    static MemoryPersistence persistence = new MemoryPersistence();
    MqttClient mqttClient;


    private CameraPreview mPreview;
    private Camera mCamera;
    private FrameLayout mCameraLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        try {
            mqttClient = new MqttClient(broker, clientId, persistence);
            Log.d(CAMERA_ACTIVITY_ON_CREATE_TAG, "MQTT client has been created");
        } catch (MqttException e) {
            e.printStackTrace();
        }

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
        setCameraInstance();
        mCamera.setDisplayOrientation(90);
        mPreview.setCamera(mCamera);
        try {
            subscribeMQTTClient();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(CAMERA_ACTIVITY_ON_PAUSE_TAG, "Activity is paused");
        try {
            Log.d(CAMERA_ACTIVITY_ON_PAUSE_TAG, "Disconnecting MQTT client......");
            mqttClient.disconnect();
            Log.d(CAMERA_ACTIVITY_ON_PAUSE_TAG, "MQTT client has been disconnected");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
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


    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.d("MQTT", "Message arrived");
        mCamera.takePicture(null, null, mPicture);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    void subscribeMQTTClient() throws MqttException {
        mqttClient.connect();
        Log.d("MQTT", "Client Connected");
        mqttClient.setCallback(this);
        Log.d("MQTT", "Callback is set");
        mqttClient.subscribe(topic);
        Log.d("MQTT", "Android subscribed");
    }

}