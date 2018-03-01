package com.insprout.okubo.mytool;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Implementation of App Widget functionality.
 */
public class FlashLightWidget extends AppWidgetProvider {
    private final static String TAG = "widget";
    private final static String ACTION_CLICK = "ACTION_CLICK";


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate()");

        Intent intent = new Intent(ACTION_CLICK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        getWidgetController().setOnClickPendingIntent(context, R.id.button, pendingIntent);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "onReceive() : action=" + intent.getAction());

        if (ACTION_CLICK.equals(intent.getAction())) {
            // FlashLight点灯/消灯
//            boolean flashing = FlashLight.getInstance(context.getApplicationContext()).toggle();
            FlashLight flash = FlashLight.getInstance(context);
            boolean flashing = flash.toggle();
            // FlashLightの状態を ボタンに反映
            switchButtonImage(context, flashing);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    // 他 Activityなどから呼べる staticメソッド
    //

    public static WidgetController getWidgetController() {
        return new WidgetController(FlashLightWidget.class, R.layout.widget_flash_light);
    }

    public static void switchButtonImage(Context context, boolean flashing) {
        // ボタンの foreground、background画像を設定
        getWidgetController().setImageResource(context, R.id.button, flashing ? R.mipmap.ic_light_on : R.mipmap.ic_light_off);
        getWidgetController().setBackgroundResource(context, R.id.button, flashing ? R.drawable.bg_circle_on : R.drawable.bg_circle_off);
    }

}

