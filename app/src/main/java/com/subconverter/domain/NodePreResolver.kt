package com.subconverter.domain

import com.subconverter.data.NodeDnsCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import java.net.IDN
import java.net.Inet4Address
import java.time.Duration

internal data class ResolvedNodeAddress(
    val ipAddress: String,
    val ttlSeconds: Long,
)

internal fun interface NodeAddressResolver {
    fun resolve(hostname: String, config: SubscriptionDnsConfig): ResolvedNodeAddress
}

internal data class NodeResolutionResult(
    val entries: List<NodeDnsCacheEntity>,
    val addressByHostname: Map<String, String>,
    val successCount: Int,
    val failureCount: Int,
)

class NodePreResolver internal constructor(
    private val resolver: NodeAddressResolver = NetworkNodeAddressResolver(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    internal suspend fun refresh(
        sourceId: Long,
        hostnames: Set<String>,
        existingEntries: List<NodeDnsCacheEntity>,
        config: SubscriptionDnsConfig,
    ): NodeResolutionResult {
        val normalizedHostnames = hostnames.mapNotNull(::normalizeNodeHostname).toSet()
        val fingerprint = config.nodeResolutionFingerprint()
        val now = nowMillis()
        val existingByHostname = existingEntries.associateBy { it.hostname }
        val validEntries = normalizedHostnames.mapNotNull { hostname ->
            existingByHostname[hostname]?.takeIf {
                it.configFingerprint == fingerprint && it.expiresAt > now
            }
        }
        val validHostnames = validEntries.mapTo(mutableSetOf()) { it.hostname }
        val unresolvedHostnames = normalizedHostnames - validHostnames
        val semaphore = Semaphore(MAX_CONCURRENT_LOOKUPS)

        val newEntries = coroutineScope {
            unresolvedHostnames.map { hostname ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        runCatching {
                            val resolved = resolver.resolve(hostname, config)
                            val ttlSeconds = if (config.usesSystemDns) {
                                SYSTEM_DNS_TTL_SECONDS
                            } else {
                                resolved.ttlSeconds.coerceIn(MIN_TTL_SECONDS, MAX_TTL_SECONDS)
                            }
                            NodeDnsCacheEntity(
                                sourceId = sourceId,
                                hostname = hostname,
                                ipAddress = resolved.ipAddress,
                                expiresAt = now + ttlSeconds * 1_000,
                                configFingerprint = fingerprint,
                            )
                        }.getOrNull()
                    }
                }
            }.awaitAll().filterNotNull()
        }

        val entries = (validEntries + newEntries).sortedBy { it.hostname }
        return NodeResolutionResult(
            entries = entries,
            addressByHostname = entries.associate { it.hostname to it.ipAddress },
            successCount = entries.size,
            failureCount = normalizedHostnames.size - entries.size,
        )
    }

    private companion object {
        const val MAX_CONCURRENT_LOOKUPS = 3
        const val SYSTEM_DNS_TTL_SECONDS = 3_600L
        const val MIN_TTL_SECONDS = 300L
        const val MAX_TTL_SECONDS = 86_400L
    }
}

private class NetworkNodeAddressResolver(
    private val bootstrapClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(30))
        .build(),
) : NodeAddressResolver {
    override fun resolve(hostname: String, config: SubscriptionDnsConfig): ResolvedNodeAddress {
        val record = EncryptedDns(config, bootstrapClient)
            .lookupPreferredRecord(hostname)
        return ResolvedNodeAddress(
            ipAddress = requireNotNull(record.address.hostAddress).substringBefore('%'),
            ttlSeconds = record.ttlSeconds,
        )
    }
}

internal fun selectPreferredDnsRecord(records: List<DnsAddressRecord>): DnsAddressRecord =
    records.firstOrNull { it.address is Inet4Address } ?: records.first()

internal fun SubscriptionDnsConfig.nodeResolutionFingerprint(): String =
    listOf(
        protocol?.name.orEmpty(),
        server.trim().lowercase(),
        connectionMode.name,
        allowHostnameMismatch.toString(),
    ).joinToString("|")

internal fun normalizeNodeHostname(value: String): String? {
    val input = value.trim().removePrefix("[").removeSuffix("]").removeSuffix(".")
    if (input.isEmpty() || ':' in input || isIpv4Address(input)) return null
    return runCatching { IDN.toASCII(input).lowercase() }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}

private fun isIpv4Address(value: String): Boolean {
    val parts = value.split('.')
    return parts.size == 4 && parts.all { part ->
        part.isNotEmpty() &&
            part.length <= 3 &&
            part.all(Char::isDigit) &&
            part.toInt() in 0..255
    }
}
