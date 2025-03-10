package com.example.animeapp.ui.animeSearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentResultsBinding
import com.example.animeapp.ui.common.AnimeHeaderAdapter
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    private lateinit var animeHeaderAdapter: AnimeHeaderAdapter

    private val viewModel: AnimeSearchViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAnimeHeaderRecyclerView()
        setupDetailNavigationListeners()
        setupAnimeSearchResultsObservers()
    }

    private fun setupAnimeHeaderRecyclerView() {
        animeHeaderAdapter = AnimeHeaderAdapter()
        binding.rvAnimeSearch.apply {
            adapter = animeHeaderAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun setupDetailNavigationListeners() {
        animeHeaderAdapter.setOnItemClickListener { animeId ->
            Navigation.navigateToAnimeDetail(
                this,
                animeId,
                R.id.action_animeSearchFragment_to_animeDetailFragment
            )
        }
    }

    private fun setupAnimeSearchResultsObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animeSearchResults.collectLatest { response ->
                    binding.apply {
                        when (response) {
                            is Resource.Success -> {
                                response.data?.data?.let { data ->
                                    animeHeaderAdapter.setLoading(false)
                                    if (data.isEmpty()) {
                                        tvError.visibility = View.VISIBLE
                                        "No results found".also { tvError.text = it }
                                    } else {
                                        animeHeaderAdapter.differ.submitList(data)
                                    }
                                }
                            }

                            is Resource.Error -> {
                                animeHeaderAdapter.setLoading(false)
                                tvError.visibility = View.VISIBLE
                                "An error occurred: ${response.message}".also {
                                    tvError.text = it
                                }
                                Toast.makeText(
                                    requireContext(),
                                    "An error occurred: ${response.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            is Resource.Loading -> {
                                animeHeaderAdapter.setLoading(true)
                                tvError.visibility = View.GONE
                            }
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