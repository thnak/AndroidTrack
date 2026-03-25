package com.androidtrack.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.NetworkInterface
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the current Wi-Fi RSSI value by listening to system [ConnectivityManager] callbacks.
 * Also provides the device MAC address if available.
 *
 * Call [startObserving] once (e.g. when the simulation starts) and [stopObserving] when done.
 * [getRssi] returns [RSSI_UNKNOWN] when there is no Wi-Fi connection or the callback has not
 * yet fired.
 */
@Singleton
class WifiInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val RSSI_UNKNOWN = Int.MIN_VALUE
        private const val DEFAULT_MAC = "02:00:00:00:00:00"
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Volatile
    private var currentRssi: Int = RSSI_UNKNOWN

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            val wifiInfo = capabilities.transportInfo as? WifiInfo
            currentRssi = wifiInfo?.rssi ?: RSSI_UNKNOWN
        }

        override fun onLost(network: Network) {
            currentRssi = RSSI_UNKNOWN
        }
    }

    private var isObserving = false

    /**
     * Registers the Wi-Fi network callback. Safe to call multiple times (no-op if already active).
     */
    fun startObserving() {
        if (isObserving) return
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isObserving = true
        } catch (e: Exception) {
            // Permission or system service unavailable – RSSI will remain RSSI_UNKNOWN.
            Log.w("WifiInfoProvider", "Could not register network callback", e)
        }
    }

    /**
     * Unregisters the Wi-Fi network callback. Safe to call even if not observing.
     */
    fun stopObserving() {
        if (!isObserving) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w("WifiInfoProvider", "Error unregistering network callback", e)
        } finally {
            isObserving = false
            currentRssi = RSSI_UNKNOWN
        }
    }

    /** Returns the last known RSSI in dBm, or [RSSI_UNKNOWN] if unavailable. */
    fun getRssi(): Int = currentRssi

    /**
     * Attempts to retrieve the Wi-Fi MAC address.
     * Note: On Android 6.0+, standard APIs return "02:00:00:00:00:00".
     * This method attempts to read it from network interfaces as a fallback.
     */
    fun getMacAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                if (!networkInterface.name.equals("wlan0", ignoreCase = true)) continue

                val macBytes = networkInterface.hardwareAddress ?: return DEFAULT_MAC
                val res = StringBuilder()
                for (b in macBytes) {
                    res.append(String.format("%02X:", b))
                }

                if (res.isNotEmpty()) {
                    res.deleteCharAt(res.length - 1)
                }
                return res.toString()
            }
        } catch (e: Exception) {
            Log.e("WifiInfoProvider", "Error getting MAC address", e)
        }
        return DEFAULT_MAC
    }
}
