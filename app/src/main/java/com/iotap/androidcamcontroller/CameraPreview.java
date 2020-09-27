package com.iotap.androidcamcontroller;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static final String CAMERA_PREVIEW_TAG = "PREVIEW_CREATED";
    private static final String CAMERA_PREVIEW_SURFACE_CREATED_TAG = "SURFACE_CREATED";
    private static final String CAMERA_PREVIEW_SURFACE_DESTROYED_TAG = "SURFACE_DESTROYED";
    private static final String CAMERA_PREVIEW_SURFACE_CHANGED_TAG = "SURFACE_CHANGED";


    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        Log.d(CAMERA_PREVIEW_TAG, "Camera set");
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        if (camera == mCamera) {
            Log.d(CAMERA_PREVIEW_TAG, "Camera is the same");
            return;
        }
        mCamera = camera;
        Log.d(CAMERA_PREVIEW_TAG, "Camera reset");

    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        Log.d(CAMERA_PREVIEW_SURFACE_CREATED_TAG, "Surface created");
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(CAMERA_PREVIEW_SURFACE_CREATED_TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        Log.d(CAMERA_PREVIEW_SURFACE_DESTROYED_TAG, "Surface destroyed");
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        Log.d(CAMERA_PREVIEW_SURFACE_CHANGED_TAG, "Surface changed");
        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(CAMERA_PREVIEW_SURFACE_CHANGED_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}