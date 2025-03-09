package com.example.animeapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.animeapp.R
import com.example.animeapp.models.NetworkStatus

class NetworkStateMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _downstreamSpeed = MutableLiveData<Int>()

    private val _networkStatus = MutableLiveData<NetworkStatus>()
    val networkStatus: LiveData<NetworkStatus> = _networkStatus

    init {
        checkConnectivity(context)
    }

    private fun checkConnectivity(context: Context) {
        val network = connectivityManager.activeNetwork ?: return
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return
        val isCurrentlyConnected = when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
        _isConnected.postValue(isCurrentlyConnected)

        _downstreamSpeed.postValue(activeNetwork.linkDownstreamBandwidthKbps)

        updateNetworkStatus(activeNetwork.linkDownstreamBandwidthKbps, context)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.postValue(true)
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return
            _downstreamSpeed.postValue(activeNetwork.linkDownstreamBandwidthKbps)
            updateNetworkStatus(activeNetwork.linkDownstreamBandwidthKbps, context)
        }

        override fun onLost(network: Network) {
            _isConnected.postValue(false)
            _downstreamSpeed.postValue(0)
            updateNetworkStatus(0, context)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            _downstreamSpeed.postValue(networkCapabilities.linkDownstreamBandwidthKbps)
            updateNetworkStatus(networkCapabilities.linkDownstreamBandwidthKbps, context)
        }
    }

    private fun updateNetworkStatus(speed: Int, context: Context) {
        val connectivityManager = connectivityManager
        val network = connectivityManager.activeNetwork
        val activeNetwork = connectivityManager.getNetworkCapabilities(network)
        val networkInfo = connectivityManager.activeNetworkInfo

        val isAirplaneModeOn = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0

        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val isCellularDataEnabled =
            telephonyManager.dataState == TelephonyManager.DATA_CONNECTED || telephonyManager.dataState == TelephonyManager.DATA_CONNECTING

        if (isAirplaneModeOn) {
            _networkStatus.postValue(
                NetworkStatus(
                    R.drawable.ic_airplanemode_blue_24dp,
                    "Airplane"
                )
            )
            return
        }

        if (networkInfo == null || !networkInfo.isConnected) {
            if (!isCellularDataEnabled) {
                _networkStatus.postValue(
                    NetworkStatus(
                        R.drawable.ic_cellular_off_red_24dp,
                        "Cellular Off"
                    )
                )
            } else {
                _networkStatus.postValue(NetworkStatus(R.drawable.ic_wifi_off_red_24dp, "Offline"))
            }
        } else if (activeNetwork != null) {
            if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                when (speed) {
                    in 1..1000 -> _networkStatus.postValue(
                        NetworkStatus(R.drawable.ic_wifi_1_bar_orange_24dp, "$speed Kbps")
                    )

                    in 1001..2000 -> _networkStatus.postValue(
                        NetworkStatus(R.drawable.ic_wifi_2_bar_yellow_24dp, "$speed Kbps")
                    )

                    else -> _networkStatus.postValue(
                        NetworkStatus(R.drawable.ic_wifi_3_bar_green_24dp, "$speed Kbps")
                    )
                }
            } else if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                when (speed) {
                    in 1..1000 -> _networkStatus.postValue(
                        NetworkStatus(
                            R.drawable.ic_cellular_1_bar_orange_24dp, "$speed Kbps"
                        )
                    )

                    in 1001..2000 -> _networkStatus.postValue(
                        NetworkStatus(
                            R.drawable.ic_cellular_2_bar_yellow_24dp, "$speed Kbps"
                        )
                    )

                    else -> _networkStatus.postValue(
                        NetworkStatus(
                            R.drawable.ic_cellular_3_bar_green_24dp, "$speed Kbps"
                        )
                    )
                }
            } else {
                _networkStatus.postValue(NetworkStatus(R.drawable.ic_wifi_off_red_24dp, "Unknown"))
            }
        } else {
            if (!isCellularDataEnabled) {
                _networkStatus.postValue(
                    NetworkStatus(
                        R.drawable.ic_cellular_off_red_24dp,
                        "Cellular Off"
                    )
                )
            } else {
                _networkStatus.postValue(NetworkStatus(R.drawable.ic_wifi_off_red_24dp, "Offline"))
            }
        }
    }

    fun startMonitoring(context: Context) {
        val builder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)

        checkConnectivity(context)
    }

    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}