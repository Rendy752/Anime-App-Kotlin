package com.example.animeapp.utils

import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

object Theme {
    fun setTheme(context: Context, isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            setStatusBarColor(context, android.R.color.black)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            setStatusBarColor(context, android.R.color.white)
        }
    }

    private fun setStatusBarColor(context: Context, colorResId: Int) {
        if (context is AppCompatActivity) {
            val window: Window = context.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(context, colorResId)

            val decorView: View = window.decorView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                decorView.windowInsetsController?.setSystemBarsAppearance(
                    if (colorResId == android.R.color.black) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                val flags = decorView.systemUiVisibility
                decorView.systemUiVisibility = if (colorResId == android.R.color.black) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }

    fun isDarkMode(): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else -> false
        }
    }
}