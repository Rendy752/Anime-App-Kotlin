package com.example.animeapp.ui.animeDetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.animeapp.R
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.databinding.FragmentDetailBinding
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.ui.common.NameAndUrlAdapter
import com.example.animeapp.ui.common.TitleSynonymsAdapter
import com.example.animeapp.ui.common.UnorderedListAdapter
import com.example.animeapp.utils.Const.Companion.YOUTUBE_URL
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.TextUtils.formatSynopsis
import com.example.animeapp.utils.TextUtils.joinOrNA
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AnimeDetailFragment : Fragment(), MenuProvider {
    @Inject
    lateinit var animeAPI: AnimeAPI

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        observeAnimeDetail()
        fetchAnimeDetail()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeAnimeDetail() {
        viewModel.animeDetail.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> handleSuccess(response)
                is Resource.Error -> handleError(response)
                is Resource.Loading -> handleLoading()
                else -> handleEmpty()
            }
        }
    }

    private fun fetchAnimeDetail() {
        val animeId = arguments?.getInt("id")
        if (animeId != null) {
            viewModel.getAnimeDetail(animeId)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.detail_fragment_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_share -> {
                viewModel.animeDetail.value?.data?.data?.let { detail ->
                    val animeUrl = detail.url
                    val animeTitle = detail.title
                    val animeScore = detail.score ?: "0"
                    val animeGenres = detail.genres?.joinToString(", ") { it.name }

                    val animeSynopsis = formatSynopsis(detail.synopsis ?: "-")
                    val animeTrailerUrl = detail.trailer.url ?: ""
                    val malId = detail.mal_id
                    val customUrl = "animeapp://anime/detail/$malId"

                    val trailerSection = if (animeTrailerUrl.isNotEmpty()) {
                        """
                            
                    -------
                    Trailer
                    -------
                    $animeTrailerUrl
                    """
                    } else {
                        ""
                    }

                    val sharedText = """
                    Check out this anime on AnimeApp!

                    Title: $animeTitle
                    Score: $animeScore
                    Genres: $animeGenres

                    --------
                    Synopsis
                    --------
                    $animeSynopsis
                    $trailerSection

                    Web URL: $animeUrl
                    App URL: $customUrl
                    Download the app now: https://play.google.com/store/apps/details?id=com.example.animeapp
                """.trimIndent()

                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, sharedText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)
                }
                true
            }

            else -> false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun handleSuccess(response: Resource.Success<AnimeDetailResponse>) {
        binding.apply {
            shimmerViewContainer.stopShimmer()
            shimmerViewContainer.hideShimmer()
            tvError.visibility = View.GONE

            response.data?.data?.let { detail ->
                Glide.with(requireContext()).load(detail.images.jpg.large_image_url)
                    .into(ivAnimeImage)
                tvTitle.text = detail.title
                tvTitle.setOnClickListener {
                    viewModel.animeDetail.value?.data?.data?.let { detail ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detail.url))
                        startActivity(intent)
                    }
                }
                tvTitle.setOnLongClickListener {
                    val clipboard =
                        ContextCompat.getSystemService(
                            requireContext(),
                            ClipboardManager::class.java
                        )
                    val clip = ClipData.newPlainText("Anime Title", tvTitle.text.toString())
                    clipboard?.setPrimaryClip(clip)
                    true
                }
                tvEnglishTitle.text = detail.title_english
                tvJapaneseTitle.text = detail.title_japanese
                rvTitleSynonyms.apply {
                    adapter = detail.title_synonyms?.let { TitleSynonymsAdapter(it.toList()) }
                    layoutManager = LinearLayoutManager(
                        requireContext(), LinearLayoutManager.HORIZONTAL, false
                    )
                }

                when (detail.approved) {
                    true -> {
                        ivApproved.visibility = View.VISIBLE
                    }

                    false -> {
                        ivApproved.visibility = View.GONE
                    }
                }

                when (detail.airing) {
                    true -> {
                        tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            R.drawable.ic_notifications_active_24dp,
                            0
                        )
                    }

                    false -> {
                        tvAiredStatus.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            R.drawable.ic_done_24dp,
                            0
                        )
                    }
                }

                tvStatus.text = detail.status
                tvType.text = detail.type
                tvSource.text = detail.source
                tvSeason.text = detail.season ?: "-"
                tvReleased.text = detail.year?.toString() ?: "-"
                tvAired.text = detail.aired.string
                tvRating.text = detail.rating ?: "Unknown"
                tvGenres.text = joinOrNA(detail.genres) { it.name }
                detail.episodes.toString().also { tvEpisodes.text = it }

                tvStudios.text = joinOrNA(detail.studios) { it.name }
                tvProducers.text = joinOrNA(detail.producers) { it.name }
                tvLicensors.text = joinOrNA(detail.licensors) { it.name }
                tvBroadcast.text = detail.broadcast.string ?: "-"
                tvDuration.text = detail.duration

                val embedUrl = detail.trailer.embed_url ?: ""
                if (embedUrl.isNotEmpty()) {
                    llYoutubePreview.visibility = View.VISIBLE
                    youtubePlayerView.playVideo(embedUrl)
                }

                tvScore.text = detail.score?.toString() ?: "0"
                tvScoredBy.text =
                    resources.getString(R.string.scored_by_users, detail.scored_by ?: 0)
                tvRanked.text =
                    resources.getString(R.string.ranked_number, detail.rank ?: 0)
                tvPopularity.text =
                    resources.getString(R.string.popularity_number, detail.popularity)
                detail.members.toString().also { tvMembers.text = it }
                detail.favorites.toString().also { tvFavorites.text = it }

                detail.background?.let { background ->
                    if (background.isNotBlank()) {
                        llBackground.visibility = View.VISIBLE
                        tvBackground.text = background
                    } else {
                        llBackground.visibility = View.GONE
                    }
                }

                tvSynopsis.text = detail.synopsis ?: "-"

                if (detail.relations?.size!! > 0) {
                    if (detail.relations.size > 1) "${detail.relations.size} Relations".also {
                        tvRelation.text = it
                    }
                    rvRelations.apply {
                        adapter = RelationsAdapter(animeAPI, detail.relations) { animeId ->
                            Navigation.navigateToAnimeDetail(
                                this@AnimeDetailFragment,
                                animeId,
                                R.id.action_animeDetailFragment_self
                            )
                        }
                        layoutManager = LinearLayoutManager(
                            requireContext(), LinearLayoutManager.HORIZONTAL, false
                        )
                    }
                } else {
                    relationContainer.visibility = View.GONE
                }

                if (detail.theme.openings?.size!! > 0) {
                    rvOpening.apply {
                        adapter = detail.theme.openings.let {
                            UnorderedListAdapter(it) { opening ->
                                val encodedOpening = Uri.encode(opening)
                                val youtubeSearchUrl =
                                    "${YOUTUBE_URL}results?search_query=$encodedOpening"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeSearchUrl))
                                startActivity(intent)
                            }
                        }
                        layoutManager = LinearLayoutManager(
                            requireContext()
                        )
                    }
                } else {
                    openingContainer.visibility = View.GONE
                }

                if (detail.theme.endings?.size!! > 0) {
                    rvEnding.apply {
                        adapter = detail.theme.endings.let {
                            UnorderedListAdapter(it)
                            { ending ->
                                val encodedEnding = Uri.encode(ending)
                                val youtubeSearchUrl =
                                    "${YOUTUBE_URL}results?search_query=$encodedEnding"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeSearchUrl))
                                startActivity(intent)
                                startActivity(intent)
                            }
                        }
                        layoutManager = LinearLayoutManager(
                            requireContext()
                        )
                    }
                } else {
                    endingContainer.visibility = View.GONE
                }

                if (detail.external?.size!! > 0) {
                    rvExternal.apply {
                        adapter = NameAndUrlAdapter(detail.external)
                        layoutManager = LinearLayoutManager(
                            requireContext()
                        )
                    }
                } else {
                    externalContainer.visibility = View.GONE
                }

                if (detail.streaming?.size!! > 0) {
                    rvStreaming.apply {
                        adapter =
                            NameAndUrlAdapter(detail.streaming)
                        layoutManager = LinearLayoutManager(
                            requireContext()
                        )
                    }
                } else {
                    streamingContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun handleError(response: Resource.Error<AnimeDetailResponse>) {
        binding.apply {
            shimmerViewContainer.stopShimmer()
            shimmerViewContainer.hideShimmer()
            tvError.visibility = View.VISIBLE
            "An error occurred: ${response.message}".also { tvError.text = it }
        }
    }

    private fun handleLoading() {
        binding.apply {
            shimmerViewContainer.showShimmer(true)
            shimmerViewContainer.startShimmer()
        }
    }

    private fun handleEmpty() {
        binding.apply {
            shimmerViewContainer.stopShimmer()
            shimmerViewContainer.hideShimmer()
            tvError.visibility = View.VISIBLE
            "No results found".also { tvError.text = it }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}