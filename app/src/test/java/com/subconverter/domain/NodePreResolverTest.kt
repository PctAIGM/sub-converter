package com.subconverter.domain

import com.subconverter.data.NodeDnsCacheEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

class NodePreResolverTest {
    @Test
    fun reusesValidEntriesAndResolvesExpiredHosts() = runBlocking {
        val now = 1_000_000L
        val config = SubscriptionDnsConfig()
        val calls = mutableListOf<String>()
        val resolver = NodePreResolver(
            resolver = NodeAddressResolver { hostname, _ ->
                calls += hostname
                ResolvedNodeAddress("2.2.2.2", 10)
            },
            nowMillis = { now },
        )
        val existing = listOf(
            entry("valid.example", "1.1.1.1", now + 1, config),
            entry("expired.example", "1.1.1.2", now, config),
            entry("removed.example", "1.1.1.3", now + 1, config),
        )

        val result = resolver.refresh(
            sourceId = 7,
            hostnames = setOf("valid.example", "expired.example"),
            existingEntries = existing,
            config = config,
        )

        assertEquals(listOf("expired.example"), calls)
        assertEquals(
            mapOf(
                "expired.example" to "2.2.2.2",
                "valid.example" to "1.1.1.1",
            ),
            result.addressByHostname,
        )
        assertEquals(now + 3_600_000, result.entries.first { it.hostname == "expired.example" }.expiresAt)
        assertEquals(2, result.successCount)
        assertEquals(0, result.failureCount)
    }

    @Test
    fun clampsEncryptedDnsTtlAndKeepsFailuresAsDomains() = runBlocking {
        val now = 2_000_000L
        val config = SubscriptionDnsConfig(
            protocol = DnsProtocol.DOH,
            server = "https://dns.example/dns-query",
        )
        val resolver = NodePreResolver(
            resolver = NodeAddressResolver { hostname, _ ->
                when (hostname) {
                    "short.example" -> ResolvedNodeAddress("3.3.3.3", 1)
                    "long.example" -> ResolvedNodeAddress("4.4.4.4", 999_999)
                    else -> error("lookup failed")
                }
            },
            nowMillis = { now },
        )

        val result = resolver.refresh(
            sourceId = 8,
            hostnames = setOf("short.example", "long.example", "failed.example"),
            existingEntries = emptyList(),
            config = config,
        )

        assertEquals(now + 300_000, result.entries.first { it.hostname == "short.example" }.expiresAt)
        assertEquals(now + 86_400_000, result.entries.first { it.hostname == "long.example" }.expiresAt)
        assertEquals(2, result.successCount)
        assertEquals(1, result.failureCount)
        assertTrue("failed.example" !in result.addressByHostname)
    }

    @Test
    fun prefersFirstIpv4Record() {
        val ipv6 = DnsAddressRecord(InetAddress.getByName("2001:db8::1"), 60)
        val ipv4 = DnsAddressRecord(InetAddress.getByName("1.2.3.4"), 30)

        assertEquals(ipv4, selectPreferredDnsRecord(listOf(ipv6, ipv4)))
    }

    @Test
    fun limitsConcurrentNodeLookupsToThree() = runBlocking {
        val active = AtomicInteger()
        val maximum = AtomicInteger()
        val resolver = NodePreResolver(
            resolver = NodeAddressResolver { _, _ ->
                val current = active.incrementAndGet()
                maximum.updateAndGet { maxOf(it, current) }
                try {
                    Thread.sleep(40)
                    ResolvedNodeAddress("1.2.3.4", 60)
                } finally {
                    active.decrementAndGet()
                }
            },
        )

        resolver.refresh(
            sourceId = 9,
            hostnames = (1..12).map { "node-$it.example" }.toSet(),
            existingEntries = emptyList(),
            config = SubscriptionDnsConfig(),
        )

        assertEquals(3, maximum.get())
    }

    @Test
    fun skipsIpv6QueryWhenIpv4RecordExists() {
        val queries = mutableListOf<Int>()
        val ipv4 = DnsAddressRecord(InetAddress.getByName("1.2.3.4"), 60)

        val result = queryPreferredDnsRecord("node.example") { type ->
            queries += type
            if (type == 1) listOf(ipv4) else error("AAAA should not be queried")
        }

        assertEquals(ipv4, result)
        assertEquals(listOf(1), queries)
    }

    @Test
    fun queriesIpv6OnlyWhenIpv4IsUnavailable() {
        val queries = mutableListOf<Int>()
        val ipv6 = DnsAddressRecord(InetAddress.getByName("2001:db8::1"), 60)

        val result = queryPreferredDnsRecord("node.example") { type ->
            queries += type
            if (type == 1) emptyList() else listOf(ipv6)
        }

        assertEquals(ipv6, result)
        assertEquals(listOf(1, 28), queries)
    }

    private fun entry(
        hostname: String,
        ipAddress: String,
        expiresAt: Long,
        config: SubscriptionDnsConfig,
    ) = NodeDnsCacheEntity(
        sourceId = 7,
        hostname = hostname,
        ipAddress = ipAddress,
        expiresAt = expiresAt,
        configFingerprint = config.nodeResolutionFingerprint(),
    )
}
