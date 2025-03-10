package com.example.animeapp.ui.animeDetail

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.databinding.EpisodeDetailItemBinding
import com.example.animeapp.models.Episode
import com.example.animeapp.utils.StreamingUtils

@SuppressLint("NotifyDataSetChanged")
class EpisodesDetailAdapter(
    private val context: Context,
    val episodes: MutableList<Episode>,
    private val onEpisodeClick: (String) -> Unit
) : RecyclerView.Adapter<EpisodesDetailAdapter.EpisodeViewHolder>() {

    class EpisodeViewHolder(val binding: EpisodeDetailItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding =
            EpisodeDetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]
        bindEpisodeData(holder.binding, episode)

        holder.itemView.setOnClickListener {
            onEpisodeClick(episode.episodeId)
        }
    }

    private fun bindEpisodeData(binding: EpisodeDetailItemBinding, episode: Episode) {
        binding.apply {
            tvEpisodeNumber.background = StreamingUtils.getEpisodeBackground(context, episode)
            "Ep. ${episode.episodeNo}".also { tvEpisodeNumber.text = it }
            tvEpisodeTitle.text = episode.name
        }
    }

    fun addData(newData: List<Episode>) {
        episodes.addAll(newData)
        notifyItemRangeInserted(episodes.size - newData.size, newData.size)
    }

    fun replaceData(newData: List<Episode>){
        episodes.clear()
        episodes.addAll(newData)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return episodes.size
    }
}