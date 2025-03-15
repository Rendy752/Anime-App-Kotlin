package com.example.animeapp.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.animeapp.R
import com.example.animeapp.databinding.AnimeHeaderBinding
import com.example.animeapp.databinding.AnimeSearchItemBinding
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.ui.common.TitleSynonymsAdapter

object BindAnimeUtils {
    fun bindAnimeData(binding: AnimeSearchItemBinding, data: AnimeDetail) {
        binding.apply {
            Glide.with(root.context)
                .load(data.images.jpg.image_url)
                .into(ivAnimeImage)

            when (data.approved) {
                true -> ivApproved.visibility = View.VISIBLE
                false -> ivApproved.visibility = View.GONE
            }

            when (data.airing) {
                true -> {
                    tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_notifications_active_24dp,
                        0
                    )
                }

                false -> {
                    tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_done_24dp,
                        0
                    )
                }
            }

            tvAnimeTitle.text = data.title
            rvTitleSynonyms.apply {
                adapter = data.title_synonyms?.let { TitleSynonymsAdapter(it.toList()) }
                layoutManager =
                    LinearLayoutManager(root.context, LinearLayoutManager.HORIZONTAL, false)
            }
            "${data.type ?: "Unknown"} (${data.episodes} eps)".also { tvAnimeType.text = it }
            "Ranked #${data.rank ?: 0}".also { tvAnimeRanked.text = it }
            "Popularity #${data.popularity}".also { tvAnimePopularity.text = it }
            "Scored ${data.score ?: 0} by ${data.scored_by ?: 0} users".also {
                tvAnimeScore.text = it
            }
            "${data.members} members".also { tvAnimeMembers.text = it }
        }

        resetBackground(binding)
    }


    fun handleNullData(binding: AnimeSearchItemBinding, title: String? = null) {
        binding.apply {
            Glide.with(root.context)
                .load(R.drawable.ic_error_yellow_24dp)
                .into(ivAnimeImage)

            tvAnimeTitle.text = title ?: "Unknown Title"
            rvTitleSynonyms.apply {
                adapter = TitleSynonymsAdapter(emptyList())
                layoutManager =
                    LinearLayoutManager(root.context, LinearLayoutManager.HORIZONTAL, false)
            }
            "Unknown Type (Unknown Episodes)".also { tvAnimeType.text = it }
            "Ranked #Unknown".also { tvAnimeRanked.text = it }
            "Popularity #Unknown".also { tvAnimePopularity.text = it }
            "Scored Unknown by Unknown users".also { tvAnimeScore.text = it }
            "Unknown members".also { tvAnimeMembers.text = it }
        }

        resetBackground(binding)
    }

    private fun resetBackground(binding: AnimeSearchItemBinding) {
        binding.apply {
            ivAnimeImage.background = null
            contentLayout.background = null
            rvTitleSynonyms.background = null
            tvAnimeType.background = null
            tvAnimeRanked.background = null
            tvAnimePopularity.background = null
            tvAnimeScore.background = null
            tvAnimeMembers.background = null
        }
    }

    fun bindAnimeHeader(
        context: Context,
        binding: AnimeHeaderBinding,
        redirectToUrl: (String) -> Unit,
        detail: AnimeDetail
    ) {
        with(binding) {
            Glide.with(context).load(detail.images.jpg.large_image_url)
                .into(ivAnimeImage)
            tvTitle.text = detail.title
            tvTitle.setOnClickListener {
                redirectToUrl(detail.url)
            }
            tvTitle.setOnLongClickListener {
                val clipboard =
                    ContextCompat.getSystemService(
                        context,
                        ClipboardManager::class.java
                    )
                val clip =
                    ClipData.newPlainText("Anime Title", tvTitle.text.toString())
                clipboard?.setPrimaryClip(clip)
                true
            }
            tvEnglishTitle.text = detail.title_english
            tvJapaneseTitle.text = detail.title_japanese
            rvTitleSynonyms.apply {
                adapter = detail.title_synonyms?.let { TitleSynonymsAdapter(it.toList()) }
                layoutManager = LinearLayoutManager(
                    context, LinearLayoutManager.HORIZONTAL, false
                )
            }

            when (detail.approved) {
                true -> {
                    ivApproved.visibility = View.VISIBLE
                }

                false -> {
                    ivApproved.visibility = View.GONE
                }
            }

            when (detail.airing) {
                true -> {
                    tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_notifications_active_24dp,
                        0
                    )
                }

                false -> {
                    tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_done_24dp,
                        0
                    )
                }
            }
        }
    }
}