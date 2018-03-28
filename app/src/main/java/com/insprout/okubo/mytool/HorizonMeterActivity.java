package com.insprout.okubo.mytool;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class HorizonMeterActivity extends AppCompatActivity implements SensorEventListener, SurfaceHolder.Callback {

    private SensorManager mSensorManager;
    private Sensor mMagneticSensor = null;          // 磁気センサー
    private Sensor mAccelerometerSensor = null;     // 加速度センサー
    private TextView mTvHorizon, mTvFace;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;


    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null && mMagneticSensor != null &&  mAccelerometerSensor != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null && mMagneticSensor != null &&  mAccelerometerSensor != null) {
            mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horizon);

        initVars();
        initView();

        if (mMagneticSensor == null ||  mAccelerometerSensor== null) {
            Toast.makeText(this, R.string.msg_missing_sensor, Toast.LENGTH_LONG).show();
            finish();
        }
    }


    private void initVars() {

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        if (mSensorManager != null) {
            List<Sensor> sensors;
            sensors = mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
            if (sensors.size() >= 1) {
                mMagneticSensor = sensors.get(0);
            }
            sensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
            if (sensors.size() >= 1) {
                mAccelerometerSensor = sensors.get(0);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        mTvHorizon = findViewById(R.id.tv_angle);
        mTvFace = findViewById(R.id.tv_face);

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            boolean mFocusing = false;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mFocusing) return true;
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                    mFocusing = true;
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean b, Camera camera) {
                            mFocusing = false;
                        }
                    });
                }
                return true;
            }
        });
    }


//    @Override
//    public void onClick(View view) {
//        switch(view.getId()) {
//            case R.id.btn_flash:
////                boolean flashing = mFlashLight.toggle();
//
////                mButton.setSelected(flashing);
//                // Widgetの ボタンの状態もあわせておく
////                FlashLightWidget.switchButtonImage(this, flashing);
//                break;
//
//            case R.id.btn_viewer:
//                TextViewerActivity.startActivity(this);
//                break;
//
//            case R.id.btn_angle:
//                TextViewerActivity.startActivity(this);
//                break;
//        }
//    }



    public static void startActivity(Context context) {
        Intent intent = new Intent(context, HorizonMeterActivity.class);
        // FLAG_ACTIVITY_CLEAR_TOP: 遷移先のアクティビティが既に動いていればそのアクティビティより上にあるアクティビティを消す。
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // FLAG_ACTIVITY_SINGLE_TOP: 既に動いているアクティビティに遷移する際、作りなおさずに再利用する。
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }


    //////////////////////////////////////////////////////////////////////
    //
    // SensorEventListener 実装
    //

    private final int MATRIX_SIZE = 16;
    /* 回転行列 */
    float[]  inR = new float[MATRIX_SIZE];
    float[] outR = new float[MATRIX_SIZE];
    float[]    I = new float[MATRIX_SIZE];

    /* センサーの値 */
    float[] orientationValues   = new float[3];
    float[] magneticValues      = new float[3];
    float[] accelerometerValues = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) return;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerValues = event.values.clone();
                break;
        }

        if (magneticValues != null && accelerometerValues != null) {
            SensorManager.getRotationMatrix(inR, I, accelerometerValues, magneticValues);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, orientationValues);

            mTvHorizon.setText(getString(R.string.fmt_degree_horizon, Math.abs(Math.toDegrees(orientationValues[2]) + 90)));       //Y軸方向,roll
            mTvFace.setText(getString(R.string.fmt_degree_face, Math.toDegrees(orientationValues[1]) ));       //Y軸方向,roll
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }



    //////////////////////////////////////////////////////////////////////
    //
    // SurfaceHolder.Callback 実装
    //

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        mCamera.stopPreview();

        Camera.Parameters parameters = mCamera.getParameters();
        // 画面の向きを設定
        int orientation = getResources().getConfiguration().orientation;
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
        mCamera.autoFocus(null);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
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


}
