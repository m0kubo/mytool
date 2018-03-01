package com.insprout.okubo.mytool;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

/**
 * Created by okubo on 2018/02/13.
 * Widgetの Viewの制御をまとめたクラス
 */

public class WidgetController {

    private Class<?> mWidgetClass;
    private int mLayoutId;


    /**
     * コンストラクタ
     * @param widgetClass Widgetのクラス
     * @param layoutId WidgetのレイアウトID
     */
    public WidgetController(Class<?> widgetClass, int layoutId) {
        // ほぼ固定の値は、毎回指定しなくてもいい様にコンストラクタで先に記憶させておく
        mWidgetClass = widgetClass;
        mLayoutId = layoutId;
    }

    /**
     * Widgetの指定の Viewに対して 画像を設定する
     * @param context コンテキスト
     * @param viewId 目的のViewのID
     * @param intent 設定するIntent
     */
    public void setOnClickPendingIntent(Context context, int viewId, PendingIntent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, mWidgetClass));
        for (int appWidgetId : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);
            views.setOnClickPendingIntent(viewId, intent);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Widgetの指定の Viewに対して テキストを設定する
     * @param context コンテキスト
     * @param viewId 目的のViewのID
     * @param text 設定する文字列
     */
    public void setText(Context context, int viewId, CharSequence text) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, mWidgetClass));
        for (int appWidgetId : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);
            views.setTextViewText(viewId, text);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Widgetの指定の Viewに対して 表示状態を設定する
     * @param context コンテキスト
     * @param viewId 目的のViewのID
     * @param visibility 設定する値 (View.VISIBLE / View.INVISIBLE / View.GONE)
     */
    public void setVisibility(Context context, int viewId, int visibility) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, mWidgetClass));
        for (int appWidgetId : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);
            views.setViewVisibility(viewId, visibility);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Widgetの指定の Viewに対して 有効/無効状態を設定する
     * @param context コンテキスト
     * @param viewId 目的のViewのID
     * @param enabled 設定する値
     */
    public void setEnabled(Context context, int viewId, boolean enabled) {
        setBoolean(context, viewId, "setEnabled", enabled);
    }

    /**
     * Widgetの指定の Viewに対して 画像を設定する
     * @param context コンテキスト
     * @param viewId 目的のViewのID
     * @param resourceId 設定する画像のリソースID
     */
    public void setImageResource(Context context, int viewId, int resourceId) {
        setInt(context, viewId, "setImageResource", resourceId);
    }

    /**
     * Widgetの指定の Viewに対して 背景画像を設定する
     * @param context コンテキスト
     * @param viewId 目的のViewのID
     * @param resourceId 設定する画像のリソースID
     */
    public void setBackgroundResource(Context context, int viewId, int resourceId) {
        setInt(context, viewId, "setBackgroundResource", resourceId);
    }


    /**
     * Widgetの Viewに対して int型のパラメータで指定のメソッドを実行する
     * @param context コンテキスト
     * @param viewId 目的のViewのID
     * @param methodName 実行するメソッド
     * @param value メソッドに与えるint型の値
     */
    public void setInt(Context context, int viewId, String methodName, int value) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, mWidgetClass));
        for (int appWidgetId : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);
            views.setInt(viewId, methodName, value);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Widgetの Viewに対して boolean型のパラメータで指定のメソッドを実行する
     * @param context コンテキスト
     * @param viewId 目的のViewのID
     * @param methodName 実行するメソッド
     * @param value メソッドに与えるboolean型の値
     */
    public void setBoolean(Context context, int viewId, String methodName, boolean value) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, mWidgetClass));
        for (int appWidgetId : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);
            views.setBoolean(viewId, methodName, value);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
