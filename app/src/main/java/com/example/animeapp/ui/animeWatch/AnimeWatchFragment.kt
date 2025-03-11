package com.example.animeapp.ui.animeWatch

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.animeapp.R
import com.example.animeapp.databinding.FragmentAnimeWatchBinding
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.Episode
import com.example.animeapp.models.EpisodeDetailComplement
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnimeWatchFragment : Fragment() {

    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimeWatchViewModel by viewModels()

    var isFullscreen = false

    interface OnFullscreenRequestListener {
        fun onFullscreenRequested(fullscreen: Boolean)
    }

    private var mListener: OnFullscreenRequestListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFullscreenRequestListener) mListener = context
        else throw RuntimeException("$context must implement OnFullscreenRequestListener")
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
        setupFragments()
        updateLayoutForOrientation()
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
        viewModel.setInitialState(animeDetail, episodes, defaultEpisode)
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

    private fun setupFragments() {
        childFragmentManager.beginTransaction()
            .replace(R.id.player_fragment_container, AnimeWatchPlayerFragment())
            .replace(R.id.header_fragment_container, AnimeWatchHeaderFragment())
            .replace(R.id.episode_fragment_container, AnimeWatchEpisodeFragment())
            .replace(R.id.info_fragment_container, AnimeWatchInfoFragment())
            .commit()
    }

    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) handleEnterFullscreen()
        else handleExitFullscreen()
    }

    private fun setupBackButtonListener() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    toggleFullscreen()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    fun handleEnterPictureInPictureMode() {
        isFullscreen = true

        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        requireActivity().enterPictureInPictureMode(pipParams)
    }

    fun showContent() {
        binding.svContent.visibility = View.VISIBLE
        binding.playerFragmentContainer.apply {
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    fun hideContent() {
        binding.svContent.visibility = View.GONE
        binding.playerFragmentContainer.apply {
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun handleExitFullscreen() {
        mListener?.onFullscreenRequested(false)
        showContent()
        updateLayoutForOrientation()
    }

    private fun handleEnterFullscreen() {
        mListener?.onFullscreenRequested(true)
        hideContent()
        updateLayoutForOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayoutForOrientation()
    }

    fun updateLayoutForOrientation() {
        val orientation = resources.configuration.orientation
        val layoutParamsPlayer =
            binding.playerFragmentContainer.layoutParams as ConstraintLayout.LayoutParams
        val layoutParamsSvContent = binding.svContent.layoutParams as ConstraintLayout.LayoutParams

        if (isFullscreen) {
            // Fullscreen mode: player takes up the entire screen
            layoutParamsPlayer.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParamsPlayer.height = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParamsPlayer.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsPlayer.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsPlayer.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsPlayer.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

            layoutParamsSvContent.width = 0
            layoutParamsSvContent.height = 0
            binding.svContent.visibility = View.GONE
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape, non fullscreen mode, side by side
            layoutParamsPlayer.width = 0
            layoutParamsPlayer.height = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParamsPlayer.endToStart = binding.guidelineVertical.id
            layoutParamsPlayer.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsPlayer.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsPlayer.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

            layoutParamsSvContent.width = 0
            layoutParamsSvContent.height = 0
            layoutParamsSvContent.startToStart = binding.guidelineVertical.id
            layoutParamsSvContent.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsSvContent.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsSvContent.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            binding.svContent.visibility = View.VISIBLE
        } else {
            // Portrait mode, non fullscreen mode, up and down
            layoutParamsPlayer.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParamsPlayer.height = resources.getDimensionPixelSize(R.dimen.player_height)
            layoutParamsPlayer.endToStart = ConstraintLayout.LayoutParams.UNSET
            layoutParamsPlayer.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsPlayer.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsPlayer.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsPlayer.bottomToBottom = ConstraintLayout.LayoutParams.UNSET

            layoutParamsSvContent.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParamsSvContent.height = 0
            layoutParamsSvContent.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsSvContent.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParamsSvContent.topToTop = ConstraintLayout.LayoutParams.UNSET
            layoutParamsSvContent.topToBottom = binding.playerFragmentContainer.id
            layoutParamsSvContent.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            binding.svContent.visibility = View.VISIBLE
        }

        binding.playerFragmentContainer.layoutParams = layoutParamsPlayer
        binding.svContent.layoutParams = layoutParamsSvContent
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}