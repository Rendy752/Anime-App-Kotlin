package com.example.animeapp.ui.animeWatch

import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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

    private fun setupBackButtonListener() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) isFullscreen = false
                else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private var isFullscreen = false
        set(value) {
            field = value
            if (value) handleEnterFullscreen()
            else handleExitFullscreen()
        }

    fun handleEnterPictureInPictureMode() {
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

    fun handleExitFullscreen() {
        mListener?.onFullscreenRequested(false)
        showContent()
    }

    fun handleEnterFullscreen() {
        mListener?.onFullscreenRequested(true)
        hideContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}