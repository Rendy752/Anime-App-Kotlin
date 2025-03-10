package com.example.animeapp.ui.animeWatch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentAnimeWatchHeaderBinding
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeWatch
import com.example.animeapp.models.Server
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnimeWatchHeaderFragment : Fragment() {

    private var _binding: FragmentAnimeWatchHeaderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeWatchViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeWatchHeaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.episodeWatch.collect { response ->
                when (response) {
                    is Resource.Success -> handleEpisodeWatchSuccess(response)
                    is Resource.Error -> handleEpisodeWatchError()
                    is Resource.Loading -> handleEpisodeWatchLoading()
                }
            }
        }
    }

    private fun setupServerRecyclerView(
        textView: View,
        recyclerView: RecyclerView,
        servers: List<Server>,
        category: String,
        episodeId: String
    ) {
        if (servers.isNotEmpty()) {
            textView.visibility = View.VISIBLE
            recyclerView.visibility = View.VISIBLE
            val serverQueries = servers.map { server ->
                EpisodeSourcesQuery(episodeId, server.serverName, category)
            }
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = viewModel.episodeSourcesQuery.value?.let {
                    ServerAdapter(serverQueries, it) { episodeSourcesQuery ->
                        viewModel.handleSelectedEpisodeServer(episodeId, episodeSourcesQuery)
                    }
                }
            }
        } else {
            recyclerView.visibility = View.GONE
            textView.visibility = View.GONE
        }
    }

    private fun handleEpisodeWatchSuccess(response: Resource.Success<EpisodeWatch>) {
        response.data?.let { episodeWatch ->
            binding.apply {
                episodeInfoProgressBar.visibility = View.GONE
                tvEpisodeTitle.visibility = View.VISIBLE
                tvCurrentEpisode.visibility = View.VISIBLE
                serverScrollView.visibility = View.VISIBLE

                episodeWatch.servers.let { servers ->
                    viewModel.episodes.value.let { episodes ->
                        val episodeName = episodes?.find { episode ->
                            episode.episodeId == servers.episodeId

                        }?.name
                        tvEpisodeTitle.text =
                            if (episodeName != "Full") episodeName else viewModel.animeDetail.value?.title
                                ?: ""
                        episodes?.find { it.episodeId == servers.episodeId }
                            ?.let { episode ->
                                val backgroundColor = if (episode.filler) {
                                    ContextCompat.getColor(requireContext(), R.color.filler_episode)
                                } else {
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.default_episode
                                    )
                                }
                                tvEpisodeTitle.setTextColor(backgroundColor)
                            }
                        "Eps. ${servers.episodeNo}".also { tvCurrentEpisode.text = it }
                    }
                    setupServerRecyclerView(
                        tvSub,
                        rvSubServer,
                        servers.sub,
                        "sub",
                        servers.episodeId
                    )
                    setupServerRecyclerView(
                        tvDub,
                        rvDubServer,
                        servers.dub,
                        "dub",
                        servers.episodeId
                    )
                    setupServerRecyclerView(
                        tvRaw,
                        rvRawServer,
                        servers.raw,
                        "raw",
                        servers.episodeId
                    )
                }
            }
        }
    }

    private fun handleEpisodeWatchError() {
        viewModel.episodes.value?.first()?.episodeId?.let { viewModel.handleSelectedEpisodeServer(it) }
    }

    private fun handleEpisodeWatchLoading() {
        binding.apply {
            episodeInfoProgressBar.visibility = View.VISIBLE
            tvEpisodeTitle.visibility = View.GONE
            tvCurrentEpisode.visibility = View.GONE
            serverScrollView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}