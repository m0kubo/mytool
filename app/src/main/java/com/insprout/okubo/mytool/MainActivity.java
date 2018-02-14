package com.insprout.okubo.mytool;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.NumberPicker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final static int MAX_ADJUST_MILLI_VALUE = 50;
    final static int STEP_ADJUST_MILLI_VALUE = 1;

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
//        mFlashLight = new FlashLight(this);
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

        mButton = findViewById(R.id.fab);
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

    private void adjustScale() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.dlg_adjust_scale, null);
        final NumberPicker picker = dialogView.findViewById(R.id.np_adjust);
        picker.setMaxValue(mSelectionLabels.length - 1);
        picker.setMinValue(0);
        picker.setDisplayedValues(mSelectionLabels);
        picker.setWrapSelectorWheel(false);
        // 現在の設定値を 選択しておく
        int currentRate = Settings.getAdjustRate(this);
        for (int i=0; i<mSelectionValues.size(); i++) {
            if (mSelectionValues.get(i) == currentRate) {
                picker.setValue(i);
                break;
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.action_adjust_scale)
                .setMessage(R.string.msg_adjust_scale)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int rate = mSelectionValues.get(picker.getValue());
                        Settings.putAdjustRate(MainActivity.this, rate);
                        mAdjustRate = convertRate(rate);
                        mRulerView.setAdjustRate(mAdjustRate);
                        mRulerView.invalidate();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setView(dialogView);
        builder.show();
    }

    private float convertRate(int rate) {
        return (1.0f + rate / 1000.0f);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_adjust_scale:
                adjustScale();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.fab:
                boolean flashing = mFlashLight.toggle();

                mButton.setSelected(flashing);
                // Widgetの ボタンの状態もあわせておく
                FlashLightWidget.switchButtonImage(this, flashing);
                break;
        }
    }

}
