package com.example.animeapp.ui.animeSearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.animeapp.databinding.FragmentLimitAndPaginationBinding
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.utils.Limit
import com.example.animeapp.utils.Pagination
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LimitAndPaginationFragment : Fragment() {

    private var _binding: FragmentLimitAndPaginationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeSearchViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLimitAndPaginationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAnimeSearchResultsObservers()
        setupLimitSpinner()
        updatePagination(null)
    }

    private fun setupAnimeSearchResultsObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animeSearchResults.collectLatest { response ->
                    binding.apply {
                        when (response) {
                            is Resource.Success -> {
                                response.data?.data?.let { data ->
                                    if (data.isEmpty()) {
                                        limitSpinner.visibility = View.GONE
                                        updatePagination(null)
                                    } else {
                                        updatePagination(response.data.pagination)

                                        if (data.size <= 4) {
                                            limitSpinner.visibility = View.GONE
                                        } else {
                                            limitSpinner.visibility =
                                                View.VISIBLE
                                            limitSpinner.adapter
                                            val limitIndex =
                                                Limit.limitOptions.indexOf(viewModel.queryState.value.limit)
                                            limitSpinner.setSelection(if (limitIndex == -1) 0 else limitIndex)
                                        }
                                    }
                                }
                            }

                            is Resource.Error -> {
                                limitSpinner.visibility = View.GONE
                                updatePagination(null)
                            }

                            is Resource.Loading -> {
                                limitSpinner.visibility = View.GONE
                                updatePagination(null)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupLimitSpinner() {
        val limitSpinner: Spinner = binding.limitSpinner
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
                if (viewModel.queryState.value.limit != selectedLimit) {
                    val updatedQueryState = viewModel.queryState.value.copy(
                        limit = selectedLimit, page = 1
                    )
                    viewModel.applyFilters(updatedQueryState)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.applyFilters(
                    viewModel.queryState.value.copy(
                        limit = Limit.DEFAULT_LIMIT,
                        page = 1
                    )
                )
            }
        }
    }

    private fun updatePagination(pagination: CompletePagination?) {
        binding.apply {
            Pagination.setPaginationButtons(
                paginationButtonContainer,
                pagination
            ) { pageNumber ->
                viewModel.applyFilters(viewModel.queryState.value.copy(page = pageNumber))
            }
            paginationButtonContainer.visibility =
                if (pagination == null) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}