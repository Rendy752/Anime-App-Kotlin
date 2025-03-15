package com.example.animeapp.ui.animeSearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentAnimeSearchBinding
import com.example.animeapp.databinding.FragmentBottomSheetFilterBinding
import com.example.animeapp.utils.FilterUtils
import com.example.animeapp.utils.MinMaxInputFilter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnimeSearchFragment : Fragment(), MenuProvider {

    private var _binding: FragmentAnimeSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeSearchViewModel by viewModels()

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
        setupRefreshFloatingActionButton()

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.filterFragment, FilterFragment())
                .replace(R.id.resultsFragment, ResultsFragment())
                .replace(R.id.limitAndPaginationFragment, LimitAndPaginationFragment())
                .commit()
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.search_fragment_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_filter -> {
                showFilterBottomSheet()
                true
            }

            else -> false
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetBinding = FragmentBottomSheetFilterBinding.inflate(layoutInflater)

        bottomSheetBinding.apply {
            bottomSheetDialog.setContentView(root)

            populateBottomSheetFilters(this)

            resetButton.setOnClickListener {
                if (viewModel.queryState.value.isDefault()) {
                    Toast.makeText(requireContext(), "No filters applied yet", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    viewModel.resetBottomSheetFilters()
                    bottomSheetDialog.dismiss()
                }
            }

            applyButton.setOnClickListener {
                if (FilterUtils.collectFilterValues(
                        viewModel.queryState.value,
                        this
                    ) == viewModel.queryState.value.resetBottomSheetFilters()
                ) {
                    Toast.makeText(
                        requireContext(),
                        "No filters applied, you can reset",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.applyFilters(
                        FilterUtils.collectFilterValues(
                            viewModel.queryState.value,
                            this
                        )
                    )
                    bottomSheetDialog.dismiss()
                }
            }
        }

        bottomSheetDialog.setOnShowListener { dialog ->
            val bottomSheet =
                (dialog as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = bottomSheet?.let { BottomSheetBehavior.from(it) }

            bottomSheet?.apply {
                val layoutParams = layoutParams as CoordinatorLayout.LayoutParams
                val horizontalMargin =
                    resources.getDimensionPixelSize(R.dimen.bottom_sheet_horizontal_margin)
                layoutParams.leftMargin = horizontalMargin
                layoutParams.rightMargin = horizontalMargin

                background =
                    MaterialShapeDrawable.createWithElevationOverlay(requireContext()).apply {
                        shapeAppearanceModel =
                            shapeAppearanceModel.toBuilder()
                                .setTopLeftCorner(CornerFamily.ROUNDED, 40f)
                                .setTopRightCorner(CornerFamily.ROUNDED, 40f)
                                .build()
                    }
            }

            behavior?.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                maxHeight = resources.displayMetrics.heightPixels / 2
            }
        }

        bottomSheetDialog.show()
    }

    private fun populateBottomSheetFilters(binding: FragmentBottomSheetFilterBinding) {
        val currentFilterState = viewModel.queryState.value
        binding.apply {
            typeSpinner.setText(currentFilterState.type ?: "Any")
            statusSpinner.setText(currentFilterState.status ?: "Any")
            ratingSpinner.setText(
                FilterUtils.getRatingDescription(
                    currentFilterState.rating ?: "Any"
                )
            )

            val minMaxFilter = MinMaxInputFilter.createDouble(1.0, 10.0)
            scoreEditText.filters = arrayOf(minMaxFilter)
            scoreEditText.setText(currentFilterState.score.toString())
            minScoreEditText.filters = arrayOf(minMaxFilter)
            minScoreEditText.setText(currentFilterState.minScore.toString())
            maxScoreEditText.filters = arrayOf(minMaxFilter)
            maxScoreEditText.setText(currentFilterState.maxScore.toString())

            orderBySpinner.setText(currentFilterState.orderBy ?: "Any")
            sortSpinner.setText(currentFilterState.sort ?: "Any")

            enableDateRangeCheckBox.setOnCheckedChangeListener { _, isChecked ->
                startDateLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
                startDateTf.visibility = if (isChecked) View.VISIBLE else View.GONE
                endDateLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
                endDateTf.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            val startDateString = currentFilterState.startDate
            if (startDateString != null) {
                val startDate = startDateString.split("-")
                if (startDate.size == 3) {
                    startDatePicker.updateDate(
                        startDate[0].toInt(),
                        startDate[1].toInt() - 1,
                        startDate[2].toInt()
                    )
                }
            }
            val endDateString = currentFilterState.endDate
            if (endDateString != null) {
                val endDate = endDateString.split("-")
                if (endDate.size == 3) {
                    endDatePicker.updateDate(
                        endDate[0].toInt(),
                        endDate[1].toInt() - 1,
                        endDate[2].toInt()
                    )
                }
            }

            enableDateRangeCheckBox.isChecked =
                currentFilterState.startDate != null && currentFilterState.endDate != null
            sfwCheckBox.isChecked = currentFilterState.sfw ?: false
            unapprovedCheckBox.isChecked = currentFilterState.unapproved ?: false

            val typeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.TYPE_OPTIONS
            )
            typeSpinner.setAdapter(typeAdapter)

            val statusAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.STATUS_OPTIONS
            )
            statusSpinner.setAdapter(statusAdapter)

            val ratingAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.RATING_OPTIONS.map { ratingCode ->
                    FilterUtils.getRatingDescription(ratingCode)
                }
            )
            ratingSpinner.setAdapter(ratingAdapter)

            val orderByAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.ORDER_BY_OPTIONS
            )
            orderBySpinner.setAdapter(orderByAdapter)

            val sortAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                FilterUtils.SORT_OPTIONS
            )
            sortSpinner.setAdapter(sortAdapter)
        }
    }

    private fun setupRefreshFloatingActionButton() {
        binding.fabRefresh.setOnClickListener {
            viewModel.applyFilters(viewModel.queryState.value)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}