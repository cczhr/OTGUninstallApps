package com.cczhr.otguninstallapps.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.StringRes



/**
 * @author cczhr
 * @description
 * @since 2020/10/19 10:19
 */
open class CommonUtil {
    companion object {
        fun showProgressDialog(@NonNull context: Context, @StringRes messageId: Int): Dialog? {
            val progressDialog = ProgressDialog(context)
            progressDialog.setCancelable(false)
            progressDialog.setMessage(context.getString(messageId))
            progressDialog.show()
            return progressDialog
        }
        private var mToast: Toast? = null
        @SuppressLint("ShowToast")
        fun showToast(@NonNull context: Context?, content: String?) {
            if (mToast == null) {
                mToast = Toast.makeText(context, content, Toast.LENGTH_SHORT)
            } else {
                mToast?.setText(content)
            }
            mToast?.setGravity(Gravity.CENTER, 0, 0)
            mToast?.show()
        }

        @SuppressLint("ShowToast")
        fun showToast(@NonNull context: Context?, @StringRes stringId: Int) {
            if (mToast == null) {
                mToast = Toast.makeText(context, stringId, Toast.LENGTH_SHORT)
            } else {
                mToast?.setText(stringId)
            }
            mToast?.setGravity(Gravity.CENTER, 0, 0)
            mToast?.show()
        }

        fun setOnEnterClickListener(
            target: View,
            listener: (view:View  ,content: String  )->Unit
        ) {
            target.setOnKeyListener { v: View, keyCode: Int, event: KeyEvent ->
                if (event.action == KeyEvent.ACTION_UP) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            listener.invoke(
                                v,
                                if (v is TextView) v.text.toString() else ""
                            )
                            return@setOnKeyListener true
                        }
                        else -> {
                        }
                    }
                }
                false
            }
        }
        fun getProgressDialog(@NonNull context: Context, @StringRes messageId: Int ): ProgressDialog {
            val progressDialog = ProgressDialog(context)
            progressDialog.setCancelable(false)
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage(context.getString(messageId))
            progressDialog.max=100
            progressDialog.setProgress(0);
            progressDialog.show()
            return progressDialog
        }



    }
}