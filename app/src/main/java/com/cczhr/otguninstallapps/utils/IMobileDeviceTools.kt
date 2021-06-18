package com.cczhr.otglocation.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.cczhr.otguninstallapps.bean.App
import com.cczhr.otguninstallapps.utils.Application
import com.cczhr.otguninstallapps.utils.runMainThread
import com.cczhr.otguninstallapps.utils.saveFilesDir
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * @author cczhr
 * @description  库指令
 * @since 2021/2/22
 */
class IMobileDeviceTools {
    val logStr = MutableLiveData<String>()
    var fixedThreadPool: ExecutorService = Executors.newFixedThreadPool(10)
    private val lib = "lib"
    private val bin = "bin"
    private val saveFilePath = "/data/local/tmp"
    val systemLibPath = "/system/lib"
    var process: Process? = null
    var successResult: BufferedReader? = null
    var errorResult: BufferedReader? = null
    var os: DataOutputStream? = null

    @Volatile
    var isKilling = false

    companion object {
        var DEVICE_PATH: String =
            Application.context.getExternalFilesDir(null)!!.absolutePath + File.separator + "drivers"

    }

    fun killUsbmuxd(deviceNode: String = "") {
        if (!isKilling) {
            isKilling = true
            SystemClock.sleep(1500)
            val killSystemMtp =
                if (deviceNode.isNotEmpty()) "kill `lsof  -t $deviceNode`\n" else deviceNode
            val killPort =
                "kill  `netstat -tunlp  | grep 27015|awk '{print $7} '|awk -F '/' '{print $1}'`"
            runCommand("$killSystemMtp.$saveFilePath/usbmuxd -X -v -f\n$killPort",isFinish = {
                isKilling = false
            })
        }

    }

    fun String.addLogStr() {
        logStr.value = this
    }

    fun release() {
        try {
            stopUsbmuxd {
                killUsbmuxd()
                fixedThreadPool.shutdownNow()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopUsbmuxd(disconnect: () -> Unit) {
        fixedThreadPool.execute {
            try {
                //killUsbmuxd()
                successResult?.close()
                errorResult?.close()
                os?.close()
                process?.errorStream?.close()
                process?.inputStream?.close()
                process?.outputStream?.close()
                process?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Application.context.runMainThread {
                disconnect.invoke()
            }

        }

    }

    fun listApps(
        input: ((str: String) -> Unit)? = null,
        error: ((str: String) -> Unit)? = null,
        isFinish: (() -> Unit)? = null
    ) = runCommand(".$saveFilePath/ideviceinstaller -l", input, error, isFinish)

    fun uninstallApps(
        listApps: ArrayList<App>,
        input: ((str: String) -> Unit),
        error: ((str: String) -> Unit),
        isFinish: (() -> Unit)
    ) {
        var command = ""
        val appMap = mutableMapOf<String, String>()
        listApps.forEach {
            if (it.isSelected) {
                command += ".$saveFilePath/ideviceinstaller -U ${it.PackageName}\n"
                appMap[it.PackageName] = it.name
            }
        }
        var uninstallName=""
        runCommand(command, {
            if (it.contains("Uninstalling", true) && it.contains("\'", true)) {
                val first = it.indexOfFirst { char -> char == '\'' }
                val last = it.indexOfLast { char -> char == '\'' }
                if (first != -1 && last != -1) {
                    val packageName = it.substring(first + 1, last)
                    appMap[packageName]?.let { name ->
                        uninstallName=name
                        input("正在卸载:$name")
                    }
                }
            } else if (it.contains("Complete", true)) {
                if(uninstallName.isNotEmpty()){
                    for (i in listApps.size - 1 downTo 0) {
                       if(listApps[i].name==uninstallName){
                           listApps.removeAt(i)
                           break
                       }
                    }
                    uninstallName=""
                }
                input("卸载完成")
            }
        }, {
            uninstallName=""
            error("出错:$it")
        }, isFinish)
    }


    fun startUsbmuxd(
        deviceNode: String,
        connect: () -> Unit,
        msg: (msg: String) -> Unit,
        version: (msg: String) -> Unit,
        deviceName: (msg: String) -> Unit

    ) {
        fixedThreadPool.execute {
            try {
                if (isKilling)
                    return@execute
                killUsbmuxd(deviceNode)
                process = Runtime.getRuntime().exec("su", arrayOf("LD_LIBRARY_PATH=$saveFilePath"))
                successResult = BufferedReader(InputStreamReader(process!!.inputStream))
                errorResult = BufferedReader(InputStreamReader(process!!.errorStream))
                os = DataOutputStream(process!!.outputStream)
                os?.write(".$saveFilePath/usbmuxd -v -f".toByteArray())
                os?.writeBytes("\n")
                os?.flush()
                os?.close()
                fixedThreadPool.execute {
                    try {
                        var line: String?
                        while (errorResult!!.readLine().also { line = it } != null) {
                            line?.let {
                                Application.context.runMainThread {
                                    msg(it)
                                    if (it.contains(
                                            "Finished preflight on device",
                                            true
                                        ) || it.contains("is_device_connected", true)
                                    ) {
                                        connect.invoke()
                                        runCommand(
                                            ".${saveFilePath}/ideviceinfo -k DeviceName",
                                            { dName ->
                                                deviceName.invoke(dName)

                                                runCommand(
                                                    ".${saveFilePath}/ideviceinfo -k ProductVersion",
                                                    { pVersion ->
                                                        version.invoke(pVersion)

                                                    })
                                            })
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                    }


                }
                fixedThreadPool.execute {
                    try {
                        var line: String?
                        while (successResult!!.readLine().also { line = it } != null) {
                            line?.let {
                                msg("$it\n")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                process!!.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }


    @SuppressLint("SdCardPath")
    fun installLib(context: Context, isSuccess: ((status: Boolean) -> Unit)) {
        val assetManager: AssetManager = context.assets
        val libSavePath = "${context.getExternalFilesDir(null)!!.absolutePath}/$lib"
        val binSavePath = "${context.getExternalFilesDir(null)!!.absolutePath}/$bin"
        val libPermission = StringBuilder()
        assetManager.list(lib)?.forEach {
            val fileName = "${lib}/$it"
            libPermission.append("chmod 644 $saveFilePath/$it\n")
            assetManager.open(fileName).saveFilesDir(libSavePath, it)

        }
        assetManager.list(bin)?.forEach {
            val fileName = "${bin}/$it"
            assetManager.open(fileName).saveFilesDir(binSavePath, it)
        }
        runCommand(
            "mkdir -p /sdcard/lockdown\n" +
                    "mkdir -p /sdcard/lockdown/drivers\n" +
                    "cp -rf $libSavePath/* $saveFilePath\n" +
                    "cp -rf $binSavePath/* $saveFilePath\n" +
                    "$libPermission" +
                    "chmod 777 -R $saveFilePath", isFinish = {
                isSuccess.invoke(checkInstallLib(context))
            })


    }


    fun uninstallLib(context: Context, isFinish: (() -> Unit)) {
        val assetManager: AssetManager = context.getAssets()
        val deleteCommand = StringBuilder()
        assetManager.list(lib)?.forEach {
            deleteCommand.append("rm -f $saveFilePath/$it\n")

        }
        assetManager.list(bin)?.forEach {
            deleteCommand.append("rm -f $saveFilePath/$it\n")
        }
        runCommand(
            "${deleteCommand}rm -f $saveFilePath/usbmuxd.pid\n" +
                    "rm -rf /sdcard/lockdown", isFinish = isFinish
        )
    }

    fun checkInstallLib(context: Context): Boolean {
        val assetManager: AssetManager = context.getAssets()
        assetManager.list(lib)?.forEach {
            if (!File("$saveFilePath/$it").exists())
                return false

        }
        assetManager.list(bin)?.forEach {
            if (!File("$saveFilePath/$it").exists())
                return false

        }
        return true
    }

    fun isRoot(isRoot: (value: Boolean) -> Unit) {
        try {
            val process: Process = Runtime.getRuntime().exec("su")
            process.outputStream.run {
                this.flush()
                this.close()
            }
            val code = process.waitFor()
            isRoot(code == 0)
        } catch (e: Exception) {
            isRoot(false)
        }
    }


    fun runCommand(
        cmd: String,
        input: ((str: String) -> Unit)? = null,
        error: ((str: String) -> Unit)? = null,
        isFinish: (() -> Unit)? = null
    ) {
        fixedThreadPool.execute {
            try {
                val successResult: BufferedReader
                val errorResult: BufferedReader
                val os: DataOutputStream
                val process: Process =
                    Runtime.getRuntime().exec("su", arrayOf("LD_LIBRARY_PATH=$saveFilePath"))
                successResult = BufferedReader(InputStreamReader(process.inputStream))
                errorResult = BufferedReader(InputStreamReader(process.errorStream))
                os = DataOutputStream(process.outputStream)
                os.write(cmd.toByteArray())
                os.writeBytes("\n")
                os.flush()
                os.writeBytes("exit\n")
                os.flush()
                os.close()
                fixedThreadPool.execute {
                    var line: String?
                    try {
                        while (successResult.readLine().also { line = it } != null) {
                            line?.let {
                                Application.context.runMainThread {
                                    it.addLogStr()
                                    input?.invoke(it)
                                }
                            }

                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            successResult.close()
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                fixedThreadPool.execute {
                    var line: String?
                    try {
                        while (errorResult.readLine().also { line = it } != null) {
                            line?.let {
                                Application.context.runMainThread {
                                    it.addLogStr()
                                    error?.invoke(it)

                                }
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            errorResult.close()
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                process.waitFor()
                Application.context.runMainThread {
                    isFinish?.invoke()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                process?.destroy()
            }

        }
    }
}
