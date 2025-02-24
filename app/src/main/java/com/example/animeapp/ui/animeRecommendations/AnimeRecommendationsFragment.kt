package com.example.animeapp.ui.animeRecommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentRecommendationBinding
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnimeRecommendationsFragment : Fragment() {
    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    private lateinit var animeRecommendationsAdapter: AnimeRecommendationsAdapter

    private val viewModel: AnimeRecommendationsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupRefreshFloatingActionButton()
    }

    private fun setupRecyclerView() {
        animeRecommendationsAdapter = AnimeRecommendationsAdapter()
        binding.rvAnimeRecommendations.apply {
            adapter = animeRecommendationsAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.animeRecommendations.collectLatest { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let { animeResponse ->
                            animeRecommendationsAdapter.setLoading(false)
                            animeRecommendationsAdapter.differ.submitList(animeResponse.data)
                        }
                    }

                    is Resource.Error -> {
                        animeRecommendationsAdapter.setLoading(false)
                    }

                    is Resource.Loading -> {
                        animeRecommendationsAdapter.setLoading(true)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        animeRecommendationsAdapter.setOnItemClickListener { animeId ->
            Navigation.navigateToAnimeDetail(
                this,
                animeId,
                R.id.action_animeRecommendationsFragment_to_animeDetailFragment
            )
        }
    }

    private fun setupRefreshFloatingActionButton() {
        binding.fabRefresh.setOnClickListener {
            viewModel.refreshData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}