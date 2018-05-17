package com.insprout.okubo.mytool;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

public class CameraUi implements SurfaceHolder.Callback {

    private Context mContext;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private boolean mFocusing = false;


    @SuppressLint("ClickableViewAccessibility")
    public CameraUi(Context context, SurfaceView surfaceView) {
        mContext = context;
        mSurfaceView = surfaceView;
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mFocusing) return true;
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                    autoFocus();
                }
                return true;
            }
        });
    }

    public void close() {
        if (mCamera!= null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void autoFocus() {
        if (mCamera == null) return;

        mFocusing = true;
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {
                mFocusing = false;
            }
        });
    }


    // 最も横長な プレビューサイズを選らぶ
    private Camera.Size getWidePreviewSize(List<Camera.Size> sizes) {
        if (sizes == null) {
            return null;
        }

        Camera.Size sizeWide = null;
        double ratioMax = 0;
        for (Camera.Size size : sizes) {
            if (size.height <= 0) continue;
            double ratio = (double) size.width / size.height;
            if (ratio >= ratioMax) {
                ratioMax = ratio;
                sizeWide = size;
            }
        }

        return sizeWide;
    }


    //////////////////////////////////////////////////////////////////////
    //
    // SurfaceHolder.Callback 実装
    //

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            mCamera = Camera.open();
            mCamera.setPreviewDisplay(mSurfaceHolder);

        } catch (IOException | RuntimeException e) {
            // RuntimeExceptionは カメラPermissionがない場合に発生
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        mCamera.stopPreview();

        Camera.Parameters parameters = mCamera.getParameters();
        // 画面の向きを設定
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mCamera.setDisplayOrientation(90);
        } else {
            mCamera.setDisplayOrientation(0);
        }

        // 対応するプレビューサイズを決定する
        Camera.Size previewSize = getWidePreviewSize(parameters.getSupportedPreviewSizes());
        ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
        double preview_ratio = (double)previewSize.width / (double)previewSize.height;
        if (width > height) {
            // 横長
            int new_height = (int)(width / preview_ratio);
            if (new_height <= height) {
                layoutParams.height = height;
            } else {
                layoutParams.width = (int)(height * preview_ratio);
            }

        } else {
            // 縦長
            int new_width = (int)(height / preview_ratio);
            if (new_width <= width) {
                layoutParams.width = new_width;
            } else {
                layoutParams.height = (int)(width * preview_ratio);
            }
        }
        mSurfaceView.setLayoutParams(layoutParams);

        // パラメータを設定してカメラを再開
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        autoFocus();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        close();
    }

}
