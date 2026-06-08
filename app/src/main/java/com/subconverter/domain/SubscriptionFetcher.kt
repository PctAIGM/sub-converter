package com.subconverter.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException

class SubscriptionFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(30))
        .build(),
    private val bootstrapClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(30))
        .build(),
) {
    private val dnsClients = ConcurrentHashMap<SubscriptionDnsConfig, OkHttpClient>()

    suspend fun fetch(
        url: String,
        userAgent: String = DEFAULT_USER_AGENT,
        dnsConfig: SubscriptionDnsConfig = SubscriptionDnsConfig(),
    ): FetchResult = withContext(Dispatchers.IO) {
        dnsConfig.validate()?.let(::error)
        when {
            dnsConfig.usesSystemDns -> execute(
                client,
                buildRequest(url.toHttpUrl(), userAgent),
            )
            dnsConfig.connectionMode == DnsConnectionMode.PRESERVE_DOMAIN -> {
                val dnsClient = dnsClients.getOrPut(dnsConfig) {
                    client.newBuilder()
                        .dns(EncryptedDns(dnsConfig, bootstrapClient))
                        .build()
                }
                execute(dnsClient, buildRequest(url.toHttpUrl(), userAgent))
            }
            else -> fetchWithIpUrl(url.toHttpUrl(), userAgent, dnsConfig)
        }
    }

    private fun fetchWithIpUrl(
        initialUrl: HttpUrl,
        userAgent: String,
        dnsConfig: SubscriptionDnsConfig,
    ): FetchResult {
        val resolver = EncryptedDns(dnsConfig, bootstrapClient)
        var logicalUrl = initialUrl
        var redirectCount = 0

        while (true) {
            val addresses = resolver.lookup(logicalUrl.host)
            var lastError: IOException? = null
            var redirectUrl: HttpUrl? = null
            for (address in addresses) {
                val ipHost = address.hostAddress
                    ?.substringBefore('%')
                    ?: continue
                val requestUrl = logicalUrl.newBuilder()
                    .host(ipHost)
                    .build()
                val request = buildRequest(requestUrl, userAgent)
                    .newBuilder()
                    .header("Host", hostHeader(logicalUrl))
                    .build()
                val requestClient = ipUrlClient(ipHost, dnsConfig.allowHostnameMismatch)

                try {
                    requestClient.newCall(request).execute().use { response ->
                        if (response.code in REDIRECT_CODES) {
                            val location = response.header("Location")
                                ?: error("HTTP ${response.code} 重定向缺少 Location")
                            redirectUrl = logicalUrl.resolve(location)
                                ?: error("重定向地址无效: $location")
                        } else {
                            return parseResponse(response)
                        }
                    }
                } catch (exception: IOException) {
                    lastError = exception
                    continue
                }

                if (redirectUrl != null) break
            }

            if (redirectUrl != null) {
                redirectCount++
                require(redirectCount <= MAX_REDIRECTS) { "重定向次数过多" }
                logicalUrl = redirectUrl
                continue
            }

            if (lastError is SSLPeerUnverifiedException && !dnsConfig.allowHostnameMismatch) {
                throw IOException(
                    "HTTPS 证书与解析 IP 不匹配，请启用“忽略证书主机名不匹配”或使用保留域名模式",
                    lastError,
                )
            }
            throw IOException(
                "使用解析 IP 请求失败: ${lastError?.message ?: "没有可用 IP"}",
                lastError,
            )
        }
    }

    private fun ipUrlClient(ipHost: String, allowHostnameMismatch: Boolean): OkHttpClient {
        val builder = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
        if (allowHostnameMismatch) {
            val defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
            builder.hostnameVerifier { hostname, session ->
                hostname == ipHost || defaultVerifier.verify(hostname, session)
            }
        }
        return builder.build()
    }

    private fun buildRequest(url: HttpUrl, userAgent: String): Request {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
        if (userAgent.isNotBlank()) {
            requestBuilder.header("User-Agent", userAgent)
        }
        return requestBuilder.build()
    }

    private fun execute(client: OkHttpClient, request: Request): FetchResult =
        client.newCall(request).execute().use(::parseResponse)

    private fun parseResponse(response: Response): FetchResult {
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            error("HTTP ${response.code}")
        }

        val profileTitle = response.header("profile-title")
            ?: response.header("Profile-Title")
            ?: parseContentDispositionFilename(response.header("content-disposition"))

        val profileWebPageUrl = response.header("profile-web-page-url")
            ?: response.header("Profile-Web-Page-Url")

        val profileUpdateIntervalHours = (response.header("profile-update-interval")
            ?: response.header("Profile-Update-Interval"))
            ?.trim()?.toIntOrNull()

        return FetchResult(
            yamlBody = body,
            statusCode = response.code,
            userInfo = SubscriptionUserInfoParser.parse(
                response.header("subscription-userinfo")
                    ?: response.header("Subscription-Userinfo"),
            ),
            profileTitle = profileTitle?.trim()?.takeIf { it.isNotBlank() },
            profileWebPageUrl = profileWebPageUrl?.trim()?.takeIf { it.isNotBlank() },
            profileUpdateIntervalHours = profileUpdateIntervalHours,
        )
    }

    private fun hostHeader(url: HttpUrl): String {
        val host = if (':' in url.host) "[${url.host}]" else url.host
        val defaultPort = (url.scheme == "http" && url.port == 80) ||
            (url.scheme == "https" && url.port == 443)
        return if (defaultPort) host else "$host:${url.port}"
    }

    companion object {
        const val MIHOMO_USER_AGENT = "ClashforWindows/0.20.39"
        const val DEFAULT_USER_AGENT = "SubConverter/1.0"
        private const val MAX_REDIRECTS = 10
        private val REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)

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
