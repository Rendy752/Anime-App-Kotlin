package com.example.animeapp.ui.animeWatch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.databinding.FragmentAnimeWatchEpisodeBinding
import com.example.animeapp.models.Episode
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnimeWatchEpisodeFragment : Fragment() {

    private var _binding: FragmentAnimeWatchEpisodeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeWatchViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeWatchEpisodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEpisodeNavigationButtons()
        handleStaticAnimeDetailData()
    }

    private fun setupEpisodeNavigationButtons() {
        binding.apply {
            previousEpisodeButton.setOnClickListener {
                handleEpisodeNavigation(-1, binding.previousEpisodeButton)
            }
            nextEpisodeButton.setOnClickListener {
                handleEpisodeNavigation(1, binding.nextEpisodeButton)
            }
        }
    }

    private fun handleEpisodeNavigation(direction: Int, button: android.widget.Button) {
        viewModel.episodeWatch.value.data?.servers?.let { currentServer ->
            viewModel.episodes.value?.let { episodes ->
                if (episodes.isNotEmpty()) {
                    button.isEnabled = true
                    val targetEpisodeNo = currentServer.episodeNo + direction
                    episodes.find { it.episodeNo == targetEpisodeNo }?.let { targetEpisode ->
                        viewModel.handleSelectedEpisodeServer(targetEpisode.episodeId)
                    }
                } else {
                    button.isEnabled = false
                }
            }
        }
    }

    private fun handleStaticAnimeDetailData() {
        binding.apply {
            viewModel.episodes.value.let { episodes ->
                "Total Episode: ${viewModel.animeDetail.value?.episodes}".also {
                    tvTotalEpisodes.text = it
                }

                val debounce = Debounce(
                    lifecycleScope,
                    1000L,
                    { query ->
                        if (query.isNotEmpty()) {
                            handleJumpToEpisode(
                                query.toInt(),
                                episodes ?: emptyList()
                            )
                        }
                    }
                )

                svEpisodeSearch.setOnQueryTextListener(object : OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        newText?.let { debounce.query(it) }
                        return true
                    }
                })

                setupEpisodeRecyclerView(episodes ?: emptyList())
            }
        }
    }

    private fun setupEpisodeRecyclerView(episodes: List<Episode>) {
        binding.apply {
            episodeSearchContainer.visibility = if (episodes.size > 1) View.VISIBLE else View.GONE
            rvEpisodes.apply {
                if (episodes.isNotEmpty()) {
                    layoutManager = GridLayoutManager(context, 4)
                    adapter = EpisodesWatchAdapter(requireContext(), episodes) {
                        viewModel.handleSelectedEpisodeServer(it)
                    }
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.episodeWatch.collect { response ->
                            if (response is Resource.Success) {
                                response.data?.servers?.episodeNo?.let {
                                    (adapter as EpisodesWatchAdapter).updateSelectedEpisode(it)
                                    handleJumpToEpisode(it, episodes)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleJumpToEpisode(episodeNumber: Int, episodes: List<Episode>) {
        val foundEpisodeIndex = episodes.indexOfFirst { it.episodeNo == episodeNumber }

        if (foundEpisodeIndex != -1) {
            binding.rvEpisodes.apply {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    foundEpisodeIndex,
                    0
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}