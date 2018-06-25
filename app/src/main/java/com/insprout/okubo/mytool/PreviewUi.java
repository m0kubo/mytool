package com.insprout.okubo.mytool;

import android.app.Activity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

public class PreviewUi {
    private CameraUi mCameraUi = null;
    private Camera2Ui mCamera2Ui = null;


    public PreviewUi(Activity activity, View view) {
        // Previewに指定されたViewによって、Cameraクラスを使用するか、Camera2クラスを使用するか判別する
        if (view instanceof SurfaceView) {
            mCameraUi = new CameraUi(activity, (SurfaceView)view);
        } else if (view instanceof TextureView) {
            mCamera2Ui = new Camera2Ui(activity, (TextureView)view);
        }
    }

    public void open() {
        if (mCameraUi != null) mCameraUi.open();
        if (mCamera2Ui != null) mCamera2Ui.open();
    }

    public void close() {
        if (mCameraUi != null) mCameraUi.close();
        if (mCamera2Ui != null) mCamera2Ui.close();
    }
}