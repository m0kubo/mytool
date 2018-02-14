package com.insprout.okubo.mytool;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;

/**
 * Created by okubo on 2018/02/08.
 */

public class FlashLight {

    private static FlashLight mInstance = null;

    private Context mContext;
    private Camera mOpenedCamera = null;
    private boolean mFlashing = false;

    public static FlashLight getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FlashLight(context);
        }
        return mInstance;
    }

    private FlashLight(Context context) {
        mContext = context;
        mFlashing = false;
    }

    public void release() {
        if (mOpenedCamera == null) return;

        mOpenedCamera.release();
        mOpenedCamera = null;
    }

    public boolean isOpened() {
        return (mOpenedCamera != null);
    }

    public boolean hasFlash() {
        return hasFlash(mContext);
    }

    public void turnOn() {
        if (!hasFlash()) {
            mFlashing = false;
            return;
        }

        if (!mFlashing) {
            if (mOpenedCamera == null) {
                mOpenedCamera = openCamera();
                if (mOpenedCamera == null) return;
            }
            turnOnFlash(mOpenedCamera);
            mFlashing = true;
        }
    }

    public void turnOff() {
        if (hasFlash()) {
            if (mFlashing) {
                if (mOpenedCamera != null) {
                    turnOffFlash(mOpenedCamera);
                }
            }
            release();      // Cameraオブジェクトを openしっぱなしだと他のアプリが カメラをコントロールできないので 都度releaseする
        }
        mFlashing = false;
    }

    public boolean toggle() {
        if (mFlashing) {
            turnOff();
        } else {
            turnOn();
        }
        return mFlashing;
    }

    public boolean isFlashing() {
        return mFlashing;
    }


    //////////////////////////////////////////////////////////////////////
    //
    // static メソッド
    //

    private static Camera openCamera() {
        try {
            return Camera.open();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean hasFlash(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private static void turnOnFlash(Camera camera) {
        if (camera == null) return;

        Camera.Parameters params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(params);
        camera.startPreview();
    }

    private static void turnOffFlash(Camera camera) {
        if (camera == null) return;

        Camera.Parameters params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(params);
        camera.stopPreview();
    }

//    private static boolean isFlashing(Camera camera) {
//        if (camera == null) return false;
//
//        Camera.Parameters params = camera.getParameters();
//        String flashMode = params.getFlashMode();
//        return (flashMode != null && !Camera.Parameters.FLASH_MODE_OFF.equals(flashMode));
//    }

}