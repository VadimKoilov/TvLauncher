package com.example.tvlauncher.info

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object Http {

    private const val TIMEOUT_MS = 10_000
    private const val TAG = "TvLauncher"
    private const val USER_AGENT = "TvLauncher/1.0 (Android TV)"

    /** Тело ответа или null при любой ошибке сети и ответе не 200. */
    fun get(url: String, charset: Charset = Charsets.UTF_8): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GET $url → HTTP ${connection.responseCode}")
                null
            } else {
                connection.inputStream.use { it.readBytes().toString(charset) }
            }
        } catch (e: IOException) {
            Log.w(TAG, "GET $url failed: $e")
            null
        } finally {
            connection.disconnect()
        }
    }
}
