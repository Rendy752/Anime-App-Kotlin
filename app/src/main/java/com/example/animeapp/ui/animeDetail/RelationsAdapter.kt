package com.example.animeapp.ui.animeDetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.databinding.RelationItemBinding
import com.example.animeapp.models.Relation

class RelationsAdapter(
    private val animeAPI: AnimeAPI,
    private val relations: List<Relation>?,
    private val onItemClickListener: (Int) -> Unit
) :
    RecyclerView.Adapter<RelationsAdapter.RelationViewHolder>() {

    class RelationViewHolder(val binding: RelationItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelationViewHolder {
        val binding =
            RelationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RelationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RelationViewHolder, position: Int) {
        val relation = relations?.getOrNull(position)
        holder.binding.apply {
            relation?.let {
                "${relation.entry.size} ${relation.relation}".also {
                    tvRelationName.text = it
                }
                val relationItemsAdapter = EntriesAdapter(animeAPI, relation, onItemClickListener)
                rvRelationItems.apply {
                    adapter = relationItemsAdapter
                    layoutManager = LinearLayoutManager(holder.itemView.context)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return relations?.size ?: 0
    }
}