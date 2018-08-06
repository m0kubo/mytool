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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Ui extends CameraCtrl {

    private Display mDisplay;
    private CameraManager mCameraManager;
    private TextureView mTextureView;
    private String mCameraId = null;
    private int mCameraOrientation = 0;
    private Size mPreviewSize = null;

    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession mCaptureSession = null;
    private ImageReader mImageReader = null;


    public Camera2Ui(@NonNull Activity activity, @NonNull TextureView textureView) {
        mDisplay = activity.getWindowManager().getDefaultDisplay();
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        mTextureView = textureView;

        if (mCameraManager == null) return;
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                //if (mCameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                Integer lensFacing = mCameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                    break;
                }
            }
            if (mCameraId != null) {
                //mCameraOrientation = mCameraManager.getCameraCharacteristics(mCameraId).get(CameraCharacteristics.SENSOR_ORIENTATION);
                Integer orientation = mCameraManager.getCameraCharacteristics(mCameraId).get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (orientation != null) mCameraOrientation = orientation;
            }

        } catch (CameraAccessException e) {
            mCameraId = null;
        }
    }


    @Override
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
                    openCamera();
                    // 目的に合う 撮影サイズを選択/設定する
                    setupSize(mCameraId, width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                    // 目的に合う previewサイズを選択/設定する
                    setupSize(mCameraId, width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                }
            });
        }
    }


    private void setupSize(String cameraId, int width, int height) {
        if (cameraId == null) return;

        // 目的に合う previewサイズを選択/設定する
        mPreviewSize = getFitPreviewSize(getSupportedPreviewSizes(cameraId), width, height);
        if (mPreviewSize != null) {
            // 画像のサイズが確定した
            transformView(mTextureView, mPreviewSize);      // 表示領域にサイズ反映
            // 確定したサイズを元に 撮影用のImageReader作成
            switch(mCameraOrientation) {
                case 90:
                case 270:
                    mImageReader = ImageReader.newInstance(mPreviewSize.getHeight(), mPreviewSize.getWidth(), ImageFormat.JPEG, 1);
                    break;

                default:
                    mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 1);
            }

        } else {
            mImageReader = null;
        }
    }


    @Override
    public void close() {
        closeSession();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void closeSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }


    @Override
    public void takePicture(final File picture, final TakePictureListener listener) {
        if (picture == null || mImageReader == null || mCameraDevice == null) return;

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                // 画像撮影成功
                Log.d("Camera", "onImageAvailable()");
                Image image = imageReader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                final byte[] imageBytes = new byte[ buffer.remaining() ];
                buffer.get(imageBytes);
                image.close();

                savePhoto(picture, imageBytes, listener);
            }
        }, mUiHandler);

        try {
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getRotationDegree(mDisplay.getRotation()));

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createPreviewSession();
                }
            }, mUiHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createPreviewSession() {
        try {
            closeSession();

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            if (mPreviewSize != null) {
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }
            Surface surface = new Surface(texture);

            final CaptureRequest.Builder captureBuilder;
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(surface);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            //List<Surface> outputSurfaces = Arrays.asList(surface, mImageReader.getSurface());
            List<Surface> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(surface);
            if (mImageReader != null) outputSurfaces.add(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        mCaptureSession = session;
                        session.setRepeatingRequest(captureBuilder.build(), null, mUiHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    mCaptureSession = null;
                }
            }, mUiHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void openCamera() {
        try {
            if (mCameraManager != null && mCameraId != null) {
                mCameraManager.openCamera(mCameraId, mStateCallback, null);
            }

        } catch (SecurityException | CameraAccessException e) {
            e.printStackTrace();
        }
    }


    //////////////////////////////////////////////////////////////////////
    //
    // CameraDevice.StateCallback 実装
    //

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

            mCameraDevice = cameraDevice;
            createPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            close();
        }
    };


    //////////////////////////////////////////////////////////////////////
    //
    // private メソッド
    //

    private List<Size> getSupportedPreviewSizes(String cameraId) {
        if (mCameraManager != null) {
            try {
                StreamConfigurationMap map = mCameraManager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map != null) {
//                    return Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888));
                    return Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
                }

            } catch (CameraAccessException ignored) {
            }
        }
        return new ArrayList<>();
    }


    private Size getFitPreviewSize(List<Size> sizes, int viewWidth, int viewHeight) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        Size sizeSelected = sizes.get(0);
        if (viewWidth <= 0 || viewHeight <= 0) {
            return sizeSelected;
        }

        float viewRatio = wideRatio(viewWidth , viewHeight);
        float ratioSelected = 0;
        int widthSelected = 0;
        for (Size size : sizes) {
            float ratio = wideRatio(size.getWidth(), size.getHeight());
            int width = Math.max(size.getWidth(), size.getHeight());
            if (isRatioEqual(ratio, ratioSelected) && width < widthSelected) continue;
            if (ratio >= ratioSelected * 0.99 && ratio <= viewRatio * 1.01) {
                ratioSelected = ratio;
                sizeSelected = size;
                widthSelected = width;
            }
        }
        return sizeSelected;
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
        int cameraOrientation = (mCameraOrientation - getRotationDegree(mDisplay.getRotation()) + 360) % 360;
        switch (cameraOrientation) {
            case 90:
            case 270:
                scale = Math.min(
                        (float) textureView.getWidth() / previewWidth,
                        (float) textureView.getHeight() / previewHeight
                );
                degree = cameraOrientation - 180;
                break;

            case 0:
            case 180:
                scale = Math.min(
                        (float) textureView.getWidth() / previewHeight,
                        (float) textureView.getHeight() / previewWidth
                );
                degree = cameraOrientation;
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
