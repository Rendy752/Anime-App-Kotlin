package com.example.animeapp.utils

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.AudioFocusRequest
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import androidx.media3.common.C
import com.example.animeapp.models.EpisodeSourcesResponse
import androidx.core.net.toUri

object HlsPlayerUtil {

    private var audioFocusChangeListener: OnAudioFocusChangeListener? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false

    private var handler: Handler? = null

    @OptIn(UnstableApi::class)
    fun initializePlayer(
        player: ExoPlayer,
        introButton: Button,
        outroButton: Button,
        videoData: EpisodeSourcesResponse
    ) {
        introButton.visibility = View.GONE
        outroButton.visibility = View.GONE
        introButton.setOnClickListener(null)
        outroButton.setOnClickListener(null)

        val introOutroHandler = IntroOutroHandler(player, introButton, outroButton, videoData)
        introOutroHandler.let { handler?.removeCallbacks(it) }

        if (videoData.sources.isNotEmpty() && videoData.sources[0].type == "hls") {

            val mediaItemUri = videoData.sources[0].url.toUri()
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(mediaItemUri)

            val subtitleConfigurations = mutableListOf<SubtitleConfiguration>()

            videoData.tracks.filter { it.kind == "captions" }.forEach { track ->
                val subtitleConfiguration = SubtitleConfiguration.Builder(track.file.toUri())
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(track.label?.substringBefore("-")?.trim())
                    .setSelectionFlags(if (track.default == true) C.SELECTION_FLAG_DEFAULT else 0)
                    .setLabel(track.label?.substringBefore("-")?.trim())
                    .build()
                subtitleConfigurations.add(subtitleConfiguration)
            }

            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations)

            player.setMediaItem(mediaItemBuilder.build())
            player.prepare()

            handler = Handler(Looper.getMainLooper())
            handler?.post(introOutroHandler)

            audioFocusChangeListener = OnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        player.volume = 1f
                        if (player.playbackState == Player.STATE_READY) {
                            player.play()
                        }
                    }

                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        player.pause()
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        player.volume = 0.5f
                    }
                }
            }
        }
    }

    fun requestAudioFocus(audioManager: AudioManager) {
        if (!audioFocusRequested) {
            if (audioFocusRequest == null) {
                Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(
                        audioFocusChangeListener ?: OnAudioFocusChangeListener { })
                    .build()
            }

            val result = audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocusRequested = true
            }
        }
    }

    fun abandonAudioFocus(audioManager: AudioManager) {
        if (audioFocusRequested) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequested = false
        }
    }

    fun releasePlayer(playerView: PlayerView) {
        playerView.player?.release()
        playerView.player = null
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }
}