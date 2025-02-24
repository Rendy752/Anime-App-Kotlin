package com.example.animeapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.animeapp.utils.Navigation
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSplashScreen()
        setupViewBinding()
        setupNavigation()
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

        val navView: BottomNavigationView = binding.navView
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
        ) {
            handleAnimeUrl(intent.data)
        }
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
            } else {
                Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}