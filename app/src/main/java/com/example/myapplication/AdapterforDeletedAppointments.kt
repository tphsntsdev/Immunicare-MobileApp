package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterforDeletedAppointments(private val context: Context, private val userList : ArrayList<fetchdeletedappointments>) : RecyclerView.Adapter<AdapterforDeletedAppointments.MyViewHolder>() {
    private val prefkey = context.getString(R.string.prefKey)
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(prefkey, Context.MODE_PRIVATE)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterforDeletedAppointments.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.historydata,parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AdapterforDeletedAppointments.MyViewHolder, position: Int) {
        val child_state = sharedPreferences.getString("child_state", null)!!
        val user : fetchdeletedappointments = userList[position]
        if (child_state == "TRUE") {
            val isChildVaccineEmpty = user.childvaccine.isNullOrBlank()
            val isAdultVaccineEmpty = user.adultvaccine.isNullOrBlank()
            when {
                isChildVaccineEmpty -> holder.vaccineText.text = user.adultvaccine
                isAdultVaccineEmpty -> holder.vaccineText.text = user.childvaccine
                else -> holder.vaccineText.text = user.childvaccine + " " + user.adultvaccine
            }
        }
        if (child_state == "FALSE"){
            holder.vaccineText.text = user.adultvaccine
        }

        holder.locationText.text = user.location
        holder.datetimeText.text = user.date + " " + user.time
        holder.statusText.text = user.status

    }
    override fun getItemCount(): Int {
        return userList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vaccineText: TextView = itemView.findViewById(R.id.vaccineText)
        val locationText: TextView = itemView.findViewById(R.id.locationText)
        val datetimeText: TextView = itemView.findViewById(R.id.datetimeText)
        val statusText : TextView = itemView.findViewById(R.id.statustext)




    }
}