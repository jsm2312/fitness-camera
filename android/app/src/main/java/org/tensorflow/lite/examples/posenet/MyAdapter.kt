package com.miguelrochefort.fitnesscamera

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.miguelrochefort.fitnesscamera.RepetitionCounter

class MyAdapter(counter: RepetitionCounter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(p0.context).inflate(R.layout.row, p0)
        return MyViewHolder(v!!)
    }

    override fun getItemCount(): Int {
        return 5
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {

    }
}

class MyViewHolder(v: View) : RecyclerView.ViewHolder(v) {

}
