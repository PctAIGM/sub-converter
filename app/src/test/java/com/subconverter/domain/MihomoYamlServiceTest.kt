package com.subconverter.domain

import com.subconverter.data.TemplateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.Yaml
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
    fun appliesYamlOverridesInOrder() {
        val input = """
            proxies:
              - name: HK 01
                type: ss
        """.trimIndent()

        val rendered = service.renderTemplate(
            DEFAULT_MIHOMO_TEMPLATE.trimIndent(),
            service.extractProxies(input),
            listOf(
                OverrideEntry(
                    TemplateType.YAML,
                    """
                    +rules:
                      - DOMAIN,front.example,DIRECT
                    rules+:
                      - DOMAIN,tail.example,REJECT
                    proxy-groups!:
                      - name: CUSTOM
                        type: select
                        proxies: "{{proxy_names}}"
                    dns:
                      enable: true
                    """.trimIndent(),
                ),
            ),
        )
        val root = Yaml().load<Map<String, Any?>>(rendered)
        val rules = root["rules"] as List<*>
        val groups = root["proxy-groups"] as List<*>
        val firstGroup = groups.first() as Map<*, *>

        assertEquals("DOMAIN,front.example,DIRECT", rules.first())
        assertEquals("DOMAIN,tail.example,REJECT", rules.last())
        assertEquals("CUSTOM", firstGroup["name"])
        assertEquals(listOf("HK 01"), firstGroup["proxies"])
        assertEquals(mapOf("enable" to true), root["dns"])
    }

    @Test
    fun rejectsNonObjectOverrideYaml() {
        assertNotNull(service.validateOverrideYaml("- item"))
    }

    @Test
    fun prependsAndAppendsRulesWithPlusSyntax() {
        val input = """
            proxies:
              - name: HK 01
                type: ss
        """.trimIndent()

        val rendered = service.renderTemplate(
            DEFAULT_MIHOMO_TEMPLATE.trimIndent(),
            service.extractProxies(input),
            overrides = listOf(
                OverrideEntry(
                    TemplateType.YAML,
                    """
                    +rules:
                      - DOMAIN,prepend.example,DIRECT
                    rules+:
                      - DOMAIN,append.example,REJECT
                    """.trimIndent(),
                ),
            ),
        )
        val root = Yaml().load<Map<String, Any?>>(rendered)
        val rules = root["rules"] as List<*>

        assertEquals("DOMAIN,prepend.example,DIRECT", rules.first())
        assertEquals("DOMAIN,append.example,REJECT", rules.last())
        assertFalse(rules.any { it.toString().startsWith("+") || it.toString().endsWith("+") })
    }

    @Test
    fun plusSyntaxScalarValueIsRejectedAsValidationError() {
        val badOverride = "+rules:\n  DOMAIN-SUFFIX,jd.com,DIRECT"
        val error = service.validateOverride(TemplateType.YAML, badOverride)
        assertNotNull(error)
        assertTrue(error!!.contains("列表"))
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

    @Test
    fun replacesOnlyProxyServerUsingNormalizedHostname() {
        val input = """
            proxies:
              - name: Node
                type: vmess
                server: NODE.Example.COM.
                port: 443
                sni: tls.example.com
                ws-opts:
                  headers:
                    Host: ws.example.com
        """.trimIndent()

        val hostnames = service.extractProxyServerHostnames(input)
        val replaced = service.replaceProxyServers(
            service.extractProxies(input),
            mapOf("node.example.com" to "1.2.3.4"),
        )

        assertEquals(setOf("node.example.com"), hostnames)
        assertEquals("1.2.3.4", replaced.single()["server"])
        assertEquals("tls.example.com", replaced.single()["sni"])
        val wsOptions = replaced.single()["ws-opts"] as Map<*, *>
        val headers = wsOptions["headers"] as Map<*, *>
        assertEquals("ws.example.com", headers["Host"])
    }

    @Test
    fun rendersWithoutJavaScriptOverridesWhenListEmpty() {
        val input = """
            proxies:
              - name: HK 01
                type: ss
        """.trimIndent()

        val rendered = service.renderTemplate(
            DEFAULT_MIHOMO_TEMPLATE.trimIndent(),
            service.extractProxies(input),
            overrides = listOf(OverrideEntry(TemplateType.YAML, "rules+:\n  - DOMAIN,a.example,DIRECT")),
        )
        val root = Yaml().load<Map<String, Any?>>(rendered)
        val rules = root["rules"] as List<*>

        assertEquals("DOMAIN,a.example,DIRECT", rules.last())
    }

    @Test
    fun validateOverrideDispatchesByType() {
        // YAML 分支可在 JVM 单元测试下验证；JS 分支依赖原生 QuickJS 引擎，
        // 需在 androidTest（设备/模拟器）下覆盖，此处不调用。
        assertNull(service.validateOverride("YAML", "rules+: []"))
        assertNotNull(service.validateOverride("YAML", "- not-an-object"))
    }
}
