package com.subconverter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionDnsTest {
    @Test
    fun providesEightValidPublicDnsPresets() {
        assertEquals(8, PublicDnsPresets.all.size)
        assertEquals(4, PublicDnsPresets.all.count { it.protocol == DnsProtocol.DOH })
        assertEquals(4, PublicDnsPresets.all.count { it.protocol == DnsProtocol.DOT })
        assertTrue(
            PublicDnsPresets.all.all {
                SubscriptionDnsConfig(protocol = it.protocol, server = it.server).validate() == null
            },
        )
    }

    @Test
    fun systemDnsDoesNotRequireServer() {
        assertNull(SubscriptionDnsConfig(server = "invalid").validate())
    }

    @Test
    fun dohRequiresHttpsEndpoint() {
        assertEquals(
            "DoH 地址必须使用 HTTPS",
            SubscriptionDnsConfig(
                protocol = DnsProtocol.DOH,
                server = "http://dns.example/dns-query",
            ).validate(),
        )
    }

    @Test
    fun dotUsesDefaultPortAndSupportsExplicitPort() {
        assertEquals(DotEndpoint("dns.example", 853), DotEndpoint.parse("dns.example"))
        assertEquals(DotEndpoint("dns.example", 8853), DotEndpoint.parse("dns.example:8853"))
    }

    @Test
    fun dotSupportsBracketedIpv6() {
        assertEquals(
            853,
            DotEndpoint.parse("[2001:4860:4860::8888]:853").port,
        )
    }

    @Test
    fun dotRejectsInvalidPort() {
        assertEquals(
            "DoT 端口必须在 1-65535 之间",
            SubscriptionDnsConfig(
                protocol = DnsProtocol.DOT,
                server = "dns.example:70000",
            ).validate(),
        )
    }

    @Test
    fun dotRejectsNonNumericPort() {
        assertEquals(
            "DoT 端口必须为数字",
            SubscriptionDnsConfig(
                protocol = DnsProtocol.DOT,
                server = "dns.example:abc",
            ).validate(),
        )
    }
}
