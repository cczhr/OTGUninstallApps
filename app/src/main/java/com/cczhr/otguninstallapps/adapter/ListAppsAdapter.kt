package com.cczhr.otguninstallapps.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cczhr.otguninstallapps.R
import com.cczhr.otguninstallapps.bean.App
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.item_app.view.*

/**
 * @author cczhr
 * @description
 * @since 2021/6/17 14:22
 */
class ListAppsAdapter(var  list:ArrayList<App>,var context: Context):RecyclerView.Adapter<ListAppsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cb:CheckBox=itemView.cb
        var name:TextView=itemView.name
        var version:TextView=itemView.version

        fun bind(app: App) {
            name.text=app.name
            version.text=app.version
            cb.isChecked=app.isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bind(list[position])
        holder.itemView.setOnClickListener {
            list[position].isSelected=!list[position].isSelected
            notifyItemChanged(position)
        }

    }


    fun hasSelected():Boolean{
       for(item in list){
           if(item.isSelected){
               return true
           }
       }
        return false
    }
    override fun getItemCount(): Int {
       return list.size
    }
}