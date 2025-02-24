package com.example.animeapp.ui.animeSearch

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.animeapp.R
import com.example.animeapp.databinding.GenresFlowLayoutBinding
import com.example.animeapp.databinding.ProducersFlowLayoutBinding
import com.example.animeapp.databinding.FragmentFilterBinding
import com.example.animeapp.models.Genres
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.ui.common.FlowLayout
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.Theme
import com.example.animeapp.utils.ViewUtils.toPx
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeSearchViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchView()
        setupGenresPopupWindow()
        setupProducersPopupWindow()
    }

    private fun setupSearchView() {
        val debounce = Debounce(
            lifecycleScope,
            1000L,
            { query ->
                viewModel.applyFilters(viewModel.queryState.value.copy(query = query))
            },
            viewModel
        )

        binding.apply {
            searchView.setOnQueryTextListener(object : OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { debounce.query(it) }
                    return true
                }
            })
        }
    }

    private fun setupGenresPopupWindow() {
        val genresPopupWindow = createPopupWindow()

        binding.apply {
            val genresFlowLayoutBinding =
                GenresFlowLayoutBinding.inflate(layoutInflater, root, false)
            genresFlowLayoutBinding.apply {
                genresPopupWindow.contentView = genresFlowLayoutBinding.root
                val genreFlowLayout = genresFlowLayoutBinding.genreFlowLayout
                genresFlowLayoutBinding.retryButton.setOnClickListener { viewModel.fetchGenres() }

                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.genres.collect { response ->
                            handleGenreResponse(response, genresFlowLayoutBinding, genreFlowLayout)

                        }
                    }
                }
            }

            genresPopupWindow.setOnDismissListener {}
            genresField.setOnClickListener {
                genresPopupWindow.showAsDropDown(it, -it.width, 1.toPx())
            }
        }
    }

    private fun setupProducersPopupWindow() {
        val producersPopupWindow = createPopupWindow()

        binding.apply {
            val producersFlowLayoutBinding =
                ProducersFlowLayoutBinding.inflate(layoutInflater, root, false)
            producersPopupWindow.contentView = producersFlowLayoutBinding.root
            val producerFlowLayout = producersFlowLayoutBinding.producerFlowLayout

            producersPopupWindow.setOnDismissListener {}
            producersField.setOnClickListener {
                producersPopupWindow.showAsDropDown(it, it.width, 1.toPx())
            }
        }
    }

    private fun handleGenreResponse(
        response: Resource<GenresResponse>,
        binding: GenresFlowLayoutBinding,
        genreFlowLayout: FlowLayout
    ) {
        binding.apply {
            when (response) {
                is Resource.Success -> {
                    genreFlowLayout.setLoading(false)
                    val genres = response.data?.data ?: emptyList()
                    if (genres.isEmpty()) {
                        emptyTextView.visibility = View.VISIBLE
                        retryButton.visibility = View.VISIBLE
                    } else {
                        emptyTextView.visibility = View.GONE
                        retryButton.visibility = View.GONE
                        populateGenreChipGroup(genreFlowLayout, genres)
                    }
                }

                is Resource.Loading -> {
                    emptyTextView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    genreFlowLayout.setLoading(true)
                }

                is Resource.Error -> {
                    genreFlowLayout.setLoading(false)
                    emptyTextView.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                    Toast.makeText(
                        requireContext(),
                        "An error occurred",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun createPopupWindow(): PopupWindow {
        return PopupWindow(requireContext()).apply {
            isOutsideTouchable = true
            isFocusable = true
            elevation = 10f
            width = ViewGroup.LayoutParams.MATCH_PARENT

            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(if (Theme.isDarkMode()) Color.WHITE else Color.BLACK)
                cornerRadius = 20f
                alpha = (255 * 0.7f).toInt()
            }

            setBackgroundDrawable(backgroundDrawable)
        }
    }

    private fun populateGenreChipGroup(flowLayout: FlowLayout, genres: List<Genres>) {
        flowLayout.removeAllViews()
        for (genre in genres) {
            val chip = layoutInflater.inflate(R.layout.chip_layout, flowLayout, false) as Chip
            chip.text = genre.name
            chip.id = genre.mal_id

            //... (Set other chip properties like onClickListener, etc.)...
            flowLayout.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}