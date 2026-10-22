package com.example.tvlauncher.info

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.example.tvlauncher.BuildConfig
import java.io.File

/** Есть ли интернет: у активной сети подтверждён выход в интернет (VALIDATED). */
class Connectivity(private val context: Context) {

    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun isOnline(): Boolean {
        // Только для проверки офлайн-вида без обрыва adb: `run-as … touch files/force_offline`.
        if (BuildConfig.DEBUG && File(context.filesDir, FORCE_OFFLINE).exists()) return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** [onChange] вызывается на главном потоке при каждом изменении сети. */
    fun watch(onChange: (Boolean) -> Unit) {
        unwatch()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onChange(isOnline())
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = onChange(isOnline())
            override fun onLost(network: Network) = onChange(isOnline())
        }
        manager.registerDefaultNetworkCallback(cb, main)
        callback = cb
    }

    fun unwatch() {
        callback?.let(manager::unregisterNetworkCallback)
        callback = null
    }

    private companion object {
        const val FORCE_OFFLINE = "force_offline"
    }
}
