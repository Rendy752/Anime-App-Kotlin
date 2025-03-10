package com.example.animeapp.ui.animeSearch

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentFilterBinding
import com.example.animeapp.databinding.GenresFilterLayoutBinding
import com.example.animeapp.databinding.ProducersFilterLayoutBinding
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.Genre
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.Producer
import com.example.animeapp.models.ProducersResponse
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.Limit
import com.example.animeapp.utils.Pagination
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.Theme
import com.example.animeapp.utils.ViewUtils.toDp
import com.example.animeapp.utils.ViewUtils.toPx
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var genresPopupWindow: PopupWindow
    private lateinit var genresFilterLayoutBinding: GenresFilterLayoutBinding

    private lateinit var producersPopupWindow: PopupWindow
    private lateinit var producersFilterLayoutBinding: ProducersFilterLayoutBinding

    private val viewModel: AnimeSearchViewModel by viewModels({ requireParentFragment() })

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
        setupRecyclerView(
            genresFilterLayoutBinding.genreRecyclerView,
            GenreChipAdapter { genre -> viewModel.setSelectedGenreId(genre.mal_id) }
        )
        setupRecyclerView(
            producersFilterLayoutBinding.producerRecyclerView,
            ProducerChipAdapter { producer -> viewModel.setSelectedProducerId(producer.mal_id) }
        )
        setupFiltersObservers()
        setupLimitSpinner(25)
        updatePagination(null)
    }

    private fun setupSearchView() {
        val debounce = Debounce(
            lifecycleScope,
            1000L,
            { query ->
                viewModel.applyFilters(viewModel.queryState.value.copy(query = query, page = 1))
            },
            viewModel,
            Debounce.StateType.ANIME_SEARCH
        )


        binding.apply {
            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.queryState.collectLatest { response ->
                            searchView.setQuery(response.query, false)
                        }
                    }
                }
            }

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

    private fun setupFilterPopupWindow(
        popupWindow: PopupWindow,
        filterType: FilterType,
        field: View,
        fieldIcon: ImageView,
        getFilterLayout: () -> View,
        resetAction: () -> Unit,
        applyAction: () -> Unit,
        isEmptySelection: () -> Boolean,
        isDefault: () -> Boolean,
        fetchData: () -> Unit
    ) {
        val filterLayout = getFilterLayout()

        popupWindow.contentView = filterLayout
        filterLayout.findViewById<Button>(R.id.retryButton).setOnClickListener { fetchData() }

        filterLayout.findViewById<Button>(R.id.resetButton).setOnClickListener {
            if (isDefault()) {
                Toast.makeText(
                    requireContext(),
                    "No ${filterType.displayName} filter applied yet",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                resetAction()
                popupWindow.dismiss()
            }
        }

        filterLayout.findViewById<Button>(R.id.applyButton).setOnClickListener {
            if (isEmptySelection()) {
                Toast.makeText(
                    requireContext(),
                    "No ${filterType.displayName} filter applied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                applyAction()
                popupWindow.dismiss()
            }
        }

        field.setOnClickListener {
            fieldIcon.setImageResource(R.drawable.ic_close_red_24dp)
            popupWindow.showAsDropDown(it, -it.width, 1.toPx())
        }

        popupWindow.setOnDismissListener {
            fieldIcon.setImageResource(R.drawable.ic_chevron_down_blue_24dp)
        }
    }

    private fun setupGenresPopupWindow() {
        genresFilterLayoutBinding =
            GenresFilterLayoutBinding.inflate(layoutInflater, binding.root, false)
        genresPopupWindow = createPopupWindow()
        setupFilterPopupWindow(
            popupWindow = genresPopupWindow,
            filterType = FilterType.GENRES,
            field = binding.genresField,
            fieldIcon = binding.genresFieldIcon,
            getFilterLayout = { genresFilterLayoutBinding.root },
            resetAction = { viewModel.resetGenreSelection() },
            applyAction = { viewModel.applyGenreFilters() },
            isEmptySelection = { viewModel.selectedGenreId.value.isEmpty() },
            isDefault = { viewModel.queryState.value.isGenresDefault() },
            fetchData = { viewModel.fetchGenres() }
        )
    }

    private fun setupProducersPopupWindow() {
        producersFilterLayoutBinding =
            ProducersFilterLayoutBinding.inflate(layoutInflater, binding.root, false)
        producersPopupWindow = createPopupWindow()

        val debounce = Debounce(
            lifecycleScope,
            1000L,
            { query ->
                viewModel.applyProducerQueryStateFilters(
                    viewModel.producersQueryState.value.copy(
                        query = query, page = 1
                    )
                )
            },
            viewModel,
            Debounce.StateType.PRODUCER_SEARCH
        )

        producersFilterLayoutBinding.searchView.setOnQueryTextListener(object :
            OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { debounce.query(it) }
                return true
            }
        })

        setupFilterPopupWindow(
            popupWindow = producersPopupWindow,
            filterType = FilterType.PRODUCERS,
            field = binding.producersField,
            fieldIcon = binding.producersFieldIcon,
            getFilterLayout = { producersFilterLayoutBinding.root },
            resetAction = { viewModel.resetProducerSelection() },
            applyAction = { viewModel.applyProducerFilters() },
            isEmptySelection = { viewModel.selectedProducerId.value.isEmpty() },
            isDefault = { viewModel.queryState.value.isProducersDefault() },
            fetchData = { viewModel.fetchProducers() }
        )
    }

    private enum class FilterType(val displayName: String) {
        GENRES("genres"),
        PRODUCERS("producers")
    }

    private fun adjustPopupWindowWidth(popupWindow: PopupWindow) {
        val newWidth =
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                (resources.displayMetrics.widthPixels * 0.92).toInt()
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
        popupWindow.apply {
            dismiss()
            width = newWidth
        }
    }

    private fun adjustProducerFilterLayout() {
        producersFilterLayoutBinding.apply {
            producerRecyclerView.setPadding(
                8.toDp(),
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 24.toDp() else 0,
                8.toDp(),
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 24.toDp() else 0
            )
            searchView.visibility =
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) View.GONE else View.VISIBLE
            limitAndPaginationFragment.root.visibility =
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) View.GONE else View.VISIBLE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustPopupWindowWidth(genresPopupWindow)
        adjustPopupWindowWidth(producersPopupWindow)
        adjustProducerFilterLayout()
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        adapter: RecyclerView.Adapter<*>
    ) {
        recyclerView.apply {
            this.adapter = adapter
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexWrap = FlexWrap.WRAP
                flexDirection = FlexDirection.ROW
                alignItems = AlignItems.STRETCH
                justifyContent = JustifyContent.SPACE_AROUND
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

    @SuppressLint("NotifyDataSetChanged")
    private fun setupFiltersObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.producers.collectLatest { response ->
                        handleProducersResponse(response)
                    }
                }
                launch {
                    viewModel.genres.collectLatest { response ->
                        handleGenreResponse(response)
                    }
                }
                genresFilterLayoutBinding.genreRecyclerView.apply {
                    launch {
                        viewModel.selectedGenreId.collectLatest { selectedIds ->
                            if (adapter != null) {
                                (adapter as GenreChipAdapter).selectedIds = selectedIds
                                adapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
                producersFilterLayoutBinding.producerRecyclerView.apply {
                    launch {
                        viewModel.selectedProducerId.collectLatest { selectedIds ->
                            if (adapter != null) {
                                (adapter as ProducerChipAdapter).selectedIds = selectedIds
                                adapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <T> handleFilterResponse(
        response: Resource<T>,
        recyclerView: RecyclerView,
        progressBar: ProgressBar,
        emptyTextView: TextView,
        retryButton: Button,
        updateAdapter: (List<Any>, List<Int>) -> Unit,
        additionalSuccessHandling: (T?) -> Unit = {}
    ) {
        when (response) {
            is Resource.Success -> {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                retryButton.visibility = View.GONE

                val dataList = when (response.data) {
                    is GenresResponse -> (response.data as GenresResponse).data
                    is ProducersResponse -> (response.data as ProducersResponse).data
                    else -> emptyList()
                }

                if (dataList.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyTextView.visibility = View.VISIBLE
                    additionalSuccessHandling(null)
                } else {
                    emptyTextView.visibility = View.GONE
                    updateAdapter(
                        dataList, when (response.data) {
                            is GenresResponse -> viewModel.selectedGenreId.value
                            is ProducersResponse -> viewModel.selectedProducerId.value
                            else -> emptyList()
                        }
                    )
                    additionalSuccessHandling(response.data)
                }
            }

            is Resource.Error -> {
                recyclerView.visibility = View.GONE
                progressBar.visibility = View.GONE
                emptyTextView.text = response.message
                emptyTextView.visibility = View.VISIBLE
                retryButton.visibility = View.VISIBLE
                additionalSuccessHandling(null)
            }

            is Resource.Loading -> {
                recyclerView.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE
                retryButton.visibility = View.GONE
                additionalSuccessHandling(null)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleGenreResponse(response: Resource<GenresResponse>) {
        with(genresFilterLayoutBinding) {
            handleFilterResponse(
                response = response,
                recyclerView = genreRecyclerView,
                progressBar = progressBar,
                emptyTextView = emptyTextView,
                retryButton = retryButton,
                updateAdapter = { dataList, selectedIds ->
                    val adapter =
                        genreRecyclerView.adapter as GenreChipAdapter
                    adapter.items = dataList.filterIsInstance<Genre>()
                    adapter.selectedIds = selectedIds
                    adapter.notifyDataSetChanged()
                }
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleProducersResponse(response: Resource<ProducersResponse>) {
        with(producersFilterLayoutBinding) {
            handleFilterResponse(
                response = response,
                recyclerView = producerRecyclerView,
                progressBar = progressBar,
                emptyTextView = emptyTextView,
                retryButton = retryButton,
                updateAdapter = { dataList, selectedIds ->
                    val adapter =
                        producerRecyclerView.adapter as ProducerChipAdapter
                    adapter.items = dataList.filterIsInstance<Producer>()
                    adapter.selectedIds = selectedIds
                    adapter.notifyDataSetChanged()
                },
                additionalSuccessHandling = { responseData ->
                    updatePagination(responseData?.pagination)
                    if ((responseData?.data?.size ?: 0) <= 4) {
                        limitAndPaginationFragment.limitSpinner.visibility =
                            View.GONE
                    } else {
                        limitAndPaginationFragment.limitSpinner.visibility =
                            View.VISIBLE
                        setupLimitSpinner(
                            viewModel.producersQueryState.value.limit ?: 25
                        )
                    }
                }
            )
        }
    }

    private fun setupLimitSpinner(limit: Int) {
        val limitSpinner: Spinner =
            producersFilterLayoutBinding.limitAndPaginationFragment.limitSpinner
        val limitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            Limit.limitOptions
        )
        limitSpinner.adapter = limitAdapter
        limitSpinner.setSelection(Limit.limitOptions.indexOf(limit))

        limitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedLimit = Limit.getLimitValue(position)
                if (viewModel.producersQueryState.value.limit != selectedLimit) {
                    val updatedQueryState = viewModel.producersQueryState.value.copy(
                        limit = selectedLimit, page = 1
                    )
                    viewModel.applyProducerQueryStateFilters(updatedQueryState)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.applyProducerQueryStateFilters(
                    viewModel.producersQueryState.value.copy(
                        limit = 25,
                        page = 1
                    )
                )
            }
        }
    }

    private fun updatePagination(pagination: CompletePagination?) {
        producersFilterLayoutBinding.limitAndPaginationFragment.apply {
            Pagination.setPaginationButtons(
                paginationButtonContainer,
                pagination
            ) { pageNumber ->
                viewModel.applyProducerQueryStateFilters(
                    viewModel.producersQueryState.value.copy(
                        page = pageNumber
                    )
                )
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