package com.example.animeapp.ui.animeSearch

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
import com.bumptech.glide.Glide
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentFilterBinding
import com.example.animeapp.databinding.GenresFlowLayoutBinding
import com.example.animeapp.databinding.ProducersFlowLayoutBinding
import com.example.animeapp.models.Genre
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.Producer
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.ui.common.FlowLayout
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.Theme
import com.example.animeapp.utils.ViewUtils.toPx
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                genresPopupWindow.contentView = root
                val genreFlowLayout = genreFlowLayout
                retryButton.setOnClickListener { viewModel.fetchGenres() }

                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.genres.collect { response ->
                            handleGenreResponse(response, genresFlowLayoutBinding, genreFlowLayout)
                        }
                    }
                }

                resetButton.setOnClickListener {
                    if (viewModel.queryState.value.isGenresDefault()) {
                        Toast.makeText(
                            requireContext(),
                            "No genres filter applied yet",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        viewModel.resetGenreSelection()
                        genresPopupWindow.dismiss()
                    }
                }
                applyButton.setOnClickListener {
                    if (viewModel.selectedGenreId.value.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "No genres filter applied",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.applyGenreFilters()
                        genresPopupWindow.dismiss()
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

            producersFlowLayoutBinding.apply {
                producersPopupWindow.contentView = root
                val producerFlowLayout = producerFlowLayout
                retryButton.setOnClickListener { viewModel.fetchProducers() }
                viewLifecycleOwner.lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.producers.collect { response ->
                            handleProducersResponse(
                                response,
                                producersFlowLayoutBinding,
                                producerFlowLayout
                            )
                        }
                    }
                }

                resetButton.setOnClickListener {
                    if (viewModel.queryState.value.isProducersDefault()) {
                        Toast.makeText(
                            requireContext(),
                            "No producers filter applied yet",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.resetProducerSelection()
                        producersPopupWindow.dismiss()
                    }
                }

                applyButton.setOnClickListener {
                    if (viewModel.selectedProducerId.value.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "No producers filter applied",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewModel.applyProducerFilters()
                        producersPopupWindow.dismiss()
                    }
                }
            }

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

    private fun handleProducersResponse(
        response: Resource<ProducersResponse>,
        binding: ProducersFlowLayoutBinding,
        producerFlowLayout: FlowLayout
    ) {
        binding.apply {
            when (response) {
                is Resource.Success -> {
                    producerFlowLayout.setLoading(false)
                    val producers = response.data?.data ?: emptyList()
                    if (producers.isEmpty()) {
                        emptyTextView.visibility = View.VISIBLE
                        retryButton.visibility = View.VISIBLE
                    } else {
                        emptyTextView.visibility = View.GONE
                        retryButton.visibility = View.GONE
                        populateProducerChipGroup(producerFlowLayout, producers)
                    }
                }

                is Resource.Loading -> {
                    emptyTextView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    producerFlowLayout.setLoading(true)
                }

                is Resource.Error -> {
                    producerFlowLayout.setLoading(false)
                    emptyTextView.visibility = View.VISIBLE
                    retryButton.visibility
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

    private fun populateGenreChipGroup(flowLayout: FlowLayout, genres: List<Genre>) {
        flowLayout.removeAllViews()
        for (genre in genres) {
            val chip = layoutInflater.inflate(R.layout.chip_layout, flowLayout, false) as Chip
            chip.apply {
                "${genre.name} (${genre.count})".also { text = it }
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.selectedGenreId.collectLatest { selectedGenreIds ->
                            isChecked = selectedGenreIds.contains(genre.mal_id)
                        }
                    }
                }
                setOnClickListener {
                    viewModel.setSelectedGenreId(genre.mal_id)
                }
                setOnLongClickListener {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(genre.url))
                    )
                    true
                }

                flowLayout.addView(this)
            }
        }
    }

    private fun populateProducerChipGroup(flowLayout: FlowLayout, producers: List<Producer>) {
        flowLayout.removeAllViews()
        for (producer in producers) {
            val chip = layoutInflater.inflate(R.layout.chip_layout, flowLayout, false) as Chip
            chip.apply {
                "${producer.titles?.get(0)?.title ?: "Unknown"} (${producer.count})".also {
                    text = it
                }

                val iconUrl = producer.images?.jpg?.image_url
                if (!iconUrl.isNullOrEmpty()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        val drawable: Drawable? = try {
                            withContext(Dispatchers.IO) {
                                Glide.with(requireContext())
                                    .load(iconUrl)
                                    .circleCrop()
                                    .submit()
                                    .get()
                            }
                        } catch (e: Exception) {
                            null
                        }
                        if (drawable != null) {
                            chipIcon = drawable
                        } else {
                            setChipIconResource(R.drawable.ic_error_yellow_24dp)
                        }
                    }
                } else {
                    setChipIconResource(R.drawable.ic_error_yellow_24dp)
                }

                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.selectedProducerId.collectLatest { selectedProducerIds ->
                            isChecked = selectedProducerIds.contains(producer.mal_id)
                        }
                    }
                }
                setOnClickListener {
                    viewModel.setSelectedProducerId(producer.mal_id)
                }
                setOnLongClickListener {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(producer.url))
                    )
                    true
                }
                flowLayout.addView(this)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}