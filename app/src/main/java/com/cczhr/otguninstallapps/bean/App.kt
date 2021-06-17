package com.cczhr.otguninstallapps.bean

import android.os.Parcelable
import com.mcxtzhang.indexlib.IndexBar.bean.BaseIndexPinyinBean
import kotlinx.android.parcel.Parcelize

/**
 * @author cczhr
 * @description
 * @since 2021/6/17 10:13
 */
@Parcelize
data class App(var PackageName:String="", var version:String="", var name:String="", var isSelected:Boolean=false): BaseIndexPinyinBean(), Parcelable{

    constructor(data:List<String>) :this(data[0].replace("\"","").replace(" ",""),data[1].replace("\"","").replace(" ",""),data[2].replace("\"","").replace(" ",""),false)


    constructor(app:App) :this(app.PackageName,app.version,app.name,false)

    override fun getTarget(): String {
        return name
    }

    override fun isShowSuspension(): Boolean {
        return true
    }


}
