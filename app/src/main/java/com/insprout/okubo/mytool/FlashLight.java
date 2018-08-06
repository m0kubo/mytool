package com.insprout.okubo.mytool;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by okubo on 2018/03/26.
 * FlashLightを 点灯させる機能 (android6.0以降 / 5.1以前 対応)
 */

public class FlashLight {

    private static FlashLight mInstance = null;

    public static FlashLight getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FlashLight(context);
        }
        return mInstance;
    }

    private IFlashLight mFlashLight;

    private FlashLight(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFlashLight = new FlashLight6(context);
        } else {
            mFlashLight = new FlashLight5(context);
        }
    }

    public void release() {
        mFlashLight.release();
    }

    public void turnOn() {
        mFlashLight.turnOn();
    }

    public void turnOff() {
        mFlashLight.turnOff();
    }

    public boolean toggle() {
        return mFlashLight.toggle();
    }

    public boolean hasFlash() {
        return mFlashLight.hasFlash();
    }

    public boolean isFlashing() {
        return mFlashLight.isFlashing();
    }


    //////////////////////////////////////////////////////////////////////
    //
    // Interface
    //

    private interface IFlashLight {

        void release();

        void turnOn();

        void turnOff();

        boolean toggle();

        boolean hasFlash();

        boolean isFlashing();
    }


    //////////////////////////////////////////////////////////////////////
    //
    // android 6.0以降 の FlashLight機能 実装
    //

    @TargetApi(Build.VERSION_CODES.M)
    public class FlashLight6 implements FlashLight.IFlashLight {
        private final static String TAG = "FlashLight6";

        private CameraManager mCameraManager;
        private String mCameraId = null;
        private boolean mFlashing = false;

        private FlashLight6(Context context) {
            mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (mCameraManager != null) {
                mCameraManager.registerTorchCallback(mTorchCallback, new Handler());
            }
        }

        private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                super.onTorchModeChanged(cameraId, enabled);
                mCameraId = cameraId;
                mFlashing = enabled;
                Log.d("Flash", "cameraId" + cameraId + " torch:" + enabled);
            }
        };

        public void release() {
            if (mCameraManager != null) {
                mCameraManager.unregisterTorchCallback(mTorchCallback);
            }
        }

        private void turnOn(boolean flashing) {
            if (mCameraId != null) {
                try {
                    mCameraManager.setTorchMode(mCameraId, flashing);
                } catch (CameraAccessException e) {
                    Log.d(TAG, "turnOn(): " + e.getMessage());
                }
            }
        }

        public void turnOn() {
            turnOn(true);
        }

        public void turnOff() {
            turnOn(false);
        }

        public boolean toggle() {
            turnOn(!mFlashing);
            return !mFlashing;
        }

        public boolean hasFlash() {
            return (mCameraId != null);
        }

        public boolean isFlashing() {
            return mFlashing;
        }

    }


    //////////////////////////////////////////////////////////////////////
    //
    // android 5.1以前の FlashLight機能 実装
    //

    public class FlashLight5 implements FlashLight.IFlashLight {
        private final static String TAG = "FlashLight5";

        private Context mContext;
        private Camera mOpenedCamera = null;
        private boolean mFlashing = false;


        private FlashLight5(Context context) {
            mContext = context;
            mFlashing = false;
        }

        public void release() {
            if (mOpenedCamera == null) return;

            mOpenedCamera.release();
            mOpenedCamera = null;
        }

        public boolean hasFlash() {
            return hasFlash(mContext);
        }

        public void turnOn() {
            if (!hasFlash()) {
                mFlashing = false;
                return;
            }

            Log.d(TAG, "turnOn(): mFlashing = " + mFlashing);
            if (!mFlashing) {
                if (mOpenedCamera == null) {
                    Log.d(TAG, "turnOn(): mOpenedCamera = null");
                    mOpenedCamera = openCamera();
                    if (mOpenedCamera == null) {
                        Log.d(TAG, "turnOn(): mOpenedCamera = NULL");
                        return;
                    }
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
                Log.d(TAG, "hasFlash: try turn off");
                turnOff();
            } else {
                Log.d(TAG, "hasFlash: try turn ON");
                turnOn();
            }
            return mFlashing;
        }

        public boolean isFlashing() {
            return mFlashing;
        }

        private Camera openCamera() {
            try {
                return Camera.open();
            } catch (RuntimeException e) {
                return null;
            }
        }

        private boolean hasFlash(Context context) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        }

        private void turnOnFlash(Camera camera) {
            if (camera == null) return;

            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
        }

        private void turnOffFlash(Camera camera) {
            if (camera == null) return;

            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
        }

    }

}
