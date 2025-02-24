package com.example.animeapp.ui.common

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.databinding.UnorderedListItemBinding
import com.example.animeapp.models.NameAndUrl

class NameAndUrlAdapter(private val item: List<NameAndUrl>) :
    RecyclerView.Adapter<NameAndUrlAdapter.UnorderedListViewHolder>() {

    class UnorderedListViewHolder(val binding: UnorderedListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnorderedListViewHolder {
        val binding =
            UnorderedListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UnorderedListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UnorderedListViewHolder, position: Int) {
        holder.binding.apply {
            tvListItem.text = item[position].name
            tvListItem.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_light))
            tvListItem.textSize = 16f
            tvListItem.setOnClickListener {
                item[position].url.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    holder.itemView.context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return item.size
    }
}