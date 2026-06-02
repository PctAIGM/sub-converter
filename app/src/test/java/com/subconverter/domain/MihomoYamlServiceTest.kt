package com.subconverter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class MihomoYamlServiceTest {
    private val service = MihomoYamlService()

    @Test
    fun extractsFiltersPrefixesAndRendersTemplate() {
        val input = """
            proxies:
              - name: HK 01
                type: ss
                server: hk.example.com
                port: 443
              - name: JP 01
                type: ss
                server: jp.example.com
                port: 443
        """.trimIndent()

        val proxies = service.extractProxies(input)
        val sourceFiltered = service.transformProxies(
            proxies,
            TransformRules(prefix = "[A] ", includeRegex = "HK"),
        )
        val outputFiltered = service.transformProxies(
            sourceFiltered,
            TransformRules(prefix = "[OUT] "),
        )
        val rendered = service.renderTemplate(DEFAULT_MIHOMO_TEMPLATE.trimIndent(), outputFiltered)

        assertEquals(1, outputFiltered.size)
        assertEquals("[OUT] [A] HK 01", outputFiltered.first()["name"])
        assertTrue(rendered.contains("name: PROXY"))
        assertTrue(rendered.contains("[OUT] [A] HK 01"))
        assertTrue(rendered.contains("proxies:"))
    }

    @Test
    fun makesDuplicateProxyNamesUnique() {
        val input = """
            proxies:
              - name: Node
                type: ss
              - name: Node
                type: ss
        """.trimIndent()

        val rendered = service.renderTemplate(
            DEFAULT_MIHOMO_TEMPLATE.trimIndent(),
            service.extractProxies(input),
        )

        assertTrue(rendered.contains("name: Node"))
        assertTrue(rendered.contains("name: Node (2)"))
    }

    @Test
    fun extractsFromBase64ShareLinksWhenYamlHasNoProxies() {
        val link = "vless://123e4567-e89b-12d3-a456-426614174000@example.com:443?type=tcp&security=reality&sni=www.microsoft.com&fp=chrome&pbk=test-public-key&sid=abcd#HK-01"
        val base64 = Base64.getEncoder().encodeToString(link.toByteArray())

        val proxies = service.extractProxies(base64)

        assertEquals(1, proxies.size)
        assertEquals("HK-01", proxies[0]["name"])
        assertEquals("vless", proxies[0]["type"])
        assertEquals("example.com", proxies[0]["server"])
        assertEquals(443, proxies[0]["port"])
    }
}
