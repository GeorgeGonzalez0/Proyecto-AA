package com.example.proyecto.ui.history

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.domain.HistoryItem

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val items = mutableListOf<HistoryItem>()

    fun submitList(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        val tvConfidence: TextView = itemView.findViewById(R.id.tvConfidence)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvLabel.text = item.label
        holder.tvConfidence.text = "Confianza: ${(item.confidence * 100).toInt()}%"
        holder.tvDate.text = DateFormat.format("yyyy-MM-dd HH:mm", item.timestamp).toString()
    }
}
