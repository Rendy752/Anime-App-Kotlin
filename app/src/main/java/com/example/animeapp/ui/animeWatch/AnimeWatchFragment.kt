package com.example.animeapp.ui.animeWatch

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Rational
import androidx.fragment.app.Fragment
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animeapp.R
import androidx.media3.ui.R as RMedia3
import com.example.animeapp.databinding.FragmentAnimeWatchBinding
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeSourcesQuery
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodeWatch
import com.example.animeapp.models.Server
import com.example.animeapp.utils.BindAnimeUtils
import com.example.animeapp.utils.Debounce
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.HlsPlayerUtil.abandonAudioFocus
import com.example.animeapp.utils.HlsPlayerUtil.requestAudioFocus
import com.example.animeapp.utils.IntroOutroHandler
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnimeWatchFragment : Fragment() {

    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeWatchViewModel by viewModels()

    interface OnFullscreenRequestListener {
        fun onFullscreenRequested(fullscreen: Boolean)
    }

    private var mListener: OnFullscreenRequestListener? = null
    private lateinit var audioManager: AudioManager
    private var mediaSession: MediaSession? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFullscreenRequestListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFullscreenRequestListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupInitialData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeWatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackButtonListener()
        handleStaticAnimeDetailData()
        setupObservers()
    }

    private fun setupInitialData() {
        val episodeId = arguments?.getString("episodeId") ?: return
        val animeDetail: AnimeDetail = getParcelableArgument("animeDetail") ?: return
        val episodes: List<Episode> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList("episodes", Episode::class.java)
                ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList("episodes")
                ?: emptyList()
        }

        val defaultEpisode: EpisodeDetailComplement? =
            getParcelableArgument("defaultEpisode")

        (requireActivity() as AppCompatActivity).supportActionBar?.title = animeDetail.title
        viewModel.setInitialState(
            animeDetail,
            episodes,
            defaultEpisode,
        )
        viewModel.handleSelectedEpisodeServer(episodeId)
    }

    private inline fun <reified T : Any> getParcelableArgument(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("defaultEpisodeSources")

        }
    }

    private fun handleStaticAnimeDetailData() {
        binding.apply {
            viewModel.episodes.value.let { episodes ->
                "Total Episode: ${viewModel.animeDetail.value?.episodes}".also {
                    tvTotalEpisodes.text = it
                }

                val debounce = Debounce(
                    lifecycleScope,
                    1000L,
                    { query ->
                        if (query.isNotEmpty()) {
                            handleJumpToEpisode(
                                query.toInt(),
                                episodes ?: emptyList()
                            )
                        }
                    }
                )

                svEpisodeSearch.setOnQueryTextListener(object : OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        newText?.let { debounce.query(it) }
                        return true
                    }
                })

                previousEpisodeButton.setOnClickListener {
                    handleEpisodeNavigation(-1, binding.previousEpisodeButton)
                }
                nextEpisodeButton.setOnClickListener {
                    handleEpisodeNavigation(1, binding.nextEpisodeButton)
                }
                setupEpisodeRecyclerView(episodes ?: emptyList())
            }

            viewModel.animeDetail.value?.let { animeDetail ->
                BindAnimeUtils.bindAnimeHeader(
                    requireContext(),
                    animeHeader,
                    { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    },
                    animeDetail
                )

                val embedUrl = animeDetail.trailer.embed_url ?: ""
                if (embedUrl.isNotEmpty()) {
                    llYoutubePreview.visibility = View.VISIBLE
                    youtubePlayerView.playVideo(embedUrl)
                }

                with(animeSynopsis) {
                    animeDetail.synopsis?.let { synopsis ->
                        if (synopsis.isNotBlank()) {
                            tvSynopsis.visibility = View.VISIBLE
                            tvSynopsis.text = synopsis
                        } else {
                            tvSynopsis.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun handleEpisodeNavigation(direction: Int, button: android.widget.Button) {
        viewModel.episodeWatch.value.data?.servers?.let { currentServer ->
            viewModel.episodes.value?.let { episodes ->
                if (episodes.isNotEmpty()) {
                    button.isEnabled = true
                    val targetEpisodeNo = currentServer.episodeNo + direction
                    episodes.find { it.episodeNo == targetEpisodeNo }?.let { targetEpisode ->
                        viewModel.handleSelectedEpisodeServer(targetEpisode.episodeId)
                    }
                } else {
                    button.isEnabled = false
                }
            }
        }
    }

    private fun setupBackButtonListener() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    isFullscreen = false
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.episodeWatch.collect { response ->
                when (response) {
                    is Resource.Success -> handleEpisodeWatchSuccess(response)

                    is Resource.Error -> handleEpisodeWatchError()
                    is Resource.Loading -> handleEpisodeWatchLoading()
                }
            }
        }
    }

    private fun setupServerRecyclerView(
        textView: View,
        recyclerView: RecyclerView,
        servers: List<Server>,
        category: String,
        episodeId: String
    ) {
        if (servers.isNotEmpty()) {
            textView.visibility = View.VISIBLE
            recyclerView.visibility = View.VISIBLE
            val serverQueries = servers.map { server ->
                EpisodeSourcesQuery(episodeId, server.serverName, category)
            }
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = viewModel.episodeSourcesQuery.value?.let {
                    ServerAdapter(serverQueries, it) { episodeSourcesQuery ->
                        viewModel.handleSelectedEpisodeServer(episodeId, episodeSourcesQuery)
                    }
                }
            }
        } else {
            recyclerView.visibility = View.GONE
            textView.visibility = View.GONE
        }
    }

    private fun setupEpisodeRecyclerView(episodes: List<Episode>) {
        binding.apply {
            episodeSearchContainer.visibility = if (episodes.size > 1) View.VISIBLE else View.GONE
            rvEpisodes.apply {
                if (episodes.isNotEmpty()) {
                    layoutManager = GridLayoutManager(context, 4)
                    adapter = EpisodesWatchAdapter(requireContext(), episodes) {
                        viewModel.handleSelectedEpisodeServer(it)
                    }
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.episodeWatch.collect { response ->
                            if (response is Resource.Success) {
                                response.data?.servers?.episodeNo?.let {
                                    (adapter as EpisodesWatchAdapter).updateSelectedEpisode(it)
                                    handleJumpToEpisode(it, episodes)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleEpisodeWatchSuccess(response: Resource.Success<EpisodeWatch>) {
        response.data?.let { episodeWatch ->
            binding.apply {
                setupVideoPlayer(episodeWatch.sources)
                episodeInfoProgressBar.visibility = View.GONE
                tvEpisodeTitle.visibility = View.VISIBLE
                tvCurrentEpisode.visibility = View.VISIBLE
                serverScrollView.visibility = View.VISIBLE

                episodeWatch.servers.let { servers ->
                    viewModel.episodes.value.let { episodes ->
                        val episodeName = episodes?.find { episode ->
                            episode.episodeId == servers.episodeId

                        }?.name
                        tvEpisodeTitle.text =
                            if (episodeName != "Full") episodeName else viewModel.animeDetail.value?.title
                                ?: ""
                        episodes?.find { it.episodeId == servers.episodeId }
                            ?.let { episode ->
                                val backgroundColor = if (episode.filler) {
                                    ContextCompat.getColor(requireContext(), R.color.filler_episode)
                                } else {
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.default_episode
                                    )
                                }
                                tvEpisodeTitle.setTextColor(backgroundColor)
                            }
                        "Eps. ${servers.episodeNo}".also { tvCurrentEpisode.text = it }
                    }
                    setupServerRecyclerView(
                        tvSub,
                        rvSubServer,
                        servers.sub,
                        "sub",
                        servers.episodeId
                    )
                    setupServerRecyclerView(
                        tvDub,
                        rvDubServer,
                        servers.dub,
                        "dub",
                        servers.episodeId
                    )
                    setupServerRecyclerView(
                        tvRaw,
                        rvRawServer,
                        servers.raw,
                        "raw",
                        servers.episodeId
                    )
                }
            }
        }
    }

    private fun handleEpisodeWatchError() {
        viewModel.episodes.value?.first()?.episodeId?.let { viewModel.handleSelectedEpisodeServer(it) }
    }

    private fun handleEpisodeWatchLoading() {
        binding.apply {
            episodeInfoProgressBar.visibility = View.VISIBLE
            tvEpisodeTitle.visibility = View.GONE
            tvCurrentEpisode.visibility = View.GONE
            serverScrollView.visibility = View.GONE
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupVideoPlayer(sources: EpisodeSourcesResponse) {
        audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mediaSession?.release()
        mediaSession = null
        HlsPlayerUtil.releasePlayer(binding.playerViewContainer.playerView)

        binding.playerViewContainer.apply {
            val exoPlayer = ExoPlayer.Builder(requireActivity()).build()
            with(playerView) {
                player = exoPlayer
                setShowPreviousButton(false)
                setShowNextButton(false)

                setFullscreenButtonState(true)
                setFullscreenButtonClickListener {
                    val currentFullscreenState = isFullscreen
                    isFullscreen = !currentFullscreenState
                }

                var isHolding = false
                val handler = Handler(Looper.getMainLooper())
                val holdRunnable = Runnable {
                    if (isHolding) {
                        exoPlayer.playbackParameters =
                            exoPlayer.playbackParameters.withSpeed(2f)
                        useController = false
                        pipButton.visibility = View.GONE
                        speedUpContainer.visibility = View.VISIBLE
                        "2x speed".also { tvSpeedUp.text = it }
                    }
                }

                playerView.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isHolding = true
                            handler.postDelayed(holdRunnable, 1000)
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacks(holdRunnable)
                            exoPlayer.playbackParameters =
                                exoPlayer.playbackParameters.withSpeed(1f)
                            useController = true
                            speedUpContainer.visibility = View.GONE
                            "1x speed".also { tvSpeedUp.text = it }
                            isHolding = false
                        }

                    }

                    if (!isHolding) {
                        playerView.performClick()
                    }
                    true
                }

                setControllerVisibilityListener(
                    PlayerView.ControllerVisibilityListener { visibility ->
                        val subtitleView = playerView.subtitleView
                        val bottomBar =
                            playerView.findViewById<ViewGroup>(RMedia3.id.exo_bottom_bar)
                        val orientation = playerView.resources.configuration.orientation
                        pipButton.visibility = when (visibility) {
                            View.VISIBLE -> View.VISIBLE
                            else -> View.GONE
                        }
                        subtitleView?.setPadding(
                            0, 0, 0,
                            if (visibility == View.VISIBLE && orientation == Configuration.ORIENTATION_LANDSCAPE || (visibility == View.VISIBLE && !isFullscreen)) bottomBar.height else 0
                        )
                    }
                )
            }

            HlsPlayerUtil.initializePlayer(
                exoPlayer,
                introButton,
                outroButton,
                sources
            )

            val sessionId = "episode_${System.currentTimeMillis()}"
            mediaSession = MediaSession.Builder(requireActivity(), exoPlayer)
                .setId(sessionId)
                .build()

            exoPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)

                    if (isPlaying) {
                        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        requestAudioFocus(audioManager)
                    } else {
                        abandonAudioFocus(audioManager)
                    }
                    updateMediaSessionPlaybackState(exoPlayer)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    viewModel.episodeWatch.value.data?.servers.let { servers ->
                        viewModel.episodes.value?.let { episodes ->
                            if (playbackState == Player.STATE_ENDED) {
                                playerView.hideController()
                                if (episodes.isNotEmpty()) {
                                    val currentEpisode = servers?.episodeNo
                                    val nextEpisode = episodes.find {
                                        it.episodeNo == (currentEpisode?.plus(1) ?: 0)
                                    }
                                    if (nextEpisode == null) {
                                        nextEpisodeContainer.visibility = View.GONE
                                    } else {
                                        nextEpisodeContainer.visibility = View.VISIBLE
                                        tvNextEpisode.text = nextEpisode.name
                                        restartButton.setOnClickListener {
                                            exoPlayer.seekTo(0)
                                            exoPlayer.play()
                                            nextEpisodeContainer.visibility = View.GONE
                                        }
                                        skipNextEpisodeButton.setOnClickListener {
                                            viewModel.handleSelectedEpisodeServer(
                                                nextEpisode.episodeId
                                            )
                                            nextEpisodeContainer.visibility = View.GONE
                                        }
                                    }
                                } else {
                                    nextEpisodeContainer.visibility = View.GONE
                                }
                            } else {
                                nextEpisodeContainer.visibility = View.GONE
                            }
                        }
                    }

                    if (playbackState == Player.STATE_READY) {
                        if (!isFullscreen) {
                            this@apply.root.layoutParams.height =
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        } else {
                            this@apply.root.layoutParams.height =
                                ViewGroup.LayoutParams.MATCH_PARENT
                        }
                    }
                }
            })

            pipButton.setOnClickListener {
                handleEnterPictureInPictureMode()
            }
        }
    }

    private fun updateMediaSessionPlaybackState(player: Player) {
        val playbackState = when (player.playbackState) {
            Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
            Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            Player.STATE_READY -> if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            else -> PlaybackStateCompat.STATE_NONE
        }

        PlaybackStateCompat.Builder()
            .setState(playbackState, player.currentPosition, 1f, System.currentTimeMillis())
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)

        mediaSession?.setPlayer(player)
    }

    private var isFullscreen = false
        set(value) {
            field = value
            if (value) {
                handleEnterFullscreen()
            } else {
                handleExitFullscreen()
            }
        }

    private fun showContent() {
        binding.svContent.visibility = View.VISIBLE
        binding.playerViewContainer.apply {
            root.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            playerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    private fun hideContent() {
        binding.svContent.visibility = View.GONE
        binding.playerViewContainer.apply {
            root.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun handleExitFullscreen() {
        mListener?.onFullscreenRequested(false)
        showContent()
    }

    private fun handleEnterFullscreen() {
        mListener?.onFullscreenRequested(true)
        hideContent()
    }

    fun handleEnterPictureInPictureMode() {
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        requireActivity().enterPictureInPictureMode(pipParams)
    }

    private fun handleJumpToEpisode(episodeNumber: Int, episodes: List<Episode>) {
        val foundEpisodeIndex = episodes.indexOfFirst { it.episodeNo == episodeNumber }

        if (foundEpisodeIndex != -1) {
            binding.rvEpisodes.apply {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    foundEpisodeIndex,
                    0
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        binding.playerViewContainer.playerView.apply {
            useController = !isInPictureInPictureMode
            if (isInPictureInPictureMode) {
                if (!isFullscreen) hideContent()
            } else {
                if (!isFullscreen) showContent()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playerViewContainer.apply {
            val introOutroHandler = IntroOutroHandler(
                playerView.player as ExoPlayer,
                introButton,
                outroButton,
                EpisodeSourcesResponse(emptyList(), null, null, emptyList(), 0, 0)
            )
            introOutroHandler.releaseButtons()
        }
        HlsPlayerUtil.releasePlayer(binding.playerViewContainer.playerView)
        mediaSession?.release()
        mediaSession = null
        _binding = null
    }
}