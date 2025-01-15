package com.example.animeappkotlin.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeappkotlin.databinding.UnorderedListItemBinding

class UnorderedListAdapter(private val item: List<String>) :
    RecyclerView.Adapter<UnorderedListAdapter.UnorderedListViewHolder>() {

    class UnorderedListViewHolder(val binding: UnorderedListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnorderedListViewHolder {
        val binding =
            UnorderedListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UnorderedListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UnorderedListViewHolder, position: Int) {
        holder.binding.tvListItem.text = item[position]
    }

    override fun getItemCount(): Int {
        return item.size
    }
}