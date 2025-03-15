package com.example.animeapp.utils

import android.util.Log
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.media3.exoplayer.ExoPlayer
import com.example.animeapp.models.EpisodeSourcesResponse

class IntroOutroHandler(
    private val player: ExoPlayer,
    private var introButton: Button?,
    private var outroButton: Button?,
    private val videoData: EpisodeSourcesResponse
) : Runnable {

    private var introSkipped = false
    private var outroSkipped = true
    private val handler = Handler(Looper.getMainLooper())
    private var lastIntroVisibility: Int? = null
    private var lastOutroVisibility: Int? = null
    private var isHandlerRunning = false

    fun start() {
        if (!isHandlerRunning) {
            isHandlerRunning = true
            handler.post(this)
        }
    }

    override fun run() {
        if (!isHandlerRunning) return

        val currentPositionSec = player.currentPosition / 1000

        Log.d("IntroOutroHandler", "Current Position: $currentPositionSec")
        val intro = videoData.intro
        val outro = videoData.outro

        if (intro != null && currentPositionSec in intro.start..intro.end && !introSkipped) {
            if (introButton?.visibility != View.VISIBLE) {
                if (lastIntroVisibility != View.VISIBLE) {
                    introButton?.visibility = View.VISIBLE
                    lastIntroVisibility = View.VISIBLE
                    setupIntroSkipButton(intro.end)
                }
            }
        } else {
            if (introButton?.visibility == View.VISIBLE) {
                if (lastIntroVisibility != View.GONE) {
                    introButton?.visibility = View.GONE
                    lastIntroVisibility = View.GONE
                }
            }
        }

        if (outro != null && currentPositionSec in outro.start..outro.end && !outroSkipped) {
            if (outroButton?.visibility != View.VISIBLE) {
                if (lastOutroVisibility != View.VISIBLE) {
                    outroButton?.visibility = View.VISIBLE
                    lastOutroVisibility = View.VISIBLE
                    setupOutroSkipButton(outro.end)
                }
            }
        } else {
            if (outroButton?.visibility == View.VISIBLE) {
                if (lastOutroVisibility != View.GONE) {
                    outroButton?.visibility = View.GONE
                    lastOutroVisibility = View.GONE
                }
            }
        }

        if (intro != null && (currentPositionSec < intro.start || currentPositionSec > intro.end)) {
            introSkipped = false
        }

        if (outro != null && (currentPositionSec < outro.start || currentPositionSec > outro.end)) {
            outroSkipped = false
        }

        handler.postDelayed(this, 1000)
    }

    private fun setupIntroSkipButton(endTime: Long) {
        introButton?.setOnClickListener {
            player.seekTo(endTime * 1000L)
            introButton?.visibility = View.GONE
            introSkipped = true
            lastIntroVisibility = View.GONE
        }
    }

    private fun setupOutroSkipButton(endTime: Long) {
        outroButton?.setOnClickListener {
            player.seekTo(endTime * 1000L)
            outroButton?.visibility = View.GONE
            outroSkipped = true
            lastOutroVisibility = View.GONE
        }
    }

    fun releaseButtons() {
        introButton = null
        outroButton = null
    }

    fun stop() {
        isHandlerRunning = false
        handler.removeCallbacks(this)
    }
}