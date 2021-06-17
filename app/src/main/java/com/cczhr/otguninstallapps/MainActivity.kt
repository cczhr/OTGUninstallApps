package com.cczhr.otguninstallapps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.cczhr.otglocation.utils.HotPlugTools
import com.cczhr.otglocation.utils.IMobileDeviceTools
import com.cczhr.otguninstallapps.bean.App
import com.cczhr.otguninstallapps.utils.Application
import com.cczhr.otguninstallapps.utils.Application.Companion.libTools
import com.cczhr.otguninstallapps.utils.CommonUtil
import com.cczhr.otguninstallapps.utils.dealAppData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity() {
    override val layoutId: Int = R.layout.activity_main
    lateinit var hotPlugTools: HotPlugTools
    var hasLib = false
    var isRoot = false
    var isConnected = false
    var dialog: Dialog? = null
    @SuppressLint("SetTextI18n")




    override fun init() {
        version?.text = "V ${Application.getVersion()}"
        log.requestFocus()
        hotPlugTools = HotPlugTools()
        hasLib = libTools.checkInstallLib(this)
        lib_status.text = hasLib.toString()

        libTools.isRoot {
            isRoot = it
            if (!it) {
                CommonUtil.showToast(Application.context, R.string.root_hint)
            }
            root_status.text = it.toString()
        }
        libTools.logStr.observe(this, Observer {
            logAdd(it)
        })

        hotPlugTools.register(this, { deviceNode ->
            libTools.startUsbmuxd(deviceNode, {
                logAdd("连接成功")
                isConnected = true
                connect_status.setText(R.string.connected)
            }, {
                logAdd(it)
            }, {
                product_version.text = it
            }, {
                device_name.text = it
            })
        }, {
            libTools.stopUsbmuxd {
                isConnected = false
                connect_status.setText(R.string.disconnected)
                product_version.text = ""
                device_name.text = ""

            }
        })
        text_input_layout.swipeRight {
            log.setText("")
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        libTools.release()
        hotPlugTools.unRegister(this)
    }

    fun logAdd(str: String) {
        log.append(str + "\n")
        log.setSelection(log.text.toString().length)
    }

    fun installLib(view: View) {
        libTools.installLib(this) {
            hasLib = it
            lib_status.text = hasLib.toString()
            logAdd(if (it) "组件已安装" else "组件未安装")
        }
    }

    fun uninstallLib(view: View) {
        libTools.uninstallLib(this) {
            hasLib = false
            lib_status.text = hasLib.toString()
            logAdd("组件已删除")
        }

    }




    fun about(view: View) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_help)
            .setView(R.layout.view_about)
            .show()
    }

    private fun checkStatus(): Boolean {
        if (!hasLib)
            CommonUtil.showToast(Application.context, "请安装组件!")
        else if (!isConnected)
            CommonUtil.showToast(Application.context, "请连接设备!")

        return hasLib && isConnected
    }

    fun listApps(view: View) {
        dialog?.dismiss()
        dialog=CommonUtil.getProgressDialog(this,R.string.please_wait)
        if(checkStatus()){
            val  listApps=ArrayList<App>()
            var isSuccessed=true
            libTools.listApps({
                it.dealAppData()?.let { app ->
                    listApps.add(app)
                }
            }, {
                isSuccessed = false
            }, {
                dialog?.dismiss()
                if (isSuccessed && listApps.size > 0) {
                    logAdd("app数量${listApps.size}")
                    startActivity(Intent(this, ListAppsActivity::class.java).putExtras(Bundle().apply {putParcelableArrayList("listApps",listApps)}))
                } else {
                    logAdd("app数量为空！")
                }
            })
        }



    }


}