package com.subconverter.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

class RemoteTextFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(30))
        .build(),
) {
    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val parsed = url.toHttpUrlOrNull()
            ?: error("URL 无效: $url")
        val token = parsed.password.ifBlank { parsed.username }.takeIf { it.isNotBlank() }
        val cleaned = if (token != null) {
            parsed.newBuilder().username("").password("").build()
        } else {
            parsed
        }

        val request = Request.Builder()
            .url(cleaned)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "text/plain, text/yaml, application/yaml, application/javascript, */*")
            .apply {
                if (token != null && isGithubUrl(cleaned.host)) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val preview = body.take(200).replace("\n", " ").ifBlank { "无响应体" }
                error("HTTP ${response.code} - ${cleaned.toString().replace("\n", " ")}\n$preview")
            }
            body
        }
    }

    private fun isGithubUrl(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "github.com" || lower.endsWith(".github.com") ||
            lower == "raw.githubusercontent.com" || lower.endsWith(".githubusercontent.com")
    }

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
