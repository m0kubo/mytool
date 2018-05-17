package com.insprout.okubo.mytool;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Ui {

    private Context mContext;
    private Display mDisplay;
    private TextureView mTextureView;
    private Surface mPreviewSurface;
    private int mCameraOrientation = 0;
    private Size mPreviewSize = null;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;


    public Camera2Ui(@NonNull Activity activity, @NonNull TextureView textureView) {
        mContext = activity;
        mDisplay = activity.getWindowManager().getDefaultDisplay();
        mTextureView = textureView;
    }


    public void open() {
        if (mTextureView.isAvailable()) {
            // TextureView初期化済み
            openCamera();

        } else {
            // TextureView初期化処理
            mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    // open
                    String cameraId = openCamera();
                    if (cameraId == null) return;       // カメラ無効

                    // 目的に合う previewサイズを選択/設定する
                    mPreviewSize = getFitPreviewSize(getSupportedPreviewSizes(cameraId), width, height);
                    if (mPreviewSize != null) transformView(mTextureView, mPreviewSize);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                }
            });
        }
    }

    public void close() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private String openCamera() {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (manager != null) {
            try {
                //カメラIDを取得（背面カメラを選択）
                String backCameraId = null;
                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = cameraId;
                        // カメラオープン(オープン成功時に第2引数のコールバッククラスが呼ばれる)
                        manager.openCamera(backCameraId, mStateCallback, null);

                        // カメラの搭載向き、画面の縦横から 画像の補正角度を求めておく
                        Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        if (orientation == null) orientation = 0;
                        switch(mDisplay.getRotation()) {
                            case Surface.ROTATION_0:
                                mCameraOrientation = (orientation + 270) % 360;
                                break;
                            case Surface.ROTATION_90:
                                mCameraOrientation = orientation;
                                break;
                            case Surface.ROTATION_180:
                                mCameraOrientation = (orientation + 90) % 360;
                                break;
                            case Surface.ROTATION_270:
                                mCameraOrientation = (orientation + 180) % 360;
                                break;
                        }
                        return cameraId;
                    }
                }

            } catch (SecurityException | CameraAccessException e) {
                return null;
            }
        }

        return null;
    }


    //////////////////////////////////////////////////////////////////////
    //
    // CameraDevice.StateCallback 実装
    //

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            if (mPreviewSize != null) texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewSurface = new Surface(texture);

            mCameraDevice = camera;

            try {
                camera.createCaptureSession(
//                        Arrays.asList(mPreviewSurface, mImageReader.getSurface()),
                        Arrays.asList(mPreviewSurface),
                        mSessionCallback,
                        null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };


    //////////////////////////////////////////////////////////////////////
    //
    // CameraCaptureSession.StateCallback 実装
    //

    private final CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCaptureSession = cameraCaptureSession;


            CaptureRequest.Builder captureBuilder = null;
            try {
                captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureBuilder.addTarget(mPreviewSurface);
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                cameraCaptureSession.setRepeatingRequest(captureBuilder.build(), null, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
        }
    };


    //////////////////////////////////////////////////////////////////////
    //
    // private メソッド
    //

    private List<Size> getSupportedPreviewSizes(String cameraId) {
        List<Size> previewSizes = new ArrayList<>();

        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager != null) {
            try {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map != null) {
                    previewSizes = Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888));
                }

            } catch (CameraAccessException e) {
                return previewSizes;
            }
        }
        return previewSizes;
    }


    private Size getFitPreviewSize(List<Size> sizes, int viewWidth, int viewHeight) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        if (viewWidth <= 0 || viewHeight <= 0) {
            return sizes.get(0);
        }

        Size sizeSelected = null;
        float viewRatio = wideRatio(viewWidth , viewHeight);
        float ratioSelected = 0;
        int widthSelected = 0;
        for (Size size : sizes) {
            float ratio = wideRatio(size.getWidth(), size.getHeight());
            int width = Math.max(size.getWidth(), size.getHeight());
            if (isFitRatio(viewRatio, ratio, ratioSelected) && width > widthSelected) {
                ratioSelected = ratio;
                sizeSelected = size;
                widthSelected = width;
            }
        }
        return sizeSelected;
    }

    private boolean isFitRatio(float viewRatio, float ratio, float ratioCandidate) {
        if (ratioCandidate <= 0) return true;
        return wideRatio(ratio, viewRatio) <= wideRatio(ratio, ratioCandidate);
    }

    private float wideRatio(float width, float height) {
        if (width <= 0 || height <= 0) return 0f;
        if (width > height) {
            return width / height;
        } else {
            return height / width;
        }
    }


    private void transformView(TextureView textureView, Size preview) {
        float previewWidth = preview.getWidth();
        float previewHeight = preview.getHeight();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, textureView.getWidth(), textureView.getHeight());
        RectF bufferRect = new RectF(0, 0, previewHeight, previewWidth);
        PointF center = new PointF(viewRect.centerX(), viewRect.centerY());

        float scale;
        float degree;
        switch (mCameraOrientation) {
            case 90:
            case 270:
                scale = Math.min(
                        (float) textureView.getWidth() / previewWidth,
                        (float) textureView.getHeight() / previewHeight
                );
                degree = mCameraOrientation - 180;
                break;

            case 0:
            case 180:
                scale = Math.min(
                        (float) textureView.getWidth() / previewHeight,
                        (float) textureView.getHeight() / previewWidth
                );
                degree = mCameraOrientation;
                break;

            default:
                return;
        }

        bufferRect.offset(center.x - bufferRect.centerX(), center.y - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

        matrix.postScale(scale, scale, center.x, center.y);
        matrix.postRotate(degree, center.x, center.y);

        textureView.setTransform(matrix);
    }

}
