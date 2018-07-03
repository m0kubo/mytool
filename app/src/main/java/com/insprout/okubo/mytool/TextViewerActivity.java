package com.insprout.okubo.mytool;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.insprout.okubo.mytool.util.SdkUtils;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;


public class TextViewerActivity extends AppCompatActivity implements DialogUi.DialogEventListener {
    private final static int REQ_DLG_CHAR_SET = 101;
    private final static int REQ_DLG_FONT_SIZE = 102;

    private final static String CHARSET_UTF8 = "UTF-8";
    private final static String CHARSET_SJIS = "SHIFT_JIS";
    private final static String CHARSET_JIS = "ISO-2022-JP";
    private final static String CHARSET_EUC_JP = "EUC-JP";

    private Uri mFileUri;
    private TextView mTextView;
    private String mCharSet = null;
    private float mSpFontSize = 18.0f;
    private final String[] mCharSetArray = {
            CHARSET_UTF8,
            CHARSET_SJIS,
            CHARSET_JIS,
            CHARSET_EUC_JP
    };
    private final Float[] mFontSizeArray = {
            13.0f,
            18.0f,
            22.0f
    };
    private final static String[] PERMISSIONS_READ_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private final int REQUEST_PERMISSION_ACCESS_STORAGE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        initVars(getIntent());
        initView();

        viewFile();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // このアクティビティは SINGLE_TOPで起動されているため 既にこのアクティビティが起動している状態で
        // startActivityされた場合、onCreate()メソッドは呼び出されない。そのためここでパラメータのチェックする
        // ただし、このアクティビティが初めて起動される場合(onCreate()メソッドが呼び出される場合)は、このonNewIntent()は呼ばれない
        initVars(intent);
        viewFile();
    }

    private void initVars(Intent intent) {
        mFileUri = intent.getData();
        mCharSet = null;
        float fontSize = Settings.getFontSize(this);
        if (fontSize > 4.0f) mSpFontSize = fontSize;
    }

    private void initView() {
        mTextView = findViewById(R.id.tv_viewer);
        setFontSize(mSpFontSize);
    }


    private void viewFile() {
        // Runtimeパーミッションの確認
        if (!SdkUtils.requestRuntimePermissions(this, PERMISSIONS_READ_STORAGE, REQUEST_PERMISSION_ACCESS_STORAGE)) return;

        if (mFileUri != null) {
            // 暗黙的Intentでファイルが指定された
            viewFileDelayed(mFileUri, mCharSet);
            Settings.putFileUri(TextViewerActivity.this, mFileUri);

        } else {
            // 表示ファイルが指定されていないので、前回表示したファイルを開く
            mFileUri = Settings.getFileUri(TextViewerActivity.this);
            if (mFileUri != null) {
                viewFileDelayed(mFileUri, mCharSet);

            } else {
                // 指定ファイルなし
                Toast.makeText(TextViewerActivity.this, R.string.toast_no_file_specified, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void viewFileDelayed(final Uri fileUri, final String charSet) {
        String msg = getString(R.string.toast_view_fmt, "");
        final DialogFragment mProgress = new DialogUi.Builder(this, DialogUi.STYLE_PROGRESS_DIALOG)
                .setTitle(msg)
                .setMessage(mFileUri.toString())
                .show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewFile(fileUri, charSet);
                mProgress.dismiss();
            }
        }, 100);
    }

    private void viewFile(Uri fileUri, String charSet) {
        mTextView.setText(null);

        InputStream inputStream = getInputStream(fileUri);
        if (inputStream == null) return;

        if (charSet != null) {
            mCharSet = charSet;
        } else {
            mCharSet = detectCharSet(fileUri);
            if (mCharSet == null) mCharSet = CHARSET_UTF8;
        }

        StringBuilder builder = new StringBuilder();
        // try-with-resources で BufferedReaderの close()を自動呼出しさせる
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, mCharSet))) {
            int lineCount = 0;
            String line;
            while ((line = br.readLine()) != null) {            //1行ごとに読み込む
                if (lineCount++ > 0) builder.append("\n");
                builder.append(line);
            }

        } catch (IOException e) {
            return;
        }

        mTextView.setText(builder.toString());

    }

    private String detectCharSet(Uri uri) {
        if (uri == null) return null;

        // 文字コード判定ライブラリの実装
        UniversalDetector detector = new UniversalDetector(null);

        // try-with-resources で InputStreamの close()を自動呼出しさせる
        try (InputStream fis = getInputStream(uri)) {
            byte[] buf = new byte[4096];
            int size;
            while ((size = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, size);
            }
            detector.dataEnd();

        } catch (IOException e) {
            return null;
        }

        return detector.getDetectedCharset();
    }

    InputStream getInputStream(Uri uri) {
        if (uri == null) return null;

        String scheme = uri.getScheme();
        if (scheme == null) return null;

        try {
            switch (scheme) {
                case "file":
                    String path = uri.getPath();
                    if (path != null) {
                        return new FileInputStream(path);
                    }
                    break;

                case "content":
                    return getContentResolver().openInputStream(uri);
            }

        } catch (FileNotFoundException e) {
            return null;
        }

        return null;
    }


    private void setFontSize(float fontSize) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mTextView.setTextSize(fontSize * metrics.scaledDensity / metrics.density);
    }

    private String getFontSizeLabel(float fontSize) {
        return getString(R.string.label_font_size_fmt, fontSize);
    }

    private void changeFontSize() {
        int selected = Arrays.asList(mFontSizeArray).indexOf(mSpFontSize);
        // フォントサイズ設定値リストから、選択用(表示用)文字列リストを作成する
        String[] arrayLabels = new String[ mFontSizeArray.length ];
        for (int i=0; i<mFontSizeArray.length; i++) {
            arrayLabels[i] = getFontSizeLabel(mFontSizeArray[i]);
        }
        new DialogUi.Builder(this)
                .setTitle(R.string.menu_font_size)
                .setSingleChoiceItems(arrayLabels, selected)
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(REQ_DLG_FONT_SIZE)
                .show();
    }

    private void changeCharSet() {
        int selected = Arrays.asList(mCharSetArray).indexOf(mCharSet);
        new DialogUi.Builder(this)
                .setTitle(R.string.menu_char_set)
                .setSingleChoiceItems(mCharSetArray, selected)
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(REQ_DLG_CHAR_SET)
                .show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case REQUEST_PERMISSION_ACCESS_STORAGE:
                // PERMISSIONが すべて付与されたか確認する
                if (!SdkUtils.isGranted(grantResults)) {
                    // 必要な PERMISSIONは付与されなかった
                    finish();
                    return;
                }

                viewFile();
                break;
        }
    }


    /////////////////////////////////////////////////////////////////////////
    //
    // Dialog 関連
    //

    // AlertDialogが DialogFragmentでの使用が推奨されるようになった為、AlertDialogの Listenerは Activityに implementsして使用する事。
    // そうしないと、(メモリ枯渇などによる)Fragmentの再作成時に Listenerが参照されなくなる。

    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        switch (requestCode) {
            case REQ_DLG_FONT_SIZE:
                // which には ボタンID (DialogInterface.BUTTON_NEGATIVE : -2)などもくるので注意
//                if (which >= 0 && which < mFontSizeArray.length) {
//                    // フォントサイズが変更された
//                    mSpFontSize = mFontSizeArray[which];
//                    Settings.putFontSize(getApplicationContext(), mSpFontSize);
//                    setFontSize(mSpFontSize);
//                }
//                if (dialog != null) dialog.dismiss();
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    int pos = ((ListView)view).getCheckedItemPosition();
                    mSpFontSize = mFontSizeArray[pos];
                    Settings.putFontSize(getApplicationContext(), mSpFontSize);
                    setFontSize(mSpFontSize);
                }
                break;

            case REQ_DLG_CHAR_SET:
                // which には ボタンID (DialogInterface.BUTTON_NEGATIVE : -2)などもくるので注意
//                if (which >= 0 && which < mCharSetArray.length) {
//                    // charSetが変更された
//                    mCharSet = mCharSetArray[which];
//                    if (mFileUri != null) {
//                        viewFile(mFileUri, mCharSet);
//                    }
//                }
//                if (dialog != null) dialog.dismiss();
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    // charSetが変更された
                    int pos = ((ListView)view).getCheckedItemPosition();
                    String charSet = ((ListView)view).getItemAtPosition(pos).toString();
                    Log.d("dialog", "item: " + charSet);
                    mCharSet = mCharSetArray[pos];
                    if (mFileUri != null) {
                        viewFile(mFileUri, mCharSet);
                    }
                }
                break;
        }
    }


    /////////////////////////////////////////////////////////////////////////
    //
    // menu関連
    //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_viewer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem;

        if ((menuItem = menu.findItem(R.id.action_char_set)) != null) {
            String label = getString(R.string.menu_char_set);
            if (mCharSet != null) {
                label += mCharSet;
                menuItem.setEnabled(true);
            } else {
                menuItem.setEnabled(false);
            }
            menuItem.setTitle(label);
        }

        if ((menuItem = menu.findItem(R.id.action_font_size)) != null) {
            menuItem.setTitle(getString(R.string.menu_font_size) + getFontSizeLabel(mSpFontSize));
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        switch(menuId) {
            // backボタン
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_font_size:
                changeFontSize();
                return true;

            case R.id.action_char_set:
                changeCharSet();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public static void startActivity(Context context) {
        Intent intent = new Intent(context, TextViewerActivity.class);
        // FLAG_ACTIVITY_CLEAR_TOP: 遷移先のアクティビティが既に動いていればそのアクティビティより上にあるアクティビティを消す。
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // FLAG_ACTIVITY_SINGLE_TOP: 既に動いているアクティビティに遷移する際、作りなおさずに再利用する。
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
