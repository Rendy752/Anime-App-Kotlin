package com.example.animeapp.ui.animeWatch

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.animeapp.databinding.FragmentAnimeWatchInfoBinding
import com.example.animeapp.utils.BindAnimeUtils
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri

@AndroidEntryPoint
class AnimeWatchInfoFragment : Fragment() {

    private var _binding: FragmentAnimeWatchInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeWatchViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeWatchInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleStaticAnimeDetailData()
    }

    private fun handleStaticAnimeDetailData() {
        binding.apply {
            viewModel.animeDetail.value?.let { animeDetail ->
                BindAnimeUtils.bindAnimeHeader(
                    requireContext(),
                    animeHeader,
                    { url ->
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        startActivity(intent)
                    },
                    animeDetail
                )

                val embedUrl = animeDetail.trailer.embed_url ?: ""
                if (embedUrl.isNotEmpty()) {
                    llYoutubePreview.visibility = View.VISIBLE
                    youtubePlayerView.playVideo(embedUrl)
                }

                with(animeSynopsis) {
                    animeDetail.synopsis?.let { synopsis ->
                        if (synopsis.isNotBlank()) {
                            llBackground.visibility = View.VISIBLE
                            tvSynopsis.text = synopsis
                        } else {
                            llBackground.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}