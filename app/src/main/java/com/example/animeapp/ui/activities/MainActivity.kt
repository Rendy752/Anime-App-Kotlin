package com.example.animeapp.ui.activities

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.animeapp.R
import com.example.animeapp.databinding.ActivityMainBinding
import com.example.animeapp.data.local.database.AnimeDetailDatabase
import com.example.animeapp.data.local.database.AnimeRecommendationsDatabase
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.ui.viewmodels.AnimeRecommendationsViewModel
import com.example.animeapp.ui.providerfactories.AnimeRecommendationsViewModelProviderFactory
import com.example.animeapp.ui.providerfactories.AnimeDetailViewModelProviderFactory
import com.example.animeapp.ui.viewmodels.AnimeDetailViewModel
import com.example.animeapp.utils.LogUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ShakeDetector
import kotlinx.coroutines.launch
import kotlin.text.toIntOrNull

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    private val viewModel: AnimeRecommendationsViewModel by lazy {
        val repository = AnimeRecommendationsRepository(AnimeRecommendationsDatabase.getDatabase(this))
        val factory = AnimeRecommendationsViewModelProviderFactory(repository)
        ViewModelProvider(this, factory)[AnimeRecommendationsViewModel::class.java]
    }

    val animeRecommendationsViewModel: AnimeRecommendationsViewModel get() = viewModel

    val animeDetailViewModel: AnimeDetailViewModel by lazy {
        val repository = AnimeDetailRepository(AnimeDetailDatabase.getDatabase(this))
        val factory = AnimeDetailViewModelProviderFactory(repository)
        ViewModelProvider(this, factory)[AnimeDetailViewModel::class.java]
    }

    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSplashScreen()
        setupSensor()
        setupViewBinding()
        setupNavigation()
        setupActionViewIntent()
    }

    private fun setupSplashScreen() {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isDataLoaded }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.animeRecommendations.collect { resource ->
                    if (resource is Resource.Success) {
                        isDataLoaded = true
                    }
                }
            }
        }
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector { LogUtils.showLogs(this) }
    }

    private fun setupViewBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.animeRecommendationsFragment, R.id.aboutFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun setupActionViewIntent() {
        if (intent?.action == Intent.ACTION_VIEW && intent?.scheme == "animeapp") {
            handleAnimeUrl(intent.data)
        }
    }

    private fun handleAnimeUrl(uri: Uri?) {
        val id = uri?.getQueryParameter("id")?.toIntOrNull()
        if (id != null) {
            val navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)?.findNavController()

            navController?.let {
                val bundle = Bundle().apply { putInt("id", id) }
                val navOptions = NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()

                it.navigate(
                    R.id.action_animeRecommendationsFragment_to_animeDetailFragment,
                    bundle,
                    navOptions
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            shakeDetector,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }
}