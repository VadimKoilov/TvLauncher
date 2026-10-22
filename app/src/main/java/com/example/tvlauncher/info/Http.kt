package com.example.tvlauncher.info

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Итог запроса: тело ответа 200, другой код от сервера или сервер недоступен (нет сети, DNS, таймаут). */
sealed interface HttpResult {
    data class Ok(val body: String) : HttpResult
    data class Status(val code: Int) : HttpResult
    data object Unreachable : HttpResult
}

object Http {

    private const val TAG = "TvLauncher"
    private const val TIMEOUT_MS = 10_000
    private const val USER_AGENT = "TvLauncher/1.0 (Android TV)"

    fun request(url: String): HttpResult {
        var code: Int? = null
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            Log.w(TAG, "GET $url failed: $e")
            return HttpResult.Unreachable
        }
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GET $url → HTTP $code")
                HttpResult.Status(code)
            } else {
                HttpResult.Ok(connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) })
            }
        } catch (e: IOException) {
            Log.w(TAG, "GET $url failed: $e")
            // Ответ уже пришёл, но тело не дочиталось — сервер всё-таки отвечал.
            code?.let { HttpResult.Status(it) } ?: HttpResult.Unreachable
        } finally {
            connection.disconnect()
        }
    }

    /** Тело ответа 200 или null. */
    fun get(url: String): String? = (request(url) as? HttpResult.Ok)?.body
}
