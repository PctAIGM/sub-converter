package com.subconverter.domain

data class RuleLine(
    val type: String = "",
    val payload: String = "",
    val target: String = "",
    val extra: String = "",
) {
    val isBlank: Boolean
        get() = type.isBlank() && payload.isBlank() && target.isBlank() && extra.isBlank()

    fun toRuleString(): String {
        val normalizedType = type.trim()
        val normalizedTarget = target.trim()
        val normalizedPayload = payload.trim()
        val normalizedExtra = extra.trim()
        val base = if (MihomoRuleTypes.isMatchOnly(normalizedType)) {
            listOf(normalizedType, normalizedTarget).joinToString(",")
        } else {
            listOf(normalizedType, normalizedPayload, normalizedTarget).joinToString(",")
        }
        return if (normalizedExtra.isEmpty()) base else "$base,$normalizedExtra"
    }
}

object MihomoRuleTypes {
    val commonTargets = listOf("DIRECT", "REJECT", "REJECT-DROP", "PASS", "COMPATIBLE", "PROXY")

    val all = listOf(
        "DOMAIN", "DOMAIN-SUFFIX", "DOMAIN-KEYWORD", "DOMAIN-WILDCARD", "DOMAIN-REGEX", "GEOSITE",
        "IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP", "SRC-GEOIP", "SRC-IP-ASN",
        "SRC-IP-CIDR", "SRC-IP-SUFFIX", "DST-PORT", "SRC-PORT", "IN-PORT", "IN-TYPE", "IN-USER",
        "IN-NAME", "REMATCH-NAME", "PROCESS-PATH", "PROCESS-PATH-WILDCARD", "PROCESS-PATH-REGEX",
        "PROCESS-NAME", "PROCESS-NAME-WILDCARD", "PROCESS-NAME-REGEX", "UID", "NETWORK", "DSCP",
        "RULE-SET", "AND", "OR", "NOT", "SUB-RULE", "MATCH",
    )

    private val ipExtraOptions = setOf("no-resolve", "src")

    fun isMatchOnly(type: String): Boolean = type.trim().equals("MATCH", ignoreCase = true)

    fun filter(query: String): List<String> = query.trim().takeIf(String::isNotEmpty)?.let { value ->
        all.filter { it.contains(value, ignoreCase = true) }
    } ?: all

    fun isExtraOption(value: String): Boolean = value.lowercase() in ipExtraOptions
}

class RuleOverrideService {
    fun parseBody(body: String): List<RuleLine> {
        if (body.isBlank()) return emptyList()
        return body.lineSequence().mapNotNull { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#") -> null
                line.isEmpty() -> RuleLine()
                else -> parseLine(line)
            }
        }.toList()
    }

    fun parseRuleStrings(body: String): List<String> = body.lineSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map(::parseLine)
        .filterNot(RuleLine::isBlank)
        .map(RuleLine::toRuleString)
        .toList()

    fun parseLine(line: String): RuleLine {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return RuleLine()
        val firstComma = trimmed.indexOf(',')
        if (firstComma < 0) return RuleLine(type = trimmed)
        val type = trimmed.substring(0, firstComma).trim()
        val rest = trimmed.substring(firstComma + 1)
        if (rest.isBlank()) return RuleLine(type = type)
        val (core, extra) = splitTrailingExtras(rest)
        if (MihomoRuleTypes.isMatchOnly(type)) {
            return RuleLine(type = type, target = core.trim(), extra = extra)
        }
        val lastComma = core.lastIndexOf(',')
        if (lastComma < 0) return RuleLine(type = type, payload = core.trim(), extra = extra)
        return RuleLine(
            type = type,
            payload = core.substring(0, lastComma).trim(),
            target = core.substring(lastComma + 1).trim(),
            extra = extra,
        )
    }

    fun serializeBody(rules: List<RuleLine>): String = rules
        .map { if (it.isBlank) "" else it.toRuleString() }
        .dropLastWhile(String::isBlank)
        .joinToString("\n")

    fun validate(body: String): String? {
        var count = 0
        body.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            count++
            validateRule(parseLine(line), index + 1)?.let { return it }
        }
        return if (count == 0) "规则覆写至少需要一条规则" else null
    }

    fun validateRule(rule: RuleLine, lineNumber: Int? = null): String? {
        val prefix = lineNumber?.let { "第 $it 行：" }.orEmpty()
        if (rule.type.isBlank()) return "${prefix}规则类型不能为空"
        if (MihomoRuleTypes.isMatchOnly(rule.type)) {
            return if (rule.target.isBlank()) "${prefix}MATCH 规则需要目标" else null
        }
        if (rule.payload.isBlank()) return "${prefix}规则内容不能为空"
        if (rule.target.isBlank()) return "${prefix}规则目标不能为空"
        return null
    }

    private fun splitTrailingExtras(rest: String): Pair<String, String> {
        var core = rest
        val extras = mutableListOf<String>()
        while (true) {
            val lastComma = core.lastIndexOf(',')
            if (lastComma < 0) break
            val tail = core.substring(lastComma + 1).trim()
            if (!MihomoRuleTypes.isExtraOption(tail)) break
            extras.add(0, tail)
            core = core.substring(0, lastComma)
        }
        return core to extras.joinToString(",")
    }
}
