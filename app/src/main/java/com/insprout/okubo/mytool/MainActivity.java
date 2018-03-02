package com.insprout.okubo.mytool;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.NumberPicker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DialogUtils.DialogEventListener {

    private final static int REQ_ADJUST_SCALE = 100;

    private final static int MAX_ADJUST_MILLI_VALUE = 50;
    private final static int STEP_ADJUST_MILLI_VALUE = 1;

    private FlashLight mFlashLight;
    private RulerView mRulerView;
    private View mButton;

    private float mAdjustRate;
    private List<Integer> mSelectionValues;
    private String[] mSelectionLabels;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVars();
        initView();
    }


    private void initVars() {
        mFlashLight = FlashLight.getInstance(this);

        mAdjustRate = convertRate(Settings.getAdjustRate(this));
        // ものさし補正用の 選択リストを作成しておく
        mSelectionValues = new ArrayList<>();
        for (int i=-MAX_ADJUST_MILLI_VALUE; i<=MAX_ADJUST_MILLI_VALUE; i += STEP_ADJUST_MILLI_VALUE) {
            mSelectionValues.add(i);
        }
        int size = mSelectionValues.size();
        mSelectionLabels = new String[ size ];
        for (int i=0; i<size; i++) {
            mSelectionLabels[i] = getString(R.string.fmt_adjust_scale, mSelectionValues.get(i) / 10.0f);
        }
    }

    private void initView() {
        mRulerView = findViewById(R.id.v_ruler);
        mRulerView.setLineColor(Color.BLUE);
        mRulerView.setTextColor(Color.GRAY);
        mRulerView.setAdjustRate(mAdjustRate);

        mButton = findViewById(R.id.btn_flash);
        mButton.setSelected(mFlashLight.isFlashing());
    }

    @Override
    protected void onDestroy() {
        // Cameraオブジェクトをリリースしておく
        mFlashLight.release();
        // Widgetの ボタンの状態もあわせておく
        FlashLightWidget.switchButtonImage(this, false);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    private float convertRate(int rate) {
        return (1.0f + rate / 1000.0f);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_adjust_scale:
                // スケール補正ダイアログを表示する
                DialogUtils.showCustomDialog(
                        this,
                        R.string.action_adjust_scale,
                        R.string.msg_adjust_scale,
                        R.layout.dlg_adjust_scale,
                        android.R.string.ok,
                        android.R.string.cancel,
                        REQ_ADJUST_SCALE);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btn_flash:
                boolean flashing = mFlashLight.toggle();

                mButton.setSelected(flashing);
                // Widgetの ボタンの状態もあわせておく
                FlashLightWidget.switchButtonImage(this, flashing);
                break;

            case R.id.btn_viewer:
                TextViewerActivity.startActivity(this);
                break;
        }
    }

    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, Object obj) {
        switch (requestCode) {
            case REQ_ADJUST_SCALE :
                switch (which) {
                    // カスタムDialog 作成イベント
                    case DialogUtils.EVENT_DIALOG_CREATED:
                        // 子Viewなどの初期化
                        if (obj instanceof View) {
                            // Pickerの選択肢を設定する
                            NumberPicker picker = ((View)obj).findViewById(R.id.np_adjust);
                            picker.setMaxValue(mSelectionLabels.length - 1);
                            picker.setMinValue(0);
                            picker.setDisplayedValues(mSelectionLabels);
                            picker.setWrapSelectorWheel(false);
                            // 現在の設定値を 選択しておく
                            int index = mSelectionValues.indexOf(Settings.getAdjustRate(this));
                            if (index >= 0) picker.setValue(index);
                        }
                        break;

                    // OKボタン押下
                    case DialogUtils.EVENT_BUTTON_POSITIVE:
                        if (obj instanceof View) {
                            NumberPicker picker = ((View)obj).findViewById(R.id.np_adjust);
                            int rate = mSelectionValues.get(picker.getValue());
                            Settings.putAdjustRate(MainActivity.this, rate);
                            mAdjustRate = convertRate(rate);
                            mRulerView.setAdjustRate(mAdjustRate);
                            mRulerView.invalidate();
                        }
                        break;
                }
                break;
        }
    }
}
