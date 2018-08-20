package com.insprout.okubo.mytool;

import android.app.Activity;
import android.support.media.ExifInterface;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EventListener;

public abstract class CameraCtrl {

    //////////////////////////////////////////////////////////////////////
    //
    // Interface
    //

    public interface TakePictureListener extends EventListener {
        void onTakePicture(boolean result);
    }

    //////////////////////////////////////////////////////////////////////
    //
    // abstract 宣言
    //

    abstract void open();

    abstract void close();

    abstract void takePicture(File picture, TakePictureListener listener);


    //////////////////////////////////////////////////////////////////////
    //
    // Utilityメソッド
    //

    public static CameraCtrl newInstance(Activity activity, View view) {
        // Previewに指定されたViewによって、Cameraクラスを使用するか、Camera2クラスを使用するか判別する
        if (view instanceof SurfaceView) {
            return new CameraUi(activity, (SurfaceView) view);
        } else if (view instanceof TextureView) {
            return new Camera2Ui(activity, (TextureView) view);
        }
        return null;
    }

    //////////////////////////////////////////////////////////////////////
    //
    // 共通メソッド
    //

    /**
     * バイト列をjpegファイルとして指定の Fileに書き出す (exif情報は付加しない)
     * @param file            出力先ファイル
     * @param data            画像データ
     * @param listener        写真保存リスナー
     */
    protected void savePhoto(File file, byte[] data, TakePictureListener listener) {
        savePhoto(file, data, -1, listener);
    }

    /**
     * バイト列をjpegファイルとして指定の Fileに書き出す
     * また、exif情報で 画像の向きも付加する
     * @param file            出力先ファイル
     * @param data            画像データ
     * @param displayRotation 端末の向き。Display#getRotation()で得られる値
     * @param listener        写真保存リスナー
     */
    protected void savePhoto(File file, byte[] data, int displayRotation, TakePictureListener listener) {
        boolean result = false;

        if (file != null && data != null) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(data);
                result = true;
            } catch (IOException ignored) {
                result = false;
            }

            if (result && displayRotation >= 0) {
                // 画像の回転情報をつけておく
                try {
                    ExifInterface exif = new ExifInterface(file.getPath());
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(getExifOrientation(displayRotation)));
                    exif.saveAttributes();
                } catch (IOException ignored) {
                }
            }
            if (!result) file.delete();
        }

        if (listener != null) listener.onTakePicture(result);
    }


    /**
     * 端末の向きから、画像の向き(ExifInterface設定値)を返す
     * ( getRotationDegree()と同等のメソッドだが、こちらは角度ではなく EXIF用の設定を返す )
     *
     * @param displayRotation 端末の向き。Display#getRotation()で得られる値
     * @return 画像の向き(ExifInterface設定値)
     */
    private int getExifOrientation(int displayRotation) {
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

    /**
     * 端末の向きから、カメラの補正角度(degree)を返す
     *
     * @param displayRotation 端末の向き。Display#getRotation()で得られる値
     * @return カメラの補正角度(degree)
     */
    protected int getRotationDegree(int displayRotation) {
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
     *  (長寸 / 短寸)の値を返す
     *  どちらかに 0以下の値が指定された場合は 0を返す
     *  (よって結果は 0もしくは 1.0以上の正の数となる)
     *  @param width 幅
     *  @param height 高さ
     *  @return 長寸/短寸
     */
    protected float wideRatio(int width, int height) {
        if (width <= 0 || height <= 0) return 0f;
        return (float)Math.max(width, height) / (float)Math.min(width, height);
    }


    /**
     * 指定された2つのサイズの縦横比が同じかどうかを判別する
     * 計算誤差を鑑み、差が1%以内なら同一比率とみなす
     * どちらかに 0が指定された場合は、不正な比率が指定されたと見做し falseを返す
     *  @param ratio1 比較される比率
     *  @param ratio2 比較する比率
     *  @return 結果
     */
    protected boolean isRatioEqual(float ratio1, float ratio2) {
        if (ratio1 <= 0f || ratio2 <= 0f) return false;
        float rate = ratio1 / ratio2;
        return (rate >= 0.99f && rate <= 1.01f);
    }

}
