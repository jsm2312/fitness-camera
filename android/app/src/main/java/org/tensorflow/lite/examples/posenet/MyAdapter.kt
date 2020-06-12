package com.miguelrochefort.fitnesscamera

import android.content.ClipData
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.miguelrochefort.fitnesscamera.RepetitionCounter

class MyAdapter(counter: RepetitionCounter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val counter = counter

    init {
        // TODO: Listen for items change
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(p0.context).inflate(R.layout.row, p0, false)
        return ViewHolder(v!!)
    }

    override fun getItemCount(): Int {
        return counter.sets.size
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        val vh = p0 as ViewHolder
        if (vh != null) {
            vh.bind(counter.sets[p1])
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ExerciseSet) {//, listener: (ClipData.Item) -> Unit) = with(itemView) {
            var name = itemView.findViewById<TextView>(R.id.name)
            var reps = itemView.findViewById<TextView>(R.id.repetitions)
            name.text = item.exercise
            reps.text = item.repetitions.toString()

//            itemTitle.text = item.title
//            itemImage.loadUrl(item.url)
//            setOnClickListener { listener(item) }
        }
    }
}
