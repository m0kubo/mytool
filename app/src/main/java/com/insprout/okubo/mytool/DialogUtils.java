package com.insprout.okubo.mytool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by okubo on 2018/03/01.
 * AlertDialogを利用するための ユーティリティクラス
 */

public class DialogUtils {
    /**
     * 引数受け渡し用のキー
     */
    private static final String KEY_REQUEST_CODE = "dialog.REQUEST_CODE";
    private static final String KEY_DIALOG_TITLE = "dialog.TITLE";
    private static final String KEY_DIALOG_TEXT = "dialog.TEXT";
    private static final String KEY_DIALOG_LAYOUT = "dialog.LAYOUT_ID";
    private static final String KEY_DIALOG_POSITIVE_BUTTON_TEXT = "button.positive.TEXT";
    private static final String KEY_DIALOG_NEGATIVE_BUTTON_TEXT = "button.negative.TEXT";
    private static final String KEY_DIALOG_NEUTRAL_BUTTON_TEXT = "button.neutral.TEXT";
    private static final String KEY_CHOICE_ARRAY = "choice.ARRAY";
    private static final String KEY_CHOICE_SELECTED = "choice.SELECTED";

    public static final int RC_NO_LISTENER = -1;
    public static final int EVENT_BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
    public static final int EVENT_BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;
    public static final int EVENT_BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL;
    public static final int EVENT_DIALOG_CREATED = -100;

    private static final int ID_STRING_DEFAULT_OK = android.R.string.ok;
    private static final int ID_STRING_DEFAULT_CANCEL = android.R.string.cancel;

    private static final String TAG_PREFIX = "DIALOG_";


    //////////////////////////
    //
    // interface
    //

    public interface DialogEventListener {
        /**
         * AlertDialogの イベントを通知する
         * Listenerに渡される objResponseは Dialogの形態によって内容が異なる
         * - メッセージと ダイアログ標準ボタンのみの場合、常にnull
         * - 選択リスト(Single Choice)が指定されている場合、選択リストの ListViewオブジェクト
         * - カスタムViewが 設定されている場合、その Viewオブジェクト
         */
        void onDialogEvent(int requestCode, AlertDialog dialog, int which, Object objResponse);
    }


    ////////////////////////////////////
    //
    // ボタンと メッセージのみのダイアログ

    /**
     * OK/Cancel/Neutralの ボタンを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param message メッセージ文字列 (表示しない場合は nullを指定する)
     * @param labelOk OKボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param labelCancel Cancelボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param labelNeutral Neutralボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showButtonsDialog(Activity activity, String title, String message, String labelOk, String labelCancel, String labelNeutral, int requestCode) {
        DialogFragment dialog = BaseDialogFragment.newInstance(requestCode, title, message, labelOk, labelCancel, labelNeutral);
        dialog.show(activity.getFragmentManager(), TAG_PREFIX + Integer.toHexString(requestCode));
        return dialog;
    }

    /**
     * OKボタンのみの メッセージダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param messageId メッセージのリソースID (表示しない場合は 0を指定する)
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showOkDialog(Activity activity, int titleId, int messageId) {
        return showOkCancelDialog(activity, titleId, messageId, ID_STRING_DEFAULT_OK, 0, RC_NO_LISTENER);
    }

    /**
     * OK/Cancelの ボタンを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param messageId メッセージのリソースID (表示しない場合は 0を指定する)
     * @param okTextId OKボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param cancelTextId キャンセルボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showOkCancelDialog(Activity activity, int titleId, int messageId, int okTextId, int cancelTextId, int requestCode) {
        String title = getString(activity, titleId);
        String message = getString(activity, messageId);
        String labelOk = getString(activity, okTextId);
        String labelCancel = getString(activity, cancelTextId);
        return showButtonsDialog(activity, title, message, labelOk, labelCancel, null, requestCode);
    }


    ////////////////////////////////////
    //
    // ボタンと Item選択のダイアログ
    //

    /**
     * Item選択(単一選択)リストを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param list 選択(単一選択)リスト
     * @param selected 初期選択番号 (未選択の場合は-1を設定する)
     * @param labelOk OKボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param labelCancel Cancelボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showSelectDialog(final Activity activity, String title, String[] list, int selected, String labelOk, String labelCancel, int requestCode) {
        DialogFragment dialog = BaseDialogFragment.newInstance(requestCode, title, list, selected, labelOk, labelCancel);
        dialog.show(activity.getFragmentManager(), TAG_PREFIX + Integer.toHexString(requestCode));
        return dialog;
    }

    /**
     * Item選択(単一選択)リストを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param list 選択(単一選択)リスト
     * @param selected 初期選択番号 (未選択の場合は-1を設定する)
     * @param okTextId OKボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param cancelTextId キャンセルボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showSelectDialog(final Activity activity, int titleId, String[] list, int selected, int okTextId, int cancelTextId, int requestCode) {
        String title = getString(activity, titleId);
        String labelOk = getString(activity, okTextId);
        String labelCancel = getString(activity, cancelTextId);
        return showSelectDialog(activity, title, list, selected, labelOk, labelCancel, requestCode);
    }


    ////////////////////////////////////
    //
    // カスタムレイアウトのダイアログ
    //

    /**
     * カスタムレイアウトで Viewを指定したダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param message メッセージ文字列 (表示しない場合は nullを指定する)
     * @param layoutId カスタムレイアウトのリソースID
     * @param labelOk OKボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param labelCancel Cancelボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showCustomDialog(final Activity activity, String title, String message, int layoutId, String labelOk, String labelCancel, int requestCode) {
        DialogFragment dialog = BaseDialogFragment.newInstance(requestCode, title, message, layoutId, labelOk, labelCancel);
        dialog.show(activity.getFragmentManager(), TAG_PREFIX + Integer.toHexString(requestCode));
        return dialog;
    }

    /**
     * カスタムレイアウトで Viewを指定したダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param messageId メッセージのリソースID (表示しない場合は 0を指定する)
     * @param layoutId カスタムレイアウトのリソースID
     * @param okTextId OKボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param cancelTextId キャンセルボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showCustomDialog(final Activity activity, int titleId, int messageId, int layoutId, int okTextId, int cancelTextId, int requestCode) {
        String title = getString(activity, titleId);
        String message = getString(activity, messageId);
        String buttonOk = getString(activity, okTextId);
        String buttonCancel = getString(activity, cancelTextId);
        return showCustomDialog(activity, title, message, layoutId, buttonOk, buttonCancel, requestCode);
    }



    ////////////////////////////////////
    //
    // 基本DialogFragmentクラス
    //

    public static class BaseDialogFragment extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

        private AlertDialog mAlertDialog = null;
        private View mCustomView = null;
        private DialogEventListener mListener = null;
        private int mRequestCode;
        private String[] mChoiceList;


        /**
         * メッセージと 標準ボタンのみのDialogを作成する
         * @param requestCode コールバック用Listenerに返す識別値。どのダイアログからのコールバックかを識別する
         * @param title Dialogのタイトル
         * @param message Dialogのメッセージ
         * @param labelOk Positiveボタンの表記
         * @param labelCancel Negativeボタンの表記
         * @param labelNeutral Neutralボタンの表記
         * @return DialogFragmentのインスタンス
         */
        public static BaseDialogFragment newInstance(int requestCode, String title, String message, String labelOk, String labelCancel, String labelNeutral) {
            BaseDialogFragment dialogFragment = new BaseDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_REQUEST_CODE, requestCode);
            args.putString(KEY_DIALOG_TITLE, title);
            args.putString(KEY_DIALOG_TEXT, message);
            args.putString(KEY_DIALOG_POSITIVE_BUTTON_TEXT, labelOk);
            args.putString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT, labelCancel);
            args.putString(KEY_DIALOG_NEUTRAL_BUTTON_TEXT, labelNeutral);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        /**
         * SingleChoiceリストを持つDialogを作成する
         * @param requestCode コールバック用Listenerに返す識別値。どのダイアログからのコールバックかを識別する
         * @param title Dialogのタイトル
         * @param selectArray 選択用リスト
         * @param selected 初期選択位置。未選択の場合は -1を渡す
         * @param labelOk Positiveボタンの表記
         * @param labelCancel Negativeボタンの表記
         * @return DialogFragmentのインスタンス
         */
        public static BaseDialogFragment newInstance(int requestCode, String title, String[] selectArray, int selected, String labelOk, String labelCancel) {
            BaseDialogFragment dialogFragment = new BaseDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_REQUEST_CODE, requestCode);
            args.putString(KEY_DIALOG_TITLE, title);
            args.putStringArray(KEY_CHOICE_ARRAY, selectArray);
            args.putInt(KEY_CHOICE_SELECTED, selected);
            args.putString(KEY_DIALOG_POSITIVE_BUTTON_TEXT, labelOk);
            args.putString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT, labelCancel);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        /**
         * Layoutを指定してDialogを作成する
         * @param requestCode コールバック用Listenerに返す識別値。どのダイアログからのコールバックかを識別する
         * @param title Dialogのタイトル
         * @param message Dialogのメッセージ
         * @param layoutId カスタムViewの layout ID
         * @param labelOk Positiveボタンの表記
         * @param labelCancel Negativeボタンの表記
         * @return DialogFragmentのインスタンス
         */
        public static BaseDialogFragment newInstance(int requestCode, String title, String message, int layoutId, String labelOk, String labelCancel) {
            BaseDialogFragment dialogFragment = new BaseDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_REQUEST_CODE, requestCode);
            args.putString(KEY_DIALOG_TITLE, title);
            args.putString(KEY_DIALOG_TEXT, message);
            args.putInt(KEY_DIALOG_LAYOUT, layoutId);
            args.putString(KEY_DIALOG_POSITIVE_BUTTON_TEXT, labelOk);
            args.putString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT, labelCancel);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            if (activity instanceof DialogEventListener) {
                mListener = (DialogEventListener) activity;
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mRequestCode = getArguments().getInt(KEY_REQUEST_CODE);
            mChoiceList = getArguments().getStringArray(KEY_CHOICE_ARRAY);
            int selected = getArguments().getInt(KEY_CHOICE_SELECTED, 0);
            String title = getArguments().getString(KEY_DIALOG_TITLE);
            String message = getArguments().getString(KEY_DIALOG_TEXT);
            String buttonOk = getArguments().getString(KEY_DIALOG_POSITIVE_BUTTON_TEXT);
            String buttonCancel = getArguments().getString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT);
            String buttonNeutral = getArguments().getString(KEY_DIALOG_NEUTRAL_BUTTON_TEXT);
            int layoutId = getArguments().getInt(KEY_DIALOG_LAYOUT, -1);
            LayoutInflater inflater;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if (title != null) builder.setTitle(title);
            if (layoutId != -1 && (inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)) != null) {
                // カスタムViewを設定
                mCustomView = inflater.inflate(layoutId, null);
                builder.setView(mCustomView);

            } else if (mChoiceList != null) {
                // 選択リストを設定
                builder.setSingleChoiceItems(mChoiceList, selected, this);
                // setMessage()を実行すると setSingleChoiceItems()が無視されるので、
                // setSingleChoiceItems()を行った場合は、setMessage()を明示的にスキップする様にする。
                message = null;
            }
            if (message != null) builder.setMessage(message);   // messageと カスタムViewは両立する

            // ダイアログボタンを設定
            if (buttonOk != null) builder.setPositiveButton(buttonOk, this);
            if (buttonCancel != null) builder.setNegativeButton(buttonCancel, this);
            if (buttonNeutral != null) builder.setNeutralButton(buttonNeutral, this);
            mAlertDialog = builder.create();

            mAlertDialog.setCanceledOnTouchOutside(false);
            // Dialogが createされた事をListenerに通知する。(主にカスタムViewの初期化処理のため)
            callbackToListener(EVENT_DIALOG_CREATED);

            return mAlertDialog;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            callbackToListener(which);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            callbackToListener(EVENT_BUTTON_NEGATIVE);
        }

        private void callbackToListener(int which) {
            if (mListener == null) return;

            Object objCallback = null;
            // Dialogの形態によって、callbackで返す オブジェクトを切り替える
            if (mCustomView != null) {
                // カスタムlayoutが指定されている場合は、そのViewを返す
                objCallback = mCustomView;

            } else if (mChoiceList != null) {
                objCallback = mAlertDialog.getListView();
            }
            mListener.onDialogEvent(mRequestCode, mAlertDialog, which, objCallback);
        }
    }

    //////////////////////////
    //
    // private functions

    private static String getString(Context context, int resourceId) {
        if (resourceId <= 0) return null;
        return context.getString(resourceId);
    }

}
