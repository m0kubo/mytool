package com.insprout.okubo.mytool;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PhotoActivity extends AppCompatActivity {

    private CameraCtrl mCameraUi;
    private File mFolder;


    @Override
    protected void onPause() {
        super.onPause();
        mCameraUi.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraUi.open();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        initView();

    }


    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        mCameraUi = new CameraCtrl(this, findViewById(R.id.preview));
    }


    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btn_photo:
                takePhoto();
                break;
        }
    }

    private void takePhoto() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd_HHmmss_SSS", Locale.ENGLISH);
        String fileName = "IMG_"+ dateFormat.format(new Date(System.currentTimeMillis())) + ".jpeg";
        final File filePhoto = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);

        mCameraUi.takePicture(filePhoto, new CameraCtrl.TakePictureListener() {
            @Override
            public void onTakePicture(boolean result) {
                if (result) {
                    Toast.makeText(PhotoActivity.this, "撮影完了: " + filePhoto.getPath(), Toast.LENGTH_SHORT).show();
                    // コンテンツ管理DBに画像を登録
                    MediaScannerConnection.scanFile(
                            PhotoActivity.this,
                            new String[]{ filePhoto.getAbsolutePath() },
                            new String[]{ "image/jpeg" },
                            null);
                }
            }
        });
    }


    public static void startActivity(Context context) {
        Intent intent = new Intent(context, PhotoActivity.class);
        // FLAG_ACTIVITY_CLEAR_TOP: 遷移先のアクティビティが既に動いていればそのアクティビティより上にあるアクティビティを消す。
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // FLAG_ACTIVITY_SINGLE_TOP: 既に動いているアクティビティに遷移する際、作りなおさずに再利用する。
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
