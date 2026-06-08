package com.subconverter.domain

import com.subconverter.data.SubscriptionSourceEntity
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

enum class DnsProtocol {
    DOH,
    DOT,
    ;

    companion object {
        fun fromStorage(value: String): DnsProtocol? =
            entries.firstOrNull { it.name == value.trim().uppercase() }
    }
}

enum class DnsConnectionMode {
    PRESERVE_DOMAIN,
    IP_URL,
    ;

    companion object {
        fun fromStorage(value: String): DnsConnectionMode =
            entries.firstOrNull { it.name == value.trim().uppercase() } ?: PRESERVE_DOMAIN
    }
}

data class DnsPreset(
    val id: String,
    val label: String,
    val protocol: DnsProtocol,
    val server: String,
)

object PublicDnsPresets {
    val all = listOf(
        DnsPreset("cloudflare_doh", "Cloudflare · DoH", DnsProtocol.DOH, "https://cloudflare-dns.com/dns-query"),
        DnsPreset("cloudflare_dot", "Cloudflare · DoT", DnsProtocol.DOT, "one.one.one.one:853"),
        DnsPreset("google_doh", "Google · DoH", DnsProtocol.DOH, "https://dns.google/dns-query"),
        DnsPreset("google_dot", "Google · DoT", DnsProtocol.DOT, "dns.google:853"),
        DnsPreset("tencent_doh", "腾讯 DNSPod · DoH", DnsProtocol.DOH, "https://doh.pub/dns-query"),
        DnsPreset("tencent_dot", "腾讯 DNSPod · DoT", DnsProtocol.DOT, "dot.pub:853"),
        DnsPreset("ali_doh", "阿里 AliDNS · DoH", DnsProtocol.DOH, "https://dns.alidns.com/dns-query"),
        DnsPreset("ali_dot", "阿里 AliDNS · DoT", DnsProtocol.DOT, "dns.alidns.com:853"),
    )
}

data class SubscriptionDnsConfig(
    val protocol: DnsProtocol? = null,
    val server: String = "",
    val connectionMode: DnsConnectionMode = DnsConnectionMode.PRESERVE_DOMAIN,
    val allowHostnameMismatch: Boolean = false,
) {
    val usesSystemDns: Boolean
        get() = protocol == null

    fun validate(): String? {
        return when (protocol) {
            null -> null
            DnsProtocol.DOH -> {
                val endpoint = server.trim().toHttpUrlOrNull()
                when {
                    endpoint == null -> "DoH 地址格式无效"
                    endpoint.scheme != "https" -> "DoH 地址必须使用 HTTPS"
                    endpoint.username.isNotEmpty() || endpoint.password.isNotEmpty() -> "DoH 地址不能包含用户名或密码"
                    else -> null
                }
            }
            DnsProtocol.DOT -> runCatching { DotEndpoint.parse(server) }
                .exceptionOrNull()
                ?.message
        }
    }

    companion object {
        fun from(source: SubscriptionSourceEntity): SubscriptionDnsConfig {
            val protocol = DnsProtocol.fromStorage(source.dnsProtocol)
            return SubscriptionDnsConfig(
                protocol = protocol,
                server = if (protocol == null) "" else source.dnsServer.trim(),
                connectionMode = if (protocol == null) {
                    DnsConnectionMode.PRESERVE_DOMAIN
                } else {
                    DnsConnectionMode.fromStorage(source.dnsConnectionMode)
                },
                allowHostnameMismatch = protocol != null &&
                    DnsConnectionMode.fromStorage(source.dnsConnectionMode) == DnsConnectionMode.IP_URL &&
                    source.allowHostnameMismatch,
            )
        }
    }
}

internal data class DotEndpoint(
    val host: String,
    val port: Int,
) {
    companion object {
        fun parse(value: String): DotEndpoint {
            val input = value.trim()
            require(input.isNotEmpty()) { "DoT 服务器不能为空" }
            require("://" !in input && "/" !in input && "?" !in input && "#" !in input) {
                "DoT 服务器格式应为主机名或主机名:端口"
            }

            val host: String
            val portText: String?
            if (input.startsWith("[")) {
                val closing = input.indexOf(']')
                require(closing > 1) { "DoT IPv6 地址格式无效" }
                host = input.substring(1, closing)
                val suffix = input.substring(closing + 1)
                require(suffix.isEmpty() || suffix.startsWith(":")) { "DoT IPv6 地址格式无效" }
                portText = if (suffix.isEmpty()) null else suffix.removePrefix(":")
            } else if (input.count { it == ':' } == 1) {
                host = input.substringBeforeLast(':')
                portText = input.substringAfterLast(':')
            } else {
                host = input
                portText = null
            }

            require(host.isNotBlank()) { "DoT 主机名不能为空" }
            val urlHost = if (':' in host) "[$host]" else host
            val canonicalHost = "https://$urlHost/".toHttpUrlOrNull()?.host
            require(!canonicalHost.isNullOrBlank()) { "DoT 主机名格式无效" }
            val port = if (portText == null) {
                853
            } else {
                require(portText.isNotEmpty() && portText.all(Char::isDigit)) {
                    "DoT 端口必须为数字"
                }
                portText.toIntOrNull() ?: error("DoT 端口必须在 1-65535 之间")
            }
            require(port in 1..65535) { "DoT 端口必须在 1-65535 之间" }
            return DotEndpoint(canonicalHost, port)
        }
    }
}
