package com.subconverter.domain

import com.subconverter.data.TemplateType
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.util.LinkedHashMap

class MihomoYamlService(
    private val jsService: JsOverrideService = JsOverrideService(),
    private val ruleService: RuleOverrideService = RuleOverrideService(),
) {
    private val yaml = Yaml(
        DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
        },
    )

    fun extractProxies(yamlBody: String): List<LinkedHashMap<String, Any?>> {
        val root = loadMap(yamlBody)
        val proxies = (root["proxies"] as? List<*>).orEmpty().mapNotNull { item ->
            (item as? Map<*, *>)?.let(::copyMap)
        }
        if (proxies.isNotEmpty()) return proxies

        return ShareLinkParser.parseToMihomoProxies(yamlBody)
    }

    fun transformProxies(
        proxies: List<LinkedHashMap<String, Any?>>,
        rules: TransformRules,
    ): List<LinkedHashMap<String, Any?>> =
        proxies.mapNotNull { proxy ->
            val originalName = proxy["name"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!matches(originalName, rules)) return@mapNotNull null

            LinkedHashMap(proxy).apply {
                this["name"] = rules.prefix + originalName
            }
        }

    fun extractProxyServerHostnames(yamlBody: String): Set<String> =
        extractProxies(yamlBody)
            .mapNotNull { proxy -> proxy["server"]?.toString()?.let(::normalizeNodeHostname) }
            .toSet()

    fun replaceProxyServers(
        proxies: List<LinkedHashMap<String, Any?>>,
        addressByHostname: Map<String, String>,
    ): List<LinkedHashMap<String, Any?>> =
        proxies.map { proxy ->
            val hostname = proxy["server"]?.toString()?.let(::normalizeNodeHostname)
            val ipAddress = hostname?.let(addressByHostname::get)
            if (ipAddress == null) {
                LinkedHashMap(proxy)
            } else {
                LinkedHashMap(proxy).apply { this["server"] = ipAddress }
            }
        }

    fun renderTemplate(
        templateYaml: String,
        proxies: List<LinkedHashMap<String, Any?>>,
        overrides: List<OverrideEntry> = emptyList(),
    ): String {
        val root = loadMap(templateYaml).ifEmpty { LinkedHashMap() }
        val uniqueProxies = makeNamesUnique(proxies)
        val proxyNames = uniqueProxies.mapNotNull { it["name"]?.toString() }

        val expanded = replacePlaceholders(root, proxyNames)
        var renderedRoot: MutableMap<String, Any?> = if (expanded is MutableMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            expanded as MutableMap<String, Any?>
        } else {
            LinkedHashMap()
        }
        renderedRoot["proxies"] = uniqueProxies

        overrides.forEachIndexed { index, entry ->
            renderedRoot = when (entry.type) {
                TemplateType.JS -> applySingleJsOverride(renderedRoot, entry.body, index + 1)
                TemplateType.RULES -> applyRulesOverride(renderedRoot, entry.body)
                else -> {
                    val patch = parseOverrideMap(entry.body)
                    val expandedPatch = replacePlaceholders(patch, proxyNames)
                    if (expandedPatch is Map<*, *>) {
                        deepMerge(renderedRoot, expandedPatch)
                    }
                    renderedRoot
                }
            }
        }
        return yaml.dump(renderedRoot)
    }

    private fun applyRulesOverride(
        root: MutableMap<String, Any?>,
        body: String,
    ): MutableMap<String, Any?> {
        val rules = ruleService.parseRuleStrings(body)
        if (rules.isEmpty()) return root
        val existing = (root["rules"] as? List<*>)?.map(::copyValue).orEmpty()
        root["rules"] = rules + existing
        return root
    }

    private fun applySingleJsOverride(
        root: MutableMap<String, Any?>,
        script: String,
        ordinal: Int,
    ): MutableMap<String, Any?> {
        val result = runCatching { jsService.execute(script, root) }.getOrElse { cause ->
            val reason = (cause as? JsOverrideException)?.message ?: cause.message ?: cause::class.java.simpleName
            throw IllegalStateException("JavaScript 覆写 #$ordinal 执行失败: $reason", cause)
        }
        return LinkedHashMap(result)
    }

    fun validateOverrideYaml(yamlBody: String): String? {
        if (yamlBody.isBlank()) return null
        return runCatching {
            val map = parseOverrideMap(yamlBody)
            map.forEach { (rawKey, value) ->
                val key = rawKey.toString()
                val isListSyntax = key.startsWith("+") || key.endsWith("+")
                if (isListSyntax && value !is List<*>) {
                    val cleanKey = key.trimStart('+').trimEnd('+').trim('<', '>')
                    throw IllegalArgumentException("「$cleanKey」使用追加语法(+ / +)，值必须是列表，请在每项前加「- 」")
                }
            }
            null
        }.getOrElse { throwable ->
            throwable.message ?: "覆写 YAML 解析失败"
        }
    }

    fun validateOverride(type: String, body: String): String? =
        when (type) {
            TemplateType.JS -> jsService.validate(body)
            TemplateType.RULES -> ruleService.validate(body)
            else -> validateOverrideYaml(body)
        }

    private fun loadMap(yamlBody: String): LinkedHashMap<String, Any?> {
        val loaded = runCatching { yaml.load<Any?>(yamlBody) }.getOrNull()
        return (loaded as? Map<*, *>)?.let(::copyMap) ?: LinkedHashMap()
    }

    private fun parseOverrideMap(yamlBody: String): LinkedHashMap<String, Any?> {
        if (yamlBody.isBlank()) return LinkedHashMap()
        val loaded = yaml.load<Any?>(yamlBody) ?: return LinkedHashMap()
        return (loaded as? Map<*, *>)?.let(::copyMap)
            ?: throw IllegalArgumentException("覆写 YAML 必须是对象")
    }

    private fun matches(name: String, rules: TransformRules): Boolean {
        val included = rules.includeRegex.isBlank() ||
            runCatching { Regex(rules.includeRegex).containsMatchIn(name) }.getOrDefault(false)
        val excluded = rules.excludeRegex.isNotBlank() &&
            runCatching { Regex(rules.excludeRegex).containsMatchIn(name) }.getOrDefault(false)
        return included && !excluded
    }

    private fun makeNamesUnique(
        proxies: List<LinkedHashMap<String, Any?>>,
    ): List<LinkedHashMap<String, Any?>> {
        val seen = mutableMapOf<String, Int>()
        return proxies.map { proxy ->
            val name = proxy["name"]?.toString().orEmpty()
            val count = seen.getOrDefault(name, 0) + 1
            seen[name] = count

            if (count == 1) {
                LinkedHashMap(proxy)
            } else {
                LinkedHashMap(proxy).apply { this["name"] = "$name ($count)" }
            }
        }
    }

    private fun replacePlaceholders(value: Any?, proxyNames: List<String>): Any? =
        when (value) {
            is Map<*, *> -> LinkedHashMap<String, Any?>().apply {
                value.forEach { (key, child) ->
                    this[key.toString()] = replacePlaceholders(child, proxyNames)
                }
            }

            is List<*> -> value.map { replacePlaceholders(it, proxyNames) }
            "{{proxy_names}}" -> proxyNames
            "{{proxy_names_csv}}" -> proxyNames.joinToString(",")
            is String -> value
                .replace("{{proxy_names_csv}}", proxyNames.joinToString(","))
            else -> value
        }

    private fun deepMerge(target: MutableMap<String, Any?>, patch: Map<*, *>) {
        patch.forEach { (rawKeyValue, patchValue) ->
            val rawKey = rawKeyValue.toString()
            if (rawKey.endsWith("!")) {
                target[trimWrap(rawKey.dropLast(1))] = copyValue(patchValue)
                return@forEach
            }

            val prepend = rawKey.startsWith("+")
            val append = !prepend && rawKey.endsWith("+")
            val cleanKey = trimWrap(
                when {
                    prepend -> rawKey.drop(1)
                    append -> rawKey.dropLast(1)
                    else -> rawKey
                },
            )

            when (patchValue) {
                is Map<*, *> -> {
                    val current = target[cleanKey]
                    val targetChild = if (current is MutableMap<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        current as MutableMap<String, Any?>
                    } else {
                        LinkedHashMap<String, Any?>().also { target[cleanKey] = it }
                    }
                    deepMerge(targetChild, patchValue)
                }

                is List<*> -> {
                    val patchList = patchValue.map(::copyValue)
                    when {
                        prepend -> {
                            val current = target[cleanKey] as? List<*> ?: emptyList<Any?>()
                            target[cleanKey] = patchList + current.map(::copyValue)
                        }

                        append -> {
                            val current = target[cleanKey] as? List<*> ?: emptyList<Any?>()
                            target[cleanKey] = current.map(::copyValue) + patchList
                        }

                        else -> {
                            target[cleanKey] = patchList
                        }
                    }
                }

                else -> {
                    target[cleanKey] = copyValue(patchValue)
                }
            }
        }
    }

    private fun trimWrap(key: String): String =
        if (key.startsWith("<") && key.endsWith(">")) key.substring(1, key.lastIndex) else key

    private fun copyValue(value: Any?): Any? =
        when (value) {
            is Map<*, *> -> copyMap(value)
            is List<*> -> value.map(::copyValue)
            else -> value
        }

    private fun copyMap(map: Map<*, *>): LinkedHashMap<String, Any?> =
        LinkedHashMap<String, Any?>().apply {
            map.forEach { (key, value) ->
                this[key.toString()] = copyValue(value)
            }
        }
}
