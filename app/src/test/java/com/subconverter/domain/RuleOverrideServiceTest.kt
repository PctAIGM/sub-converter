package com.subconverter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RuleOverrideServiceTest {
    private val service = RuleOverrideService()

    @Test
    fun parsesAndSerializesCommonRules() {
        val body = """
            DOMAIN-SUFFIX,example.com,DIRECT
            GEOIP,CN,DIRECT,no-resolve
            MATCH,PROXY
        """.trimIndent()

        val rules = service.parseBody(body)

        assertEquals(RuleLine("DOMAIN-SUFFIX", "example.com", "DIRECT", ""), rules[0])
        assertEquals(RuleLine("GEOIP", "CN", "DIRECT", "no-resolve"), rules[1])
        assertEquals(RuleLine("MATCH", "", "PROXY", ""), rules[2])
        assertEquals(body, service.serializeBody(rules))
    }

    @Test
    fun keepsCommaPayloadForLogicalRules() {
        val line = "AND,((DOMAIN,a.com),(NETWORK,TCP)),PROXY"

        val rule = service.parseLine(line)

        assertEquals("AND", rule.type)
        assertEquals("((DOMAIN,a.com),(NETWORK,TCP))", rule.payload)
        assertEquals("PROXY", rule.target)
        assertEquals(line, rule.toRuleString())
    }

    @Test
    fun validationRejectsIncompleteRules() {
        assertNotNull(service.validate(""))
        assertNotNull(service.validate("MATCH"))
        assertNotNull(service.validate("DOMAIN-SUFFIX,example.com"))
        assertNull(service.validate("MATCH,DIRECT"))
    }

    @Test
    fun skipsCommentsAndBlankLinesWhenRendering() {
        val rules = service.parseRuleStrings("# note\n\nDOMAIN,a.com,DIRECT\n")

        assertEquals(listOf("DOMAIN,a.com,DIRECT"), rules)
    }

    @Test
    fun preservesFieldPositionsWhileEditingIncompleteRule() {
        val withoutType = RuleLine(payload = "example.com", target = "DIRECT")
        val withoutPayload = RuleLine(type = "DOMAIN-SUFFIX", target = "DIRECT")
        val withoutTarget = RuleLine(type = "DOMAIN-SUFFIX", payload = "example.com")

        assertEquals(",example.com,DIRECT", withoutType.toRuleString())
        assertEquals(withoutType, service.parseLine(withoutType.toRuleString()))
        assertEquals("DOMAIN-SUFFIX,,DIRECT", withoutPayload.toRuleString())
        assertEquals(withoutPayload, service.parseLine(withoutPayload.toRuleString()))
        assertEquals("DOMAIN-SUFFIX,example.com,", withoutTarget.toRuleString())
        assertEquals(withoutTarget, service.parseLine(withoutTarget.toRuleString()))
    }
}
