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
import android.media.MediaScannerConnection;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Ui {

    private Context mContext;
    private Display mDisplay;
    private TextureView mTextureView;
    private Surface mPreviewSurface;
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
                        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 1);
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

                                savePhoto(mFile, imageBytes);

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
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                }
            });
        }
    }


    private void savePhoto(File picture, byte[] data) {
        if (picture == null || data == null) return;

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(picture);
            out.write(data);
        } catch (IOException ignored) {
            ignored.printStackTrace();

        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException ignored) {
            }
        }

        MediaScannerConnection.scanFile(
                mContext,
                new String[] { picture.getAbsolutePath() },
                null,
                null);

        Toast.makeText(mContext, "撮影完了: " + picture.getPath(), Toast.LENGTH_SHORT).show();
    }



    public void close() {
        closeSession();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
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
            createPhotoSession();
        }
    }


    private void createPhotoSession() {
        try {
            closeSession();

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, adjustDegree());

//            List<Surface> outputSurfaces = Arrays.asList(mImageReader.getSurface(), new Surface(mTextureView.getSurfaceTexture()));
            List<Surface> outputSurfaces = Collections.singletonList(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        mCaptureSession = session;
                        session.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);
                                // もう一度カメラのプレビュー表示を開始する.
                                createPreviewSession();
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        close();
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


    private void createPreviewSession() {
        try {
            closeSession();

            final CaptureRequest.Builder captureBuilder;
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(mPreviewSurface);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            mPreviewSurface = new Surface(mTextureView.getSurfaceTexture());
            //List<Surface> outputSurfaces = Arrays.asList(mImageReader.getSurface(), mPreviewSurface);
            List<Surface> outputSurfaces = Collections.singletonList(mPreviewSurface);
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
                //カメラIDを取得（背面カメラを選択）
                String backCameraId;
                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = cameraId;
                        // カメラオープン(オープン成功時に第2引数のコールバッククラスが呼ばれる)
                        manager.openCamera(backCameraId, mStateCallback, null);

                        // カメラの搭載向き、画面の縦横から 画像の補正角度を求めておく
                        mCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

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
            if (mPreviewSize != null) {
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }
            mPreviewSurface = new Surface(texture);
            mCameraDevice = camera;

//            try {
//                List<Surface> surfaces = (mImageReader != null) ? Arrays.asList(mImageReader.getSurface(), mPreviewSurface) : Collections.singletonList(mPreviewSurface);
//                camera.createCaptureSession(surfaces, mSessionCallback, null);
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            }
            createPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            close();
        }
    };


    //////////////////////////////////////////////////////////////////////
    //
    // CameraCaptureSession.StateCallback 実装
    //

//    private final CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
//        @Override
//        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//            mCaptureSession = cameraCaptureSession;
//            setRepeating(mCameraDevice, cameraCaptureSession);
//
//        }
//
//        @Override
//        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
//        }
//    };

//    private void setRepeating(CameraDevice cameraDevice, CameraCaptureSession cameraCaptureSession) {
//        if (cameraDevice == null || cameraCaptureSession == null) return;
//        try {
//            CaptureRequest.Builder captureBuilder;
//            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            captureBuilder.addTarget(mPreviewSurface);
//            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            cameraCaptureSession.setRepeatingRequest(captureBuilder.build(), null, null);
//
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }


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
        int cameraOrientation = adjustOrientation(mCameraOrientation);
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

    private int adjustOrientation(Integer cameraOrientation) {
        int orientation = (cameraOrientation != null) ? cameraOrientation : 0;
        return ((orientation - adjustDegree()) + 360) % 360;
    }

    private int adjustDegree() {
        switch (mDisplay.getRotation()) {
            // 反時計回りに 90度 (横)
            case Surface.ROTATION_90:
                return 0;

            // 時計回りに 90度 (横)
            case Surface.ROTATION_270:
                return 180;

            // 180度 (上下逆さま)
            case Surface.ROTATION_180:
                return 270;

            // 正位置 (縦)
            case Surface.ROTATION_0:
            default:
                return 90;
        }
    }

    /////////////////////////////////////

}
