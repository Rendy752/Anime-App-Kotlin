package com.example.animeapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.animeapp.R
import com.example.animeapp.databinding.ActivityMainBinding
import com.example.animeapp.ui.animeWatch.AnimeWatchFragment
import com.example.animeapp.utils.Navigation
import com.example.animeapp.utils.Theme
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AnimeWatchFragment.OnFullscreenRequestListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navView: BottomNavigationView

    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSplashScreen()
        setupViewBinding()
        setupNavigation()
        Theme.setTheme(this, Theme.isDarkMode())
    }

    private fun setupSplashScreen() {
        installSplashScreen()
    }

    private fun setupViewBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setupNavigation() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBarsInsets.left,
                top = systemBarsInsets.top,
                right = systemBarsInsets.right,
            )
            insets
        }

        navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.animeRecommendationsFragment,
                R.id.animeSearchFragment,
                R.id.settingsFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW &&
            intent.scheme == "animeapp" &&
            intent.data != null
        ) handleAnimeUrl(intent.data)
    }

    private fun handleAnimeUrl(uri: Uri?) {
        uri?.pathSegments?.let { segments ->
            if (segments.size >= 2 && segments[0] == "detail") {
                val animeId = segments[1].toIntOrNull()
                if (animeId != null) {
                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
                    val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
                    if (currentFragment != null) {
                        Navigation.navigateToAnimeDetail(
                            currentFragment,
                            animeId,
                            R.id.action_global_animeDetailFragment
                        )
                    }
                }
            } else Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) hideActionAndBottomNavBar()
        else {
            if (isFullscreen) hideActionAndBottomNavBar()
            else showActionAndBottomNavBar()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)

        if (currentFragment is AnimeWatchFragment && currentFragment.isVisible) currentFragment.handleEnterPictureInPictureMode()
    }

    override fun onFullscreenRequested(fullscreen: Boolean) {
        isFullscreen = fullscreen
        if (fullscreen) enterFullscreen()
        else exitFullscreen()
    }

    private fun enterFullscreen() {
        hideActionAndBottomNavBar()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun exitFullscreen() {
        showActionAndBottomNavBar()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.insetsController?.show(
            WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
        )
        else window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    private fun hideActionAndBottomNavBar() {
        navView.visibility = View.GONE
        supportActionBar?.hide()
    }

    private fun showActionAndBottomNavBar() {
        navView.visibility = View.VISIBLE
        supportActionBar?.show()
    }
}