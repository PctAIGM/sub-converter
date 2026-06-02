package com.subconverter.domain

import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.LinkedHashMap

object ShareLinkParser {
    fun parseToMihomoProxies(rawText: String): List<LinkedHashMap<String, Any?>> {
        val links = extractLinks(rawText)
        if (links.isEmpty()) return emptyList()
        return links.mapNotNull(::parseLink)
    }

    private fun extractLinks(rawText: String): List<String> {
        val direct = normalizeLines(rawText).filter(::looksLikeLink)
        if (direct.isNotEmpty()) return direct

        val decoded = decodeBase64Text(rawText) ?: return emptyList()
        return normalizeLines(decoded).filter(::looksLikeLink)
    }

    private fun normalizeLines(text: String): List<String> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith('#') }

    private fun looksLikeLink(line: String): Boolean {
        val marker = line.indexOf("://")
        if (marker <= 0) return false
        val scheme = line.substring(0, marker).lowercase()
        return scheme in setOf("vless", "vmess", "trojan", "ss", "hy2", "hysteria2")
    }

    private fun parseLink(link: String): LinkedHashMap<String, Any?>? {
        val scheme = link.substringBefore("://", "").lowercase()
        return when (scheme) {
            "vless" -> parseVless(link)
            "vmess" -> parseVmess(link)
            "trojan" -> parseTrojan(link)
            "ss" -> parseSs(link)
            "hy2", "hysteria2" -> parseHy2(link)
            else -> null
        }
    }

    private fun parseVless(link: String): LinkedHashMap<String, Any?>? {
        val uri = runCatching { URI(link) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it > 0 } ?: return null
        val uuid = uri.userInfo?.trim().orEmpty().ifBlank { return null }
        val params = parseQuery(uri.rawQuery)
        return buildTlsStyleProxy(
            base = linkedMapOf(
                "name" to nodeName(uri, host, port),
                "type" to "vless",
                "server" to host,
                "port" to port,
                "uuid" to uuid,
                "udp" to true,
            ),
            params = params,
        )
    }

    private fun parseVmess(link: String): LinkedHashMap<String, Any?>? {
        val payload = link.removePrefix("vmess://")
        val jsonText = decodeBase64Text(payload) ?: return null
        val json = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
        val host = json.optString("add").takeIf { it.isNotBlank() } ?: return null
        val port = json.optString("port").toIntOrNull()?.takeIf { it > 0 } ?: return null
        val uuid = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val tls = json.optString("tls")
        val network = json.optString("net").ifBlank { json.optString("type") }
        val path = json.optString("path")
        val wsHost = json.optString("host")
        val sni = json.optString("sni")

        return linkedMapOf<String, Any?>(
            "name" to json.optString("ps").ifBlank { "$host:$port" },
            "type" to "vmess",
            "server" to host,
            "port" to port,
            "uuid" to uuid,
            "alterId" to json.optString("aid").toIntOrNull().orDefault(0),
            "cipher" to json.optString("scy").ifBlank { "auto" },
            "udp" to true,
        ).apply {
            if (network.isNotBlank()) this["network"] = network
            if (tls.equals("tls", ignoreCase = true)) this["tls"] = true
            if (sni.isNotBlank()) this["servername"] = sni
            if (network == "ws") {
                this["ws-opts"] = buildMap {
                    if (path.isNotBlank()) put("path", path)
                    if (wsHost.isNotBlank()) put("headers", mapOf("Host" to wsHost))
                }
            }
        }
    }

    private fun parseTrojan(link: String): LinkedHashMap<String, Any?>? {
        val uri = runCatching { URI(link) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it > 0 } ?: return null
        val password = uri.userInfo?.trim().orEmpty().ifBlank { return null }
        val params = parseQuery(uri.rawQuery)
        return buildTlsStyleProxy(
            base = linkedMapOf(
                "name" to nodeName(uri, host, port),
                "type" to "trojan",
                "server" to host,
                "port" to port,
                "password" to password,
                "udp" to true,
            ),
            params = params,
            defaultTls = true,
        )
    }

    private fun parseSs(link: String): LinkedHashMap<String, Any?>? {
        val uri = runCatching { URI(link) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it > 0 } ?: return null

        val decodedUserInfo = decodeBase64Text(uri.userInfo ?: "").orEmpty()
        val authText = if (decodedUserInfo.contains(':')) decodedUserInfo else uri.userInfo.orEmpty()
        val marker = authText.indexOf(':')
        if (marker <= 0 || marker >= authText.lastIndex) return null

        val cipher = authText.substring(0, marker)
        val password = authText.substring(marker + 1)
        return linkedMapOf(
            "name" to nodeName(uri, host, port),
            "type" to "ss",
            "server" to host,
            "port" to port,
            "cipher" to cipher,
            "password" to password,
            "udp" to true,
        )
    }

    private fun parseHy2(link: String): LinkedHashMap<String, Any?>? {
        val uri = runCatching { URI(link) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it > 0 } ?: return null
        val params = parseQuery(uri.rawQuery)
        val password = uri.userInfo?.takeIf { it.isNotBlank() } ?: params["password"] ?: return null
        return linkedMapOf<String, Any?>(
            "name" to nodeName(uri, host, port),
            "type" to "hysteria2",
            "server" to host,
            "port" to port,
            "password" to password,
            "udp" to true,
        ).apply {
            params["sni"]?.takeIf { it.isNotBlank() }?.let { this["sni"] = it }
            params["insecure"]?.toBooleanStrictOrNull()?.let { this["skip-cert-verify"] = it }
        }
    }

    private fun buildTlsStyleProxy(
        base: LinkedHashMap<String, Any?>,
        params: Map<String, String>,
        defaultTls: Boolean = false,
    ): LinkedHashMap<String, Any?> {
        val network = params["type"].orEmpty().ifBlank { params["network"].orEmpty() }
        val security = params["security"].orEmpty()
        val sni = params["sni"].orEmpty()
        val host = params["host"].orEmpty()
        val path = params["path"].orEmpty()

        return base.apply {
            if (network.isNotBlank() && network != "tcp") this["network"] = network

            val tlsEnabled = defaultTls || (security.isNotBlank() && security != "none")
            if (tlsEnabled) this["tls"] = true
            if (sni.isNotBlank()) this["servername"] = sni
            params["alpn"]?.takeIf { it.isNotBlank() }?.let {
                this["alpn"] = it.split(',').map(String::trim).filter(String::isNotBlank)
            }
            params["fp"]?.takeIf { it.isNotBlank() }?.let { this["client-fingerprint"] = it }
            params["flow"]?.takeIf { it.isNotBlank() }?.let { this["flow"] = it }
            params["allowInsecure"]?.toBooleanStrictOrNull()?.let { this["skip-cert-verify"] = it }

            if (security == "reality") {
                val realityOpts = linkedMapOf<String, Any?>()
                params["pbk"]?.takeIf { it.isNotBlank() }?.let { realityOpts["public-key"] = it }
                params["sid"]?.takeIf { it.isNotBlank() }?.let { realityOpts["short-id"] = it }
                params["spx"]?.takeIf { it.isNotBlank() }?.let { realityOpts["spider-x"] = it }
                if (realityOpts.isNotEmpty()) {
                    this["reality-opts"] = realityOpts
                }
            }

            when (network) {
                "ws" -> {
                    val wsOpts = linkedMapOf<String, Any?>()
                    if (path.isNotBlank()) wsOpts["path"] = path
                    if (host.isNotBlank()) wsOpts["headers"] = mapOf("Host" to host)
                    if (wsOpts.isNotEmpty()) this["ws-opts"] = wsOpts
                }

                "grpc" -> {
                    val serviceName = params["serviceName"].orEmpty().ifBlank { params["service-name"].orEmpty() }
                    if (serviceName.isNotBlank()) {
                        this["grpc-opts"] = mapOf("grpc-service-name" to serviceName)
                    }
                }
            }
        }
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&')
            .mapNotNull { pair ->
                val pieces = pair.split('=', limit = 2)
                val key = decode(pieces[0]).trim()
                if (key.isBlank()) return@mapNotNull null
                val value = pieces.getOrElse(1) { "" }
                key to decode(value)
            }
            .toMap()
    }

    private fun decodeBase64Text(raw: String): String? {
        val compact = raw.trim().replace("\\s".toRegex(), "")
        if (compact.isBlank() || compact.length < 8) return null

        val candidates = listOf(
            compact,
            compact.replace('-', '+').replace('_', '/'),
        )

        return candidates.firstNotNullOfOrNull { text ->
            val padded = text + "=".repeat((4 - text.length % 4) % 4)
            runCatching {
                String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8)
            }.getOrNull()
        }
    }

    private fun nodeName(uri: URI, host: String, port: Int): String {
        val decoded = decode(uri.rawFragment.orEmpty())
        return decoded.ifBlank { "$host:$port" }
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun Int?.orDefault(default: Int): Int = this ?: default
}
