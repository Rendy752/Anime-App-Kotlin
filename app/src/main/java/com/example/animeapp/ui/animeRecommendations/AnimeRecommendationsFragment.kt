package com.example.animeapp.ui.animeRecommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentRecommendationBinding
import com.example.animeapp.databinding.NetworkStatusLayoutBinding
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.NetworkStateMonitor
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnimeRecommendationsFragment : Fragment(), MenuProvider {
    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    private lateinit var animeRecommendationsAdapter: AnimeRecommendationsAdapter

    private val viewModel: AnimeRecommendationsViewModel by viewModels()

    private var menuInstance: Menu? = null
    private lateinit var networkStateMonitor: NetworkStateMonitor
    private var networkStatusBinding: NetworkStatusLayoutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onPrepareMenu(menu: Menu) {
        val networkStatusItem = menu.findItem(R.id.network_status_item)
        networkStatusBinding = networkStatusItem.actionView?.let {
            NetworkStatusLayoutBinding.bind(
                it
            )
        }

        networkStateMonitor.networkStatus.value?.let { status ->
            networkStatusBinding?.networkStatusIcon?.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    status.iconResId
                )
            )
            networkStatusBinding?.networkStatusText?.text = status.text
        }

    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.recommendation_fragment_menu, menu)
        menuInstance = menu
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.network_status_item -> {
                true
            }

            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        networkStateMonitor = NetworkStateMonitor(requireContext())
        networkStateMonitor.startMonitoring(requireContext())
        networkStateMonitor.networkStatus.observe(viewLifecycleOwner) {
            requireActivity().invalidateMenu()
        }
        networkStateMonitor.isConnected.observe(viewLifecycleOwner) { isConnected ->
            binding.apply {
                if (isConnected) {
                    tvError.visibility = View.GONE
                    viewModel.getAnimeRecommendations()
                    setupRecyclerView()
                    setupObservers()
                    setupClickListeners()
                    setupRefreshFloatingActionButton()
                } else {
                    tvError.visibility = View.VISIBLE
                    "No internet connection".also { tvError.text = it }
                    fabRefresh.visibility = View.GONE
                }
            }
        }
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
        binding.fabRefresh.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                viewModel.getAnimeRecommendations()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        networkStateMonitor.stopMonitoring()
        _binding = null
        networkStatusBinding = null
    }
}