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
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;

import com.insprout.okubo.mytool.util.CameraUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Ui {

    private Context mContext;
    private Display mDisplay;
    private TextureView mTextureView;
    private Integer mCameraOrientation = 0;
    private Size mPreviewSize = null;

    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession mCaptureSession = null;
    private ImageReader mImageReader = null;
    private File mFile = null;


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
                    if (mPreviewSize != null) {
                        // 画像のサイズが確定した
                        transformView(mTextureView, mPreviewSize);      // 表示領域にサイズ反映
                        // 確定したサイズを元に 撮影用のImageReader作成
                        mImageReader = ImageReader.newInstance(mPreviewSize.getHeight(), mPreviewSize.getWidth(), ImageFormat.JPEG, 1);
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

                                CameraUtils.savePhoto(mContext, mFile, imageBytes, -1);

                            }
                        }, null);

                    } else {
                        mImageReader = null;
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
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


    public void takePicture(final File picture) {
        mFile = picture;
        if (picture == null) return;

        if (mImageReader != null && mCameraDevice != null) {
            //createPhotoSession();
            takePicture();
        }
    }

    private void takePicture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtils.getRotationDegree(mDisplay.getRotation()));
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

                    CameraUtils.savePhoto(mContext, mFile, imageBytes, -1);

                }
            }, null);

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createPreviewSession();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

//    private void createPhotoSession() {
//        try {
//            closeSession();
//
//            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(mImageReader.getSurface());
//            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, CameraUtils.getRotationDegree(mDisplay.getRotation()));
//
//            List<Surface> outputSurfaces = Arrays.asList(mImageReader.getSurface(), new Surface(mTextureView.getSurfaceTexture()));
//            //List<Surface> outputSurfaces = Collections.singletonList(mImageReader.getSurface());
//            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(@NonNull CameraCaptureSession session) {
//                    try {
//                        mCaptureSession = session;
//                        session.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
//                            @Override
//                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//                                super.onCaptureCompleted(session, request, result);
//                                // もう一度カメラのプレビュー表示を開始する.
//                                createPreviewSession();
//                            }
//                        }, null);
//                    } catch (CameraAccessException e) {
//                        close();
//                    }
//                }
//
//                @Override
//                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
//                    mCaptureSession = null;
//                }
//            }, null);
//
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }

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

            List<Surface> outputSurfaces = Arrays.asList(surface, mImageReader.getSurface());
            //List<Surface> outputSurfaces = Collections.singletonList(surface);
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        mCaptureSession = session;
                        session.setRepeatingRequest(captureBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    mCaptureSession = null;
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private String openCamera() {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (manager != null) {
            try {
                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    //カメラIDを取得（背面カメラを選択）
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        // カメラオープン(オープン成功時に第2引数のコールバッククラスが呼ばれる)
                        manager.openCamera(cameraId, mStateCallback, null);

                        // カメラの搭載向き、画面の縦横から 画像の補正角度を求めておく
                        mCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        if (mCameraOrientation == null) mCameraOrientation = 0;

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
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager != null) {
            try {
                StreamConfigurationMap map = cameraManager.getCameraCharacteristics(cameraId)
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
        return ratioCandidate <= 0 || wideRatio(ratio, viewRatio) <= wideRatio(ratio, ratioCandidate);
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
        int cameraOrientation = (mCameraOrientation - CameraUtils.getRotationDegree(mDisplay.getRotation()) + 360) % 360;
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


    /////////////////////////////////////

}
