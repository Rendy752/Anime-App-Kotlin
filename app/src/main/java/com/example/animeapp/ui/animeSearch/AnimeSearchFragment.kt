package com.example.animeapp.ui.animeSearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animeapp.R
import com.example.animeapp.data.remote.api.RetrofitInstance
import com.example.animeapp.databinding.FragmentAnimeSearchBinding
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.ui.common.AnimeHeaderAdapter
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Limit
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.Pagination
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnimeSearchFragment : Fragment(), MenuProvider {

    private var _binding: FragmentAnimeSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var animeHeaderAdapter: AnimeHeaderAdapter

    private val viewModel: AnimeSearchViewModel by viewModels {
        val animeSearchRepository = AnimeSearchRepository(RetrofitInstance.api)
        AnimeSearchViewModelProviderFactory(animeSearchRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupRecyclerView()
        setupSearchView()
        setupLimitSpinner()
        setupObservers()
        setupClickListeners()
        setupRefreshFloatingActionButton()
        updatePagination(null)
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        animeHeaderAdapter = AnimeHeaderAdapter()
        binding.rvAnimeSearch.apply {
            adapter = animeHeaderAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun setupClickListeners() {
        animeHeaderAdapter.setOnItemClickListener { animeId ->
            Navigation.navigateToAnimeDetail(
                this,
                animeId,
                R.id.action_animeSearchFragment_to_animeDetailFragment
            )
        }
    }

    private fun setupSearchView() {
        val debounce = Debounce(
            lifecycleScope,
            1000L,
            { query -> viewModel.updateQuery(query) },
            viewModel
        )

        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    debounce.query(it)
                }
                return true
            }
        })
    }

    private fun setupLimitSpinner() {
        val limitSpinner: Spinner = binding.subMenuContainer.limitSpinner
        val limitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            Limit.limitOptions
        )
        limitSpinner.adapter = limitAdapter

        if (viewModel.queryState.value.limit == Limit.DEFAULT_LIMIT) {
            val defaultLimitIndex = Limit.limitOptions.indexOf(10)
            limitSpinner.setSelection(defaultLimitIndex)
        }

        limitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedLimit = Limit.getLimitValue(position)
                viewModel.updateLimit(selectedLimit)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                val defaultLimit = Limit.DEFAULT_LIMIT
                viewModel.updateLimit(defaultLimit)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.search_fragment_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_filter -> {
                Toast.makeText(requireContext(), "Filter clicked", Toast.LENGTH_SHORT).show()
                true
            }

            else -> false
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animeSearchResults.collectLatest { response ->
                    binding.apply {

                        when (response) {
                            is Resource.Success -> {
                                response.data?.let { searchResponse ->
                                    animeHeaderAdapter.setLoading(false)
                                    if (searchResponse.data.isEmpty()) {
                                        tvError.visibility = View.VISIBLE
                                        "No results found".also { tvError.text = it }
                                    } else {
                                        updatePagination(response.data.pagination)

                                        subMenuContainer.limitSpinner.adapter
                                        val limitIndex =
                                            Limit.limitOptions.indexOf(viewModel.queryState.value.limit)
                                        subMenuContainer.limitSpinner.setSelection(if (limitIndex == -1) 0 else limitIndex)

                                        animeHeaderAdapter.differ.submitList(searchResponse.data)
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
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupRefreshFloatingActionButton() {
        binding.fabRefresh.setOnClickListener {
            viewModel.searchAnime()
        }
    }

    private fun updatePagination(pagination: CompletePagination?) {
        binding.apply {
            Pagination.setPaginationButtons(
                subMenuContainer.paginationButtonContainer,
                pagination,
                onPaginationClick = { pageNumber ->
                    viewModel.updatePage(pageNumber)
                }
            )
            subMenuContainer.paginationButtonContainer.visibility =
                if (pagination == null) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}