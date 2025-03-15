package com.example.animeapp.ui.animeWatch

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as RMedia3
import com.example.animeapp.databinding.FragmentAnimeWatchPlayerBinding
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.utils.HlsPlayerUtil
import com.example.animeapp.utils.HlsPlayerUtil.abandonAudioFocus
import com.example.animeapp.utils.HlsPlayerUtil.requestAudioFocus
import com.example.animeapp.utils.IntroOutroHandler
import com.example.animeapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlaybackException

@AndroidEntryPoint
class AnimeWatchPlayerFragment : Fragment() {

    private var _binding: FragmentAnimeWatchPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeWatchViewModel by viewModels({ requireParentFragment() })

    private lateinit var audioManager: AudioManager
    private var mediaSession: MediaSession? = null

    private var introOutroHandler: IntroOutroHandler? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeWatchPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.episodeDetailComplement.collect { response ->
                    when (response) {
                        is Resource.Success -> response.data?.let { setupVideoPlayer(it.sources) }
                        is Resource.Loading -> handleEpisodeWatchLoading()
                        is Resource.Error -> handleEpisodeWatchError()
                    }
                }
            }
        }
    }

    private fun handleEpisodeWatchLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun handleEpisodeWatchError() {
        binding.progressBar.visibility = View.GONE
        Toast.makeText(
            context,
            "Failed to fetch episode sources, return back to first episode",
            Toast.LENGTH_SHORT
        ).show()
        viewModel.episodes.value?.first()?.episodeId?.let { episodeId ->
            viewModel.episodeSourcesQuery.value?.let { query ->
                viewModel.handleSelectedEpisodeServer(query.copy(id = episodeId))
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupVideoPlayer(sources: EpisodeSourcesResponse) {
        audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSession?.release()
        mediaSession = null
        HlsPlayerUtil.releasePlayer(binding.playerView)
        binding.apply {
            progressBar.visibility = View.GONE
            val exoPlayer = ExoPlayer.Builder(requireActivity()).build()
            with(playerView) {
                player = exoPlayer
                setShowPreviousButton(false)
                setShowNextButton(false)
                setFullscreenButtonState(true)
                setFullscreenButtonClickListener {
                    (parentFragment as? AnimeWatchFragment)?.toggleFullscreen()
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
                            if (visibility == View.VISIBLE && orientation == Configuration.ORIENTATION_LANDSCAPE ||
                                (visibility == View.VISIBLE && !(parentFragment as AnimeWatchFragment).isFullscreen)
                            ) bottomBar.height else 0
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

            introOutroHandler?.stop()
            introOutroHandler = IntroOutroHandler(exoPlayer, introButton, outroButton, sources)
            introOutroHandler?.start()

            val sessionId = "episode_${System.currentTimeMillis()}"
            mediaSession = MediaSession.Builder(requireActivity(), exoPlayer)
                .setId(sessionId)
                .build()

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_SOURCE) {
                        Toast.makeText(
                            context,
                            "Playback error, try a different server.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)

                    if (isPlaying) {
                        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        requestAudioFocus(audioManager)
                    } else abandonAudioFocus(audioManager)
                    updateMediaSessionPlaybackState(exoPlayer)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    viewModel.episodeDetailComplement.value.data?.servers.let { servers ->
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
                                            viewModel.episodeSourcesQuery.value?.let { query ->
                                                viewModel.handleSelectedEpisodeServer(
                                                    query.copy(id = nextEpisode.episodeId)
                                                )
                                            }
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
                }
            })

            pipButton.setOnClickListener {
                (parentFragment as AnimeWatchFragment).handleEnterPictureInPictureMode()
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

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        binding.playerView.useController = !isInPictureInPictureMode
    }

    override fun onDestroyView() {
        super.onDestroyView()
        introOutroHandler?.stop()
        introOutroHandler?.releaseButtons()
        HlsPlayerUtil.releasePlayer(binding.playerView)
        mediaSession?.release()
        mediaSession = null
        _binding = null
    }
}