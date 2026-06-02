package com.subconverter.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

class SubscriptionFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(30))
        .build(),
) {
    suspend fun fetch(
        url: String,
        userAgent: String = DEFAULT_USER_AGENT,
    ): FetchResult = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
        if (userAgent.isNotBlank()) {
            requestBuilder.header("User-Agent", userAgent)
        }
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }

            val profileTitle = response.header("profile-title")
                ?: response.header("Profile-Title")
                ?: parseContentDispositionFilename(response.header("content-disposition"))

            val profileWebPageUrl = response.header("profile-web-page-url")
                ?: response.header("Profile-Web-Page-Url")

            FetchResult(
                yamlBody = body,
                statusCode = response.code,
                userInfo = SubscriptionUserInfoParser.parse(
                    response.header("subscription-userinfo")
                        ?: response.header("Subscription-Userinfo"),
                ),
                profileTitle = profileTitle?.trim()?.takeIf { it.isNotBlank() },
                profileWebPageUrl = profileWebPageUrl?.trim()?.takeIf { it.isNotBlank() },
            )
        }
    }

    companion object {
        const val MIHOMO_USER_AGENT = "ClashforWindows/0.20.39"
        const val DEFAULT_USER_AGENT = "SubConverter/1.0"

        internal fun parseContentDispositionFilename(header: String?): String? {
            if (header.isNullOrBlank()) return null
            val filenamePart = header.split(';').map { it.trim() }.find {
                it.startsWith("filename=", ignoreCase = true)
            } ?: return null
            val name = filenamePart.removePrefix("filename=").removeSurrounding("\"").trim()
            return name.removeSuffix(".yaml").removeSuffix(".yml").takeIf { it.isNotBlank() }
        }
    }
}
