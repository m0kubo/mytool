package com.insprout.okubo.mytool;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.insprout.okubo.mytool.util.CameraUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CameraUi implements SurfaceHolder.Callback {

    private Context mContext;
    private Display mDisplay;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private boolean mFocusing = false;


    @SuppressLint("ClickableViewAccessibility")
    public CameraUi(Activity activity, SurfaceView surfaceView) {
        mContext = activity;
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

    public void open() {
        mSurfaceView.getHolder().addCallback(this);
    }

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


    public void takePicture(final File picture) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                determinePictureSize(picture, mCamera, mSurfaceView.getWidth(), mSurfaceView.getHeight());
            }
        });
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
        Camera.Parameters parameters = mCamera.getParameters();
        // プレビュー用のサイズと、撮影できるサイズは異なる。
        // プレビュー用のサイズ群から 縦横比が撮影可能なサイズに一致するもののみを抽出する
        List<Camera.Size> sizes = availableSizes(parameters.getSupportedPreviewSizes(), parameters.getSupportedPictureSizes());

        Camera.Size size = selectFitSize(sizes, wideRatio(targetWidth, targetHeight));
        if (size != null) {
            setupCameraRotation(camera);
            parameters.setPreviewSize(size.width, size.height);

            // 画像が歪まない様に、SurfaceViewのサイズを プレビュー画像の縦横比にあわせてリサイズする
            float previewRatio = (float) size.width / (float) size.height;
            if ((float) mSurfaceView.getWidth() / (float) mSurfaceView.getHeight() > previewRatio) {
                // 表示エリアより プレビュー画像の方が 幅が短い
                mSurfaceView.getLayoutParams().width = Math.round(mSurfaceView.getHeight() * previewRatio);

            } else {
                // 表示エリアより プレビュー画像の方が 高さが低い
                mSurfaceView.getLayoutParams().height = Math.round(mSurfaceView.getWidth() * previewRatio);
            }

        }
    }

    private void determinePictureSize(final File picture, Camera camera, int targetWidth, int targetHeight) {
        // 対応するプレビューサイズを決定する
        Camera.Parameters parameters = mCamera.getParameters();
        // 撮影可能な画像サイズと プレビュー用サイズとは異なる。
        // プレビュー用のサイズを決める際に、縦横比が撮影可能なサイズのものを抽出しているので、その縦横比にマッチする画像サイズを選択する
        Camera.Size size =  selectPictureSize(parameters.getSupportedPictureSizes(), targetWidth, targetHeight);

        if (size != null) {
            setupCameraRotation(camera);
            parameters.setPictureSize(size.width, size.height);

            mCamera.takePicture(
                    null,
                    null,
                    new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            CameraUtils.savePhoto(mContext, picture, data, CameraUtils.getExifOrientation(mDisplay.getRotation()));

                            //プレビュー再開
                            camera.startPreview();
                        }

                    }
            );

        }
    }


    private void setupCameraRotation(Camera camera) {
        camera.setDisplayOrientation(CameraUtils.getRotationDegree(mDisplay.getRotation()));
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
        float candidateWidth = 0f;
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
        for (Camera.Size size : sizes) {
            float ratio = wideRatio(size);
            if (ratio <= 0) continue;
            if (ratio < candidateRatio) {
                candidateRatio = ratio;
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
        float wideRatio = wideRatio(width, height);
        Camera.Size size = null;
        for(Camera.Size pictureSize : pictureSizes) {
            if (isRatioEqual(wideRatio(pictureSize), wideRatio)) {
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
        return isRatioEqual(wideRatio(size1), wideRatio(size2));
    }

    private boolean isRatioEqual(float ratio1, float ratio2) {
        if (ratio1 == 0f || ratio2 == 0f) return false;
        float rate = ratio1 / ratio2;
        return (rate >= 0.99f && rate <= 1.01f);
    }

    // (長寸 / 短寸)の値を返す
    private float wideRatio(Camera.Size size) {
        return wideRatio(size.width, size.height);
    }

    // (長寸 / 短寸)の値を返す
    private float wideRatio(int width, int height) {
        if (width <= 0 || height <= 0) return 0f;
        return (float)Math.max(width, height) / (float)Math.min(width, height);
    }

}
