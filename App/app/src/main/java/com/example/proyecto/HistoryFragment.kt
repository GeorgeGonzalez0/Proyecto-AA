package com.example.proyecto

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.domain.HistoryStore
import com.example.proyecto.ui.history.HistoryAdapter

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val adapter = HistoryAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyHistory)
        val btnClear = view.findViewById<Button>(R.id.btnClearHistory)

        btnClear.setOnClickListener {
            HistoryStore.items.clear()
            adapter.submitList(emptyList())
            tvEmpty.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        val tvEmpty = requireView().findViewById<TextView>(R.id.tvEmptyHistory)

        val items = HistoryStore.items.toList().reversed() // últimos primero
        adapter.submitList(items)

        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
