package com.insprout.okubo.mytool;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CameraUi  implements CameraCtrl.ICamera, SurfaceHolder.Callback {

    private Display mDisplay;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private boolean mFocusing = false;


    @SuppressLint("ClickableViewAccessibility")
    public CameraUi(Activity activity, SurfaceView surfaceView) {
        mDisplay = activity.getWindowManager().getDefaultDisplay();

        mSurfaceView = surfaceView;
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

    @Override
    public void open() {
        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public void close() {
        closeCamera();
        mSurfaceView.getHolder().removeCallback(this);
    }

    private void closeCamera() {
        if (mCamera != null) {
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


    @Override
    public void takePicture(final File picture, final CameraCtrl.TakePictureListener listener) {
        // 対応するプレビューサイズを決定する
        Camera.Parameters parameters = mCamera.getParameters();

        // 撮影可能な画像サイズと プレビュー用サイズとは異なる。
        // プレビュー用のサイズを決める際に、縦横比が撮影可能なサイズのものを抽出しているので、その縦横比にマッチする画像サイズを選択する
        Camera.Size size =  selectPictureSize(parameters.getSupportedPictureSizes(), mSurfaceView.getWidth(), mSurfaceView.getHeight());
        if (size != null) {
            setupCameraRotation(mCamera);
            parameters.setPictureSize(size.width, size.height);

            mCamera.takePicture(
                    null,
                    null,
                    new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            CameraCtrl.savePhoto(picture, data, CameraCtrl.getExifOrientation(mDisplay.getRotation()), listener);

                            //プレビュー再開
                            camera.startPreview();
                        }

                    }
            );
        }
    }


    //////////////////////////////////////////////////////////////////////
    //
    // SurfaceHolder.Callback 実装
    //

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            mCamera = Camera.open();
            determinePreviewSize(mCamera, mSurfaceView.getWidth(), mSurfaceView.getHeight());
            mCamera.setPreviewDisplay(surfaceHolder);

        } catch (IOException | RuntimeException e) {
            // RuntimeExceptionは カメラPermissionがない場合に発生
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int viewWidth, int viewHeight) {
        mCamera.stopPreview();

        // AndroidManifest.xmlで android:configChanges="orientation"が指定された場合にそなえて
        // 念のためここでも プレビュー表示サイズとSurfaceViewとのサイズチェックを行っておく
        if (viewWidth != mSurfaceView.getWidth() || viewHeight != mSurfaceView.getHeight()) {
            determinePreviewSize(mCamera, viewWidth, viewHeight);
        }

        // カメラを再開
        mCamera.startPreview();
        autoFocus();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // 想定外の挙動でSurfaceが破棄された場合にそなえて、念のためrelease処理を呼んでおく
        closeCamera();
    }


    private void determinePreviewSize(Camera camera, int targetWidth, int targetHeight) {
        // 対応するプレビューサイズを決定する
        Camera.Parameters parameters = camera.getParameters();
        // プレビュー用のサイズと、撮影できるサイズは異なる。
        // プレビュー用のサイズ群から 縦横比が撮影可能なサイズに一致するもののみを抽出する
        List<Camera.Size> sizes = availableSizes(parameters.getSupportedPreviewSizes(), parameters.getSupportedPictureSizes());

        Camera.Size size = selectFitSize(sizes, CameraCtrl.wideRatio(targetWidth, targetHeight));
        if (size != null) {
            setupCameraRotation(camera);
            parameters.setPreviewSize(size.width, size.height);

            // 画像が歪まない様に、SurfaceViewのサイズを プレビュー画像の縦横比にあわせてリサイズする
            float previewRatio;
            switch (mDisplay.getRotation()) {
                // 反時計回りに 90度 (横)
                case Surface.ROTATION_90:
                    camera.setDisplayOrientation(0);
                    previewRatio = (float)size.width / (float)size.height;
                    break;

                // 時計回りに 90度 (横)
                case Surface.ROTATION_270:
                    camera.setDisplayOrientation(180);
                    previewRatio = (float)size.width / (float)size.height;
                    break;

                // 180度 (上下逆さま)
                case Surface.ROTATION_180:
                    camera.setDisplayOrientation(270);
                    previewRatio = (float)size.height / (float)size.width;
                    break;

                // 正位置 (縦)
                case Surface.ROTATION_0:
                default:
                    camera.setDisplayOrientation(90);
                    previewRatio = (float)size.height / (float)size.width;
                    break;
            }

            // 画像が歪まない様に、SurfaceViewのサイズを プレビュー画像の縦横比にあわせてリサイズする
            if ((float) mSurfaceView.getWidth() / (float) mSurfaceView.getHeight() > previewRatio) {
                // 表示エリアより プレビュー画像の方が 幅が短い
                mSurfaceView.getLayoutParams().width = Math.round(mSurfaceView.getHeight() * previewRatio);

            } else {
                // 表示エリアより プレビュー画像の方が 高さが低い
                mSurfaceView.getLayoutParams().height = Math.round(mSurfaceView.getWidth() / previewRatio);
            }
        }
    }


    private void setupCameraRotation(Camera camera) {
        camera.setDisplayOrientation(CameraCtrl.getRotationDegree(mDisplay.getRotation()));
    }


    private Camera.Size selectFitSize(List<Camera.Size> sizes, float maxWideRate) {
        Camera.Size size = selectWideSize(sizes, maxWideRate);
        return (size != null ? size : selectSquareSize(sizes));
    }

    // 指定比率以下で、最も横長なサイズを選ぶ
    // 縦横比が同じ場合は、解像度の高いものを返す
    private Camera.Size selectWideSize(List<Camera.Size> sizes, float maxWideRate) {
        if (sizes == null || sizes.isEmpty()) return null;

        Camera.Size candidate = null;
        float candidateRatio = 0f;
        int candidateWidth = 0;
        for (Camera.Size size : sizes) {
            float ratio = wideRatio(size);
            if (ratio >= candidateRatio && ratio <= maxWideRate && size.width > candidateWidth) {
                candidateRatio = ratio;
                candidateWidth = size.width;
                candidate = size;
            }
        }
        return candidate;
    }

    // 最も 正方形に近いサイズを選ぶ
    private Camera.Size selectSquareSize(List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) return null;

        Camera.Size candidate = sizes.get(0);
        float candidateRatio = Float.POSITIVE_INFINITY;
        int candidateWidth = 0;
        for (Camera.Size size : sizes) {
            float ratio = wideRatio(size);
            if (ratio <= 0) continue;
            if (ratio < candidateRatio && size.width > candidateWidth) {
                candidateRatio = ratio;
                candidateWidth = size.width;
                candidate = size;
            }
        }
        return candidate;
    }

    // プレビュー可能サイズと、撮影可能サイズで、縦横比が共通なサイズのみをかえす
    private List<Camera.Size> availableSizes(List<Camera.Size> previewSizes, List<Camera.Size> pictureSizes) {
        List<Camera.Size> sizes = new ArrayList<>();
        for(Camera.Size size1 : previewSizes) {
            for(Camera.Size size2 : pictureSizes) {
                if (isRatioEqual(size1, size2)) {
                    sizes.add(size1);
                    break;
                }
            }
        }
        return (!sizes.isEmpty() ? sizes : previewSizes);
    }

    private Camera.Size selectPictureSize(List<Camera.Size> pictureSizes, int width, int height) {
        float wideRatio = CameraCtrl.wideRatio(width, height);
        Camera.Size size = null;
        for(Camera.Size pictureSize : pictureSizes) {
            if (CameraCtrl.isRatioEqual(wideRatio(pictureSize), wideRatio)) {
                if (size == null || size.width < pictureSize.width) {
                    size = pictureSize;
                }
            }
        }
        return size;
    }

    // 指定された2つのサイズの縦横比が同じかどうかを判別する
    // 計算誤差を鑑み、差が1%以内なら同一比率とみなす
    private boolean isRatioEqual(Camera.Size size1, Camera.Size size2) {
        return CameraCtrl.isRatioEqual(wideRatio(size1), wideRatio(size2));
    }

    // (長寸 / 短寸)の値を返す
    private float wideRatio(Camera.Size size) {
        return CameraCtrl.wideRatio(size.width, size.height);
    }

}
