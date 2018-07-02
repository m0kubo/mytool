package com.insprout.okubo.mytool;

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.support.media.ExifInterface;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraCtrl {

    //////////////////////////////////////////////////////////////////////
    //
    // Interface
    //

    interface ICamera {

        void open();

        void close();

        void takePicture(File picture);

    }


    private ICamera mCameraUi = null;


    public CameraCtrl(Activity activity, View view) {
        // Previewに指定されたViewによって、Cameraクラスを使用するか、Camera2クラスを使用するか判別する
        if (view instanceof SurfaceView) {
            mCameraUi = new CameraUi(activity, (SurfaceView) view);
        } else if (view instanceof TextureView) {
            mCameraUi = new Camera2Ui(activity, (TextureView) view);
        }
    }

    public void open() {
        if (mCameraUi != null) mCameraUi.open();
    }

    public void close() {
        if (mCameraUi != null) mCameraUi.close();
    }

    public void takePicture(File filePicture) {
        if (filePicture == null) return;

        if (mCameraUi != null) mCameraUi.takePicture(filePicture);
    }


    //////////////////////////////////////////////////////////////////////
    //
    // Utilityメソッド
    //

    /**
     * バイト列をjpegファイルとして指定の Fileに書き出し、Content管理DBに登録する
     * また、exif情報で 画像の向きも付加する
     *
     * @param context         コンテキスト
     * @param file            出力先ファイル
     * @param data            画像データ
     * @param exifOrientation exifの画像向き情報。負の値が指定された場合はexif情報は付加しない
     */
    public static void savePhoto(Context context, File file, byte[] data, int exifOrientation) {
        if (file == null || data == null) return;

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(data);
        } catch (IOException ignored) {
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException ignored) {
            }
        }

        if (exifOrientation >= 0) {
            // 画像の回転情報をつけておく
            try {
                ExifInterface exif = new ExifInterface(file.getPath());
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(exifOrientation));
                exif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (context != null) {
            MediaScannerConnection.scanFile(
                    context,
                    new String[]{file.getAbsolutePath()},
                    null,
                    null);

            Toast.makeText(context, "撮影完了: " + file.getPath(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 端末の向きから、カメラの補正角度(degree)を返す
     *
     * @param displayRotation 端末の向き。Display#getRotation()で得られる値
     * @return カメラの補正角度(degree)
     */
    public static int getRotationDegree(int displayRotation) {
        switch (displayRotation) {
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


    /**
     * 端末の向きから、画像の向き(ExifInterface設定値)を返す
     * ( getRotationDegree()と同等のメソッドだが、こちらは角度ではなく EXIF用の設定を返す )
     *
     * @param displayRotation 端末の向き。Display#getRotation()で得られる値
     * @return 画像の向き(ExifInterface設定値)
     */
    public static int getExifOrientation(int displayRotation) {
        switch (displayRotation) {
            // 反時計回りに 90度 (横)
            case Surface.ROTATION_90:
                return ExifInterface.ORIENTATION_NORMAL;

            // 時計回りに 90度 (横)
            case Surface.ROTATION_270:
                return ExifInterface.ORIENTATION_ROTATE_180;

            // 180度 (上下逆さま)
            case Surface.ROTATION_180:
                return ExifInterface.ORIENTATION_ROTATE_270;

            // 正位置 (縦)
            case Surface.ROTATION_0:
            default:
                return ExifInterface.ORIENTATION_ROTATE_90;
        }
    }

}
