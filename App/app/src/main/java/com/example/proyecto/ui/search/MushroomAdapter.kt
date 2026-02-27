package com.example.proyecto.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.domain.MushroomItem

class MushroomAdapter : RecyclerView.Adapter<MushroomAdapter.VH>() {

    private val items = mutableListOf<MushroomItem>()

    fun submitList(newItems: List<MushroomItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mushroom, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvMeta.text = "Color: ${item.color} | Hábitat: ${item.habitat}"
    }
}
