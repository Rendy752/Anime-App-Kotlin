package com.example.animeapp.ui.animeWatch

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.databinding.TooltipLayoutBinding
import com.example.animeapp.databinding.EpisodeWatchItemBinding
import com.example.animeapp.models.Episode
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.StreamingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EpisodesWatchAdapter(
    private val context: Context,
    private val episodes: List<Episode>,
    private val onEpisodeClick: (String) -> Unit
) : RecyclerView.Adapter<EpisodesWatchAdapter.EpisodeViewHolder>() {

    private var selectedEpisodeNo: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private var tooltipPopupWindow: PopupWindow? = null

    class EpisodeViewHolder(val binding: EpisodeWatchItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding =
            EpisodeWatchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    fun updateSelectedEpisode(selectedEpisodeNo: Int) {
        val oldSelectedPosition =
            this.selectedEpisodeNo?.let { episodes.indexOfFirst { episode -> episode.episodeNo == it } }
        val newSelectedPosition = episodes.indexOfFirst { it.episodeNo == selectedEpisodeNo }

        this.selectedEpisodeNo = selectedEpisodeNo

        oldSelectedPosition?.let { notifyItemChanged(it) }
        notifyItemChanged(newSelectedPosition)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]
        bindEpisodeData(holder, episode)
    }

    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val episodeDebounce = Debounce(adapterScope, onDebounced = { episodeId ->
        onEpisodeClick(episodeId)
    })

    private fun bindEpisodeData(
        holder: EpisodeViewHolder,
        episode: Episode
    ) {
        holder.binding.apply {
            episode.episodeNo.toString().also { tvEpisodeNumber.text = it }

            val isSelected = selectedEpisodeNo == episode.episodeNo

            StreamingUtils.getEpisodeBackground(
                context, episode, if (isSelected) {
                    selectedEpisodeNo
                } else {
                    -1
                }
            ).let { tvEpisodeNumber.background = it }

            holder.itemView.setOnClickListener {
                if (episode.episodeNo != selectedEpisodeNo) {
                    updateSelectedEpisode(episode.episodeNo)
                    episodeDebounce.query(episode.episodeId)
                }
            }

            holder.itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        handler.postDelayed({
                            showTooltip(v, episode.name)
                        }, 500)
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacksAndMessages(null)
                        hideTooltip()
                        if (event.action == MotionEvent.ACTION_UP) {
                            v.performClick()
                        }
                    }
                }
                false
            }
        }
    }

    override fun getItemCount(): Int = episodes.size

    private fun showTooltip(anchorView: View, episodeName: String) {
        val tooltipView = TooltipLayoutBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply {
            tooltipText.text = episodeName
        }.root

        tooltipPopupWindow = PopupWindow(
            tooltipView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )

        tooltipPopupWindow?.showAsDropDown(
            anchorView,
            0,
            -anchorView.height * 2 + 10,
            Gravity.CENTER
        )
    }

    private fun hideTooltip() {
        tooltipPopupWindow?.dismiss()
        tooltipPopupWindow = null
    }
}