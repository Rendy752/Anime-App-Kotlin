package com.example.animeapp

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.chuckerteam.chucker.api.Chucker
import com.example.animeapp.utils.ShakeDetector
import com.example.animeapp.utils.Theme
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AnimeApplication : Application() {
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate() {
        super.onCreate()
        setupTheme()
        setupSensor()
    }

    private fun isDarkMode(): Boolean {
        val sharedPrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        return sharedPrefs.getBoolean("is_dark_mode", false)
    }

    private fun setupTheme() {
        Theme.setTheme(this, isDarkMode())
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector { startActivity(Chucker.getLaunchIntent(this)) }

        sensorManager.registerListener(
            shakeDetector,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

}