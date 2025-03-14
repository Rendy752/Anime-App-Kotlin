package com.example.animeapp.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.animeapp.R
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement

object Navigation {
    fun navigateToAnimeDetail(fragment: Fragment, animeId: Int, actionId: Int) {
        val bundle = Bundle().apply {
            putInt("id", animeId)
        }
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .setPopUpTo(R.id.animeDetailFragment, true)
            .build()

        fragment.findNavController().navigate(
            actionId,
            bundle,
            navOptions
        )
    }

    fun navigateToAnimeWatch(
        fragment: Fragment,
        actionId: Int,
        animeDetail: AnimeDetail,
        episodeId: String,
        episodes: List<Episode>,
        defaultEpisode: EpisodeDetailComplement
    ) {
        val bundle = Bundle().apply {
            putParcelable("animeDetail", animeDetail)
            putString("episodeId", episodeId)
            putParcelableArrayList("episodes", ArrayList(episodes))
            putParcelable("defaultEpisode", defaultEpisode)
        }

        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_left)
            .setExitAnim(R.anim.slide_out_right)
            .setPopEnterAnim(R.anim.slide_in_right)
            .setPopExitAnim(R.anim.slide_out_left)
            .setPopUpTo(R.id.animeDetailFragment, false)
            .build()

        fragment.findNavController().navigate(
            actionId,
            bundle,
            navOptions
        )
    }
}