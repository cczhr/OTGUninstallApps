package com.cczhr.otguninstallapps


import android.app.Dialog
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.cczhr.otguninstallapps.adapter.ListAppsAdapter
import com.cczhr.otguninstallapps.bean.App
import com.cczhr.otguninstallapps.utils.Application.Companion.libTools
import com.cczhr.otguninstallapps.utils.CommonUtil
import com.mcxtzhang.indexlib.suspension.SuspensionDecoration
import kotlinx.android.synthetic.main.activity_list_apps.*
import kotlinx.android.synthetic.main.activity_list_apps.log
import kotlinx.android.synthetic.main.activity_list_apps.text_input_layout
import kotlinx.android.synthetic.main.activity_main.*


/**
 * @author cczhr
 * @description
 * @since 2021/6/17 11:22
 */
class ListAppsActivity:BaseActivity() {
    override val layoutId: Int=R.layout.activity_list_apps
    lateinit var adapter: ListAppsAdapter
    val listApps=ArrayList<App>()
    lateinit var decoration: SuspensionDecoration
    lateinit var linearLayoutManager: LinearLayoutManager

    var dialog: Dialog? = null
    override fun init() {
        log.requestFocus()
        text_input_layout.swipeRight {
            log.setText("")
        }
        listApps.clear()
        intent.extras?.getParcelableArrayList<App>("listApps")?.let {list->
            listApps.addAll(list)
            linearLayoutManager= LinearLayoutManager(this)
            recycler_view.layoutManager=linearLayoutManager
            index_bar.setmPressedShowTextView(tv_sidebar_hint)
                .setNeedRealIndex(true)
                .setmLayoutManager(linearLayoutManager)

            index_bar.setmSourceDatas(listApps).invalidate()
            decoration= SuspensionDecoration(this, listApps)
            adapter= ListAppsAdapter(listApps, this)
            recycler_view.adapter=adapter
            recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
            recycler_view.addItemDecoration(decoration)
            adapter.notifyDataSetChanged()
        }




    }
    fun logAdd(str: String) {
        log.append(str + "\n")
        log.setSelection(log.text.toString().length)
    }

    fun uninstallSelected(view: View) {
        dialog?.dismiss()
        if(!adapter.hasSelected()){
           CommonUtil.showToast(this,R.string.please_select_an_application)
            return
        }
        dialog=CommonUtil.showProgressDialog(this,R.string.please_wait)
        libTools.uninstallApps(listApps,{
            logAdd(it)
        },{
            logAdd(it)
        },{
            dialog?.dismiss()
            adapter.notifyDataSetChanged()
        })
    }
}